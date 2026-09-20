package uy.edu.utec.taller.procesamiento.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.procesamiento.client.dto.OrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.ProductoResponse;
import uy.edu.utec.taller.procesamiento.dto.FacturaDTO;
import uy.edu.utec.taller.procesamiento.dto.ProcesamientoDTO;
import uy.edu.utec.taller.procesamiento.exception.FacturaNoEncontradaException;
import uy.edu.utec.taller.procesamiento.model.Factura;
import uy.edu.utec.taller.procesamiento.model.FacturaItem;
import uy.edu.utec.taller.procesamiento.model.Procesamiento;
import uy.edu.utec.taller.procesamiento.model.ResultadoProcesamiento;
import uy.edu.utec.taller.procesamiento.repository.FacturaRepository;
import uy.edu.utec.taller.procesamiento.repository.ProcesamientoRepository;

/**
 * Persistencia local del procesador: registro de órdenes procesadas (idempotencia) y facturas.
 * Cada operación es una transacción corta; las llamadas HTTP a otros servicios se hacen fuera.
 */
@Service
@RequiredArgsConstructor
public class FacturacionService {

    private final FacturaRepository facturaRepository;
    private final ProcesamientoRepository procesamientoRepository;

    @Transactional(readOnly = true)
    public Optional<Procesamiento> buscarProcesamiento(Long ordenId) {
        return procesamientoRepository.findByOrdenId(ordenId);
    }

    /** Registra que la orden se resolvió como {@code No Stock}. */
    @Transactional
    public Procesamiento registrarSinStock(Long ordenId, String detalle) {
        return procesamientoRepository.save(Procesamiento.builder()
                .ordenId(ordenId)
                .resultado(ResultadoProcesamiento.NoStock)
                .fechaProcesamiento(OffsetDateTime.now())
                .ordenActualizada(false)
                .detalle(detalle)
                .build());
    }

    /**
     * Genera la factura de la orden (un ítem por producto con su precio unitario y el total como
     * suma de los subtotales) y registra el procesamiento como {@code Ready to Delivery}, todo
     * en una única transacción.
     *
     * @param cantidades cantidad total pedida por producto (ya agregadas las líneas repetidas)
     * @param productos  datos de cada producto (nombre y precio) al momento de facturar
     */
    @Transactional
    public Procesamiento registrarListaParaEntrega(OrdenResponse orden, Map<Long, Integer> cantidades,
            Map<Long, ProductoResponse> productos) {
        List<FacturaItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> pedido : cantidades.entrySet()) {
            ProductoResponse producto = productos.get(pedido.getKey());
            BigDecimal precio = BigDecimal.valueOf(producto.getPrecioUnitario()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(pedido.getValue()));
            items.add(FacturaItem.builder()
                    .productoId(producto.getId())
                    .descripcion(producto.getNombre())
                    .cantidad(pedido.getValue())
                    .precioUnitario(precio)
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);
        }

        facturaRepository.save(Factura.builder()
                .ordenId(orden.getId())
                .fecha(OffsetDateTime.now())
                .email(orden.getEmail())
                .direccionEnvio(orden.getDireccionEnvio())
                .telefono(orden.getTelefono())
                .total(total)
                .items(items)
                .build());

        return procesamientoRepository.save(Procesamiento.builder()
                .ordenId(orden.getId())
                .resultado(ResultadoProcesamiento.ReadyToDelivery)
                .fechaProcesamiento(OffsetDateTime.now())
                .ordenActualizada(false)
                .build());
    }

    @Transactional
    public void marcarOrdenActualizada(Long procesamientoId) {
        procesamientoRepository.findById(procesamientoId).ifPresent(p -> {
            p.setOrdenActualizada(true);
            procesamientoRepository.save(p);
        });
    }

    @Transactional(readOnly = true)
    public List<FacturaDTO> listarFacturas(Long ordenId) {
        List<Factura> facturas = ordenId == null
                ? facturaRepository.findAll()
                : facturaRepository.findByOrdenId(ordenId);
        return facturas.stream().map(FacturaDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public FacturaDTO obtenerFactura(Long id) {
        return facturaRepository.findById(id)
                .map(FacturaDTO::fromEntity)
                .orElseThrow(() -> new FacturaNoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<ProcesamientoDTO> listarProcesamientos() {
        return procesamientoRepository.findAll(Sort.by("id")).stream()
                .map(ProcesamientoDTO::fromEntity)
                .toList();
    }
}
