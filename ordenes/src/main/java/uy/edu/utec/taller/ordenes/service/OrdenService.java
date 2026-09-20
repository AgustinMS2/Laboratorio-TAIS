package uy.edu.utec.taller.ordenes.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.ordenes.client.ProductoClient;
import uy.edu.utec.taller.ordenes.client.dto.ProductoResponse;
import uy.edu.utec.taller.ordenes.dto.LineaOrdenCreateDTO;
import uy.edu.utec.taller.ordenes.dto.LineaOrdenDetalleDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenCreadaDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenCreateDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDetalleDTO;
import uy.edu.utec.taller.ordenes.exception.EstadoNoPermitidoException;
import uy.edu.utec.taller.ordenes.exception.OrdenNoEncontradaException;
import uy.edu.utec.taller.ordenes.exception.ProductosInexistentesException;
import uy.edu.utec.taller.ordenes.exception.StockInsuficienteException;
import uy.edu.utec.taller.ordenes.exception.TransicionEstadoInvalidaException;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;
import uy.edu.utec.taller.ordenes.model.LineaOrden;
import uy.edu.utec.taller.ordenes.model.Orden;
import uy.edu.utec.taller.ordenes.repository.OrdenRepository;

@Service
@RequiredArgsConstructor
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final ProductoClient productoClient;

    @Transactional(readOnly = true)
    public List<OrdenDTO> listarOrdenes() {
        return ordenRepository.findAll()
                .stream()
                .map(OrdenDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenDTO> listarOrdenesPorEstado(EstadoOrden estado) {
        return ordenRepository.findByEstadoOrderByIdAsc(estado)
                .stream()
                .map(OrdenDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdenDTO obtenerOrden(Long id) {
        return ordenRepository.findById(id)
                .map(OrdenDTO::fromEntity)
                .orElseThrow(() -> new OrdenNoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public OrdenDetalleDTO obtenerDetalleOrden(Long id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new OrdenNoEncontradaException(id));

        List<LineaOrdenDetalleDTO> lineas = new ArrayList<>();
        double total = 0.0;

        for (LineaOrden linea : orden.getProductos()) {
            ProductoResponse producto = productoClient.obtenerProducto(linea.getProductoId()).orElse(null);

            Double subtotal = null;
            if (producto != null && producto.getPrecioUnitario() != null) {
                subtotal = redondear(producto.getPrecioUnitario() * linea.getCantidad());
                total += subtotal;
            }

            lineas.add(LineaOrdenDetalleDTO.builder()
                    .cantidad(linea.getCantidad())
                    .subtotal(subtotal)
                    .producto(producto)
                    .build());
        }

        return OrdenDetalleDTO.builder()
                .id(orden.getId())
                .email(orden.getEmail())
                .direccionEnvio(orden.getDireccionEnvio())
                .telefono(orden.getTelefono())
                .estado(orden.getEstado())
                .fechaCreacion(orden.getFechaCreacion())
                .total(redondear(total))
                .productos(lineas)
                .build();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /**
     * Registra una orden en estado {@code Created} tras validar que cada producto existe y
     * tiene stock suficiente. <b>No descuenta el stock</b>: eso lo hace el servicio de
     * procesamiento cuando recibe la orden que el servicio publicador toma de aquí (estado Created)
     * y publica en el broker de mensajería.
     *
     * @throws ProductosInexistentesException si algún producto solicitado no existe (409).
     * @throws StockInsuficienteException si algún producto no tiene stock suficiente (409).
     */
    @Transactional
    public OrdenCreadaDTO crearOrden(OrdenCreateDTO ordenCreate) {
        // Cantidad total pedida por producto (agrega líneas repetidas del mismo producto).
        Map<Long, Integer> cantidadPorProducto = new LinkedHashMap<>();
        for (LineaOrdenCreateDTO linea : ordenCreate.getProductos()) {
            cantidadPorProducto.merge(linea.getProductoId(), linea.getCantidad(), Integer::sum);
        }

        // 1. Traer cada producto del servicio de Productos y validar existencia y stock.
        List<String> inexistentes = new ArrayList<>();
        List<String> sinStock = new ArrayList<>();

        cantidadPorProducto.forEach((productoId, cantidad) -> {
            ProductoResponse producto = productoClient.obtenerProducto(productoId).orElse(null);
            if (producto == null) {
                inexistentes.add("No existe el producto con id " + productoId);
                return;
            }
            int disponible = producto.getStock() == null ? 0 : producto.getStock();
            if (disponible < cantidad) {
                sinStock.add("Producto " + productoId + ": stock disponible " + disponible
                        + ", cantidad solicitada " + cantidad);
            }
        });

        if (!inexistentes.isEmpty()) {
            throw new ProductosInexistentesException(inexistentes);
        }
        if (!sinStock.isEmpty()) {
            throw new StockInsuficienteException(sinStock);
        }

        // 2. Persistir la orden con sus líneas tal como fueron solicitadas.
        Orden orden = Orden.builder()
                .email(ordenCreate.getEmail())
                .direccionEnvio(ordenCreate.getDireccionEnvio())
                .telefono(ordenCreate.getTelefono())
                .estado(EstadoOrden.Created)
                .productos(ordenCreate.getProductos().stream()
                        .map(linea -> LineaOrden.builder()
                                .productoId(linea.getProductoId())
                                .cantidad(linea.getCantidad())
                                .build())
                        .collect(Collectors.toCollection(ArrayList::new)))
                .build();
        Orden guardada = ordenRepository.save(orden);

        return OrdenCreadaDTO.builder().id(guardada.getId()).build();
    }

    @Transactional
    public void actualizarEstadoProcesamiento(Long id, EstadoOrden nuevoEstado) {
        if (nuevoEstado != EstadoOrden.ReadyToDelivery && nuevoEstado != EstadoOrden.NoStock) {
            throw new EstadoNoPermitidoException(nuevoEstado);
        }
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new OrdenNoEncontradaException(id));

        if (orden.getEstado() == nuevoEstado) {
            return;
        }
        if (orden.getEstado() != EstadoOrden.Created) {
            throw new TransicionEstadoInvalidaException(id, orden.getEstado(), nuevoEstado);
        }
        orden.setEstado(nuevoEstado);
        ordenRepository.save(orden);
    }
}
