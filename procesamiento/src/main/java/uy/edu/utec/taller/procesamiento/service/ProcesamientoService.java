package uy.edu.utec.taller.procesamiento.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uy.edu.utec.taller.procesamiento.client.OrdenClient;
import uy.edu.utec.taller.procesamiento.client.ProductoClient;
import uy.edu.utec.taller.procesamiento.client.dto.LineaOrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.OrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.ProductoResponse;
import uy.edu.utec.taller.procesamiento.dto.OrdenMensajeDTO;
import uy.edu.utec.taller.procesamiento.model.Procesamiento;

/**
 * Procesa las órdenes recibidas por mensajería.
 *
 * <ol>
 *   <li>Valida que la orden no esté previamente procesada (registro local + estado en órdenes).</li>
 *   <li>Verifica que haya stock de todos los productos solicitados.</li>
 *   <li>Con stock: descuenta el stock, genera la factura y deja la orden en {@code Ready to Delivery}.
 *       Sin stock (o producto inexistente): deja la orden en {@code No Stock}.</li>
 * </ol>
 *
 * <p>Es idempotente ante mensajes duplicados (la entrega es "al menos una vez"): el resultado se
 * registra localmente <b>antes</b> de informarlo a órdenes, y si esa última llamada falla se
 * reintenta cuando llega el duplicado, sin volver a descontar stock ni a facturar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcesamientoService {

    private static final String ESTADO_PENDIENTE = "Created";

    private final OrdenClient ordenClient;
    private final ProductoClient productoClient;
    private final FacturacionService facturacionService;

    /**
     * @throws uy.edu.utec.taller.procesamiento.exception.ServicioExternoException si otro servicio
     *         falla; el suscriptor reintenta y, de persistir, la orden se vuelve a publicar más tarde.
     */
    public void procesar(OrdenMensajeDTO mensaje) {
        Long ordenId = mensaje.getId();

        Optional<Procesamiento> previo = facturacionService.buscarProcesamiento(ordenId);
        if (previo.isPresent()) {
            reanudar(previo.get());
            return;
        }

        Optional<OrdenResponse> encontrada = ordenClient.obtenerOrden(ordenId);
        if (encontrada.isEmpty()) {
            log.warn("La orden {} no existe en el servicio de órdenes; se descarta el mensaje", ordenId);
            return;
        }
        OrdenResponse orden = encontrada.get();
        if (!ESTADO_PENDIENTE.equals(orden.getEstado())) {
            log.info("La orden {} ya está en estado '{}'; no se vuelve a procesar", ordenId, orden.getEstado());
            return;
        }

        Map<Long, Integer> cantidades = cantidadesPorProducto(orden);
        Map<Long, ProductoResponse> productos = new LinkedHashMap<>();
        List<String> faltantes = new ArrayList<>();
        cantidades.forEach((productoId, cantidad) -> verificarStock(productoId, cantidad, productos, faltantes));

        Procesamiento procesamiento;
        if (faltantes.isEmpty()) {
            descontarStock(cantidades, productos);
            try {
                procesamiento = facturacionService.registrarListaParaEntrega(orden, cantidades, productos);
            } catch (RuntimeException ex) {
                restaurarStock(cantidades);
                throw ex;
            }
            log.info("Orden {}: stock descontado y factura generada -> Ready to Delivery", ordenId);
        } else {
            procesamiento = facturacionService.registrarSinStock(ordenId, String.join("; ", faltantes));
            log.info("Orden {}: sin stock suficiente ({}) -> No Stock", ordenId, procesamiento.getDetalle());
        }

        informarResultado(procesamiento);
    }

    /** Mensaje duplicado: si el resultado quedó sin informar a órdenes, lo reintenta; si no, lo ignora. */
    private void reanudar(Procesamiento procesamiento) {
        if (procesamiento.isOrdenActualizada()) {
            log.info("La orden {} ya fue procesada ({}); se ignora el mensaje duplicado",
                    procesamiento.getOrdenId(), procesamiento.getResultado().getValor());
            return;
        }
        log.info("La orden {} ya fue procesada pero falta informar '{}' al servicio de órdenes; se reintenta",
                procesamiento.getOrdenId(), procesamiento.getResultado().getValor());
        informarResultado(procesamiento);
    }

    private void informarResultado(Procesamiento procesamiento) {
        boolean aplicado = ordenClient.actualizarEstado(procesamiento.getOrdenId(), procesamiento.getResultado());
        if (!aplicado) {
            log.warn("La orden {} ya estaba en otro estado y no aceptó '{}'; se da por resuelta",
                    procesamiento.getOrdenId(), procesamiento.getResultado().getValor());
        }
        facturacionService.marcarOrdenActualizada(procesamiento.getId());
    }

    private static Map<Long, Integer> cantidadesPorProducto(OrdenResponse orden) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (LineaOrdenResponse linea : orden.getProductos()) {
            cantidades.merge(linea.getProductoId(), linea.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    private void verificarStock(Long productoId, int cantidad, Map<Long, ProductoResponse> productos,
            List<String> faltantes) {
        Optional<ProductoResponse> encontrado = productoClient.obtenerProducto(productoId);
        if (encontrado.isEmpty()) {
            faltantes.add("Producto " + productoId + ": no existe");
            return;
        }
        ProductoResponse producto = encontrado.get();
        productos.put(productoId, producto);
        int disponible = producto.getStock() == null ? 0 : producto.getStock();
        if (disponible < cantidad) {
            faltantes.add("Producto " + productoId + ": stock disponible " + disponible
                    + ", cantidad solicitada " + cantidad);
        }
    }

    /** Descuenta el stock de cada producto; si alguno falla, devuelve lo ya descontado y relanza. */
    private void descontarStock(Map<Long, Integer> cantidades, Map<Long, ProductoResponse> productos) {
        Map<Long, Integer> descontados = new LinkedHashMap<>();
        try {
            for (Map.Entry<Long, Integer> pedido : cantidades.entrySet()) {
                ProductoResponse producto = productos.get(pedido.getKey());
                productoClient.actualizarStock(producto.getId(), producto.getStock() - pedido.getValue());
                descontados.put(producto.getId(), pedido.getValue());
            }
        } catch (RuntimeException ex) {
            restaurarStock(descontados);
            throw ex;
        }
    }

    /** Devuelve al stock las cantidades indicadas (compensación); registra el error si no puede. */
    private void restaurarStock(Map<Long, Integer> cantidades) {
        cantidades.forEach((productoId, cantidad) -> {
            try {
                productoClient.obtenerProducto(productoId).ifPresent(actual ->
                        productoClient.actualizarStock(productoId, actual.getStock() + cantidad));
            } catch (RuntimeException ex) {
                log.error("No se pudo restaurar el stock del producto {} (+{}); revisar manualmente",
                        productoId, cantidad, ex);
            }
        });
    }
}
