package uy.edu.utec.taller.ordenes.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.ordenes.client.ProductoClient;
import uy.edu.utec.taller.ordenes.client.dto.ProductoResponse;
import uy.edu.utec.taller.ordenes.dto.LineaOrdenDetalleDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDetalleDTO;
import uy.edu.utec.taller.ordenes.exception.OrdenNoEncontradaException;
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
}
