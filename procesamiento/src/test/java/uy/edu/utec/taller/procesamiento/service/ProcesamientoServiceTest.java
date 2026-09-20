package uy.edu.utec.taller.procesamiento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.utec.taller.procesamiento.client.OrdenClient;
import uy.edu.utec.taller.procesamiento.client.ProductoClient;
import uy.edu.utec.taller.procesamiento.client.dto.LineaOrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.OrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.ProductoResponse;
import uy.edu.utec.taller.procesamiento.dto.OrdenMensajeDTO;
import uy.edu.utec.taller.procesamiento.exception.ServicioExternoException;
import uy.edu.utec.taller.procesamiento.model.Procesamiento;
import uy.edu.utec.taller.procesamiento.model.ResultadoProcesamiento;

@ExtendWith(MockitoExtension.class)
class ProcesamientoServiceTest {

    @Mock
    private OrdenClient ordenClient;

    @Mock
    private ProductoClient productoClient;

    @Mock
    private FacturacionService facturacionService;

    @InjectMocks
    private ProcesamientoService procesamientoService;

    private static OrdenMensajeDTO mensaje(long id) {
        return OrdenMensajeDTO.builder()
                .id(id)
                .estado("Created")
                .fechaCreacion(OffsetDateTime.parse("2026-06-21T14:30:00-03:00"))
                .build();
    }

    private static OrdenResponse orden(String estado, LineaOrdenResponse... lineas) {
        return OrdenResponse.builder()
                .id(1L)
                .email("cliente@email.com")
                .direccionEnvio("Av. Italia 3333, Maldonado")
                .telefono("+59899111222")
                .estado(estado)
                .productos(List.of(lineas))
                .build();
    }

    private static LineaOrdenResponse linea(long productoId, int cantidad) {
        return new LineaOrdenResponse(productoId, cantidad);
    }

    private static ProductoResponse producto(long id, String nombre, double precio, int stock) {
        return ProductoResponse.builder().id(id).nombre(nombre).precioUnitario(precio).stock(stock).build();
    }

    private static Procesamiento procesamiento(ResultadoProcesamiento resultado, boolean actualizada) {
        return Procesamiento.builder()
                .id(10L)
                .ordenId(1L)
                .resultado(resultado)
                .fechaProcesamiento(OffsetDateTime.now())
                .ordenActualizada(actualizada)
                .build();
    }

    @Test
    @DisplayName("con stock: descuenta cada producto, genera la factura y deja la orden en Ready to Delivery")
    void testProcesaConStock() {
        OrdenResponse orden = orden("Created", linea(1, 2), linea(2, 5));
        ProductoResponse p1 = producto(1, "Notebook Lenovo ThinkPad", 1250.50, 15);
        ProductoResponse p2 = producto(2, "Mouse Logitech MX Master 3", 99.90, 40);
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(1L)).thenReturn(Optional.of(p1));
        when(productoClient.obtenerProducto(2L)).thenReturn(Optional.of(p2));
        when(facturacionService.registrarListaParaEntrega(eq(orden), any(), any()))
                .thenReturn(procesamiento(ResultadoProcesamiento.ReadyToDelivery, false));
        when(ordenClient.actualizarEstado(1L, ResultadoProcesamiento.ReadyToDelivery)).thenReturn(true);

        procesamientoService.procesar(mensaje(1L));

        verify(productoClient).actualizarStock(1L, 13);
        verify(productoClient).actualizarStock(2L, 35);
        ArgumentCaptor<Map<Long, Integer>> cantidades = ArgumentCaptor.forClass(Map.class);
        verify(facturacionService).registrarListaParaEntrega(eq(orden), cantidades.capture(), any());
        assertThat(cantidades.getValue()).containsEntry(1L, 2).containsEntry(2L, 5);
        verify(ordenClient).actualizarEstado(1L, ResultadoProcesamiento.ReadyToDelivery);
        verify(facturacionService).marcarOrdenActualizada(10L);
        verify(facturacionService, never()).registrarSinStock(anyLong(), any());
    }

    @Test
    @DisplayName("agrega las líneas repetidas de un mismo producto antes de verificar y descontar")
    void testAgregaLineasRepetidas() {
        OrdenResponse orden = orden("Created", linea(1, 2), linea(1, 3));
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(1L)).thenReturn(Optional.of(producto(1, "Notebook", 100.0, 15)));
        when(facturacionService.registrarListaParaEntrega(any(), any(), any()))
                .thenReturn(procesamiento(ResultadoProcesamiento.ReadyToDelivery, false));
        when(ordenClient.actualizarEstado(anyLong(), any())).thenReturn(true);

        procesamientoService.procesar(mensaje(1L));

        verify(productoClient).actualizarStock(1L, 10);
    }

    @Test
    @DisplayName("sin stock de algún producto: no descuenta nada, no factura y deja la orden en No Stock")
    void testProcesaSinStock() {
        OrdenResponse orden = orden("Created", linea(1, 2), linea(2, 5));
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(1L)).thenReturn(Optional.of(producto(1, "Notebook", 1250.50, 15)));
        when(productoClient.obtenerProducto(2L)).thenReturn(Optional.of(producto(2, "Mouse", 99.90, 3)));
        when(facturacionService.registrarSinStock(eq(1L), any()))
                .thenReturn(procesamiento(ResultadoProcesamiento.NoStock, false));
        when(ordenClient.actualizarEstado(1L, ResultadoProcesamiento.NoStock)).thenReturn(true);

        procesamientoService.procesar(mensaje(1L));

        verify(productoClient, never()).actualizarStock(anyLong(), anyInt());
        verify(facturacionService, never()).registrarListaParaEntrega(any(), any(), any());
        verify(facturacionService).registrarSinStock(1L, "Producto 2: stock disponible 3, cantidad solicitada 5");
        verify(ordenClient).actualizarEstado(1L, ResultadoProcesamiento.NoStock);
        verify(facturacionService).marcarOrdenActualizada(10L);
    }

    @Test
    @DisplayName("un producto inexistente se trata como falta de stock (No Stock)")
    void testProductoInexistenteEsNoStock() {
        OrdenResponse orden = orden("Created", linea(99, 1));
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(99L)).thenReturn(Optional.empty());
        when(facturacionService.registrarSinStock(eq(1L), any()))
                .thenReturn(procesamiento(ResultadoProcesamiento.NoStock, false));
        when(ordenClient.actualizarEstado(1L, ResultadoProcesamiento.NoStock)).thenReturn(true);

        procesamientoService.procesar(mensaje(1L));

        verify(facturacionService).registrarSinStock(1L, "Producto 99: no existe");
        verify(productoClient, never()).actualizarStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("mensaje duplicado de una orden ya procesada e informada: se ignora sin tocar nada")
    void testDuplicadoYaInformado() {
        when(facturacionService.buscarProcesamiento(1L))
                .thenReturn(Optional.of(procesamiento(ResultadoProcesamiento.ReadyToDelivery, true)));

        procesamientoService.procesar(mensaje(1L));

        verifyNoInteractions(ordenClient, productoClient);
        verify(facturacionService, never()).marcarOrdenActualizada(anyLong());
    }

    @Test
    @DisplayName("duplicado de una orden procesada cuyo estado no llegó a órdenes: solo reintenta informarlo")
    void testDuplicadoPendienteDeInformar() {
        when(facturacionService.buscarProcesamiento(1L))
                .thenReturn(Optional.of(procesamiento(ResultadoProcesamiento.ReadyToDelivery, false)));
        when(ordenClient.actualizarEstado(1L, ResultadoProcesamiento.ReadyToDelivery)).thenReturn(true);

        procesamientoService.procesar(mensaje(1L));

        verify(ordenClient).actualizarEstado(1L, ResultadoProcesamiento.ReadyToDelivery);
        verify(facturacionService).marcarOrdenActualizada(10L);
        verifyNoInteractions(productoClient);
        verify(ordenClient, never()).obtenerOrden(anyLong());
    }

    @Test
    @DisplayName("si la orden ya no está en Created no se procesa")
    void testOrdenYaNoEstaEnCreated() {
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden("Ready to Delivery", linea(1, 1))));

        procesamientoService.procesar(mensaje(1L));

        verifyNoInteractions(productoClient);
        verify(facturacionService, never()).registrarListaParaEntrega(any(), any(), any());
        verify(facturacionService, never()).registrarSinStock(anyLong(), any());
    }

    @Test
    @DisplayName("si la orden no existe se descarta el mensaje")
    void testOrdenInexistente() {
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.empty());

        procesamientoService.procesar(mensaje(1L));

        verifyNoInteractions(productoClient);
    }

    @Test
    @DisplayName("si falla el descuento de un producto, devuelve el stock ya descontado y propaga el error")
    void testCompensaSiFallaElDescuento() {
        OrdenResponse orden = orden("Created", linea(1, 2), linea(2, 5));
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(1L))
                .thenReturn(Optional.of(producto(1, "Notebook", 1250.50, 15)))
                .thenReturn(Optional.of(producto(1, "Notebook", 1250.50, 13))); // relectura al restaurar
        when(productoClient.obtenerProducto(2L)).thenReturn(Optional.of(producto(2, "Mouse", 99.90, 40)));
        // lenient: el mismo método se invoca con otros argumentos (descuento y restauración del producto 1)
        lenient().doThrow(new ServicioExternoException("productos caído"))
                .when(productoClient).actualizarStock(2L, 35);

        assertThatThrownBy(() -> procesamientoService.procesar(mensaje(1L)))
                .isInstanceOf(ServicioExternoException.class);

        verify(productoClient).actualizarStock(1L, 13);     // descuento del primero
        verify(productoClient).actualizarStock(1L, 15);     // restauración: 13 + 2
        verify(facturacionService, never()).registrarListaParaEntrega(any(), any(), any());
        verify(ordenClient, never()).actualizarEstado(anyLong(), any());
    }

    @Test
    @DisplayName("si falla al registrar la factura, restaura el stock descontado")
    void testCompensaSiFallaLaFactura() {
        OrdenResponse orden = orden("Created", linea(1, 2));
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(1L))
                .thenReturn(Optional.of(producto(1, "Notebook", 1250.50, 15)))
                .thenReturn(Optional.of(producto(1, "Notebook", 1250.50, 13)));
        when(facturacionService.registrarListaParaEntrega(any(), any(), any()))
                .thenThrow(new IllegalStateException("BD caída"));

        assertThatThrownBy(() -> procesamientoService.procesar(mensaje(1L)))
                .isInstanceOf(IllegalStateException.class);

        verify(productoClient).actualizarStock(1L, 13);
        verify(productoClient).actualizarStock(1L, 15);
        verify(ordenClient, never()).actualizarEstado(anyLong(), any());
    }

    @Test
    @DisplayName("si órdenes responde 409 al informar el estado, igual se da por resuelta")
    void testConflictoAlInformarEstado() {
        OrdenResponse orden = orden("Created", linea(1, 1));
        when(facturacionService.buscarProcesamiento(1L)).thenReturn(Optional.empty());
        when(ordenClient.obtenerOrden(1L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(1L)).thenReturn(Optional.of(producto(1, "Notebook", 100.0, 5)));
        when(facturacionService.registrarListaParaEntrega(any(), any(), any()))
                .thenReturn(procesamiento(ResultadoProcesamiento.ReadyToDelivery, false));
        when(ordenClient.actualizarEstado(1L, ResultadoProcesamiento.ReadyToDelivery)).thenReturn(false);

        procesamientoService.procesar(mensaje(1L));

        verify(facturacionService, times(1)).marcarOrdenActualizada(10L);
    }
}
