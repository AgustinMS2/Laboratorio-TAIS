package uy.edu.utec.taller.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.utec.taller.ordenes.client.ProductoClient;
import uy.edu.utec.taller.ordenes.client.dto.ProductoResponse;
import uy.edu.utec.taller.ordenes.dto.OrdenDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDetalleDTO;
import uy.edu.utec.taller.ordenes.exception.OrdenNoEncontradaException;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;
import uy.edu.utec.taller.ordenes.model.LineaOrden;
import uy.edu.utec.taller.ordenes.model.Orden;
import uy.edu.utec.taller.ordenes.repository.OrdenRepository;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenRepository;

    @Mock
    private ProductoClient productoClient;

    @InjectMocks
    private OrdenService ordenService;

    @Test
    @DisplayName("listarOrdenes debe retornar lista mapeada de DTOs")
    void testListarOrdenes() {
        Orden orden = Orden.builder()
                .id(1001L)
                .email("cliente@email.com")
                .direccionEnvio("Av. Italia 3333, Maldonado")
                .telefono("+59899111222")
                .estado(EstadoOrden.Created)
                .fechaCreacion(OffsetDateTime.parse("2026-06-21T14:30:00-03:00"))
                .productos(List.of(
                        LineaOrden.builder().productoId(1L).cantidad(2).build(),
                        LineaOrden.builder().productoId(2L).cantidad(5).build()))
                .build();

        when(ordenRepository.findAll()).thenReturn(List.of(orden));

        List<OrdenDTO> resultado = ordenService.listarOrdenes();

        assertThat(resultado).hasSize(1);
        OrdenDTO dto = resultado.getFirst();
        assertThat(dto.getId()).isEqualTo(1001L);
        assertThat(dto.getEmail()).isEqualTo("cliente@email.com");
        assertThat(dto.getEstado()).isEqualTo(EstadoOrden.Created);
        assertThat(dto.getProductos()).hasSize(2);
        assertThat(dto.getProductos().getFirst().getProductoId()).isEqualTo(1L);
        assertThat(dto.getProductos().getFirst().getCantidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("listarOrdenes retorna lista vacía si no hay órdenes")
    void testListarOrdenesVacio() {
        when(ordenRepository.findAll()).thenReturn(List.of());

        List<OrdenDTO> resultado = ordenService.listarOrdenes();

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("obtenerOrden devuelve el DTO cuando la orden existe")
    void testObtenerOrdenExistente() {
        Orden orden = Orden.builder()
                .id(1001L)
                .email("cliente@email.com")
                .direccionEnvio("Av. Italia 3333, Maldonado")
                .telefono("+59899111222")
                .estado(EstadoOrden.Created)
                .fechaCreacion(OffsetDateTime.parse("2026-06-21T14:30:00-03:00"))
                .productos(List.of(
                        LineaOrden.builder().productoId(1L).cantidad(2).build(),
                        LineaOrden.builder().productoId(2L).cantidad(5).build()))
                .build();

        when(ordenRepository.findById(1001L)).thenReturn(Optional.of(orden));

        OrdenDTO dto = ordenService.obtenerOrden(1001L);

        assertThat(dto.getId()).isEqualTo(1001L);
        assertThat(dto.getEmail()).isEqualTo("cliente@email.com");
        assertThat(dto.getEstado()).isEqualTo(EstadoOrden.Created);
        assertThat(dto.getProductos()).hasSize(2);
    }

    @Test
    @DisplayName("obtenerOrden lanza OrdenNoEncontradaException cuando no existe")
    void testObtenerOrdenInexistente() {
        when(ordenRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenService.obtenerOrden(9999L))
                .isInstanceOf(OrdenNoEncontradaException.class)
                .hasMessage("No existe la orden con id 9999");
    }

    @Test
    @DisplayName("obtenerDetalleOrden compone cada línea con el producto, su subtotal y el total")
    void testObtenerDetalleOrden() {
        Orden orden = Orden.builder()
                .id(1001L)
                .email("cliente@email.com")
                .direccionEnvio("Av. Italia 3333, Maldonado")
                .telefono("+59899111222")
                .estado(EstadoOrden.Created)
                .fechaCreacion(OffsetDateTime.parse("2026-06-21T14:30:00-03:00"))
                .productos(List.of(
                        LineaOrden.builder().productoId(1L).cantidad(2).build(),
                        LineaOrden.builder().productoId(2L).cantidad(5).build()))
                .build();

        when(ordenRepository.findById(1001L)).thenReturn(Optional.of(orden));
        when(productoClient.obtenerProducto(1L)).thenReturn(Optional.of(ProductoResponse.builder()
                .id(1L).nombre("Notebook Lenovo ThinkPad").precioUnitario(1250.50).stock(13).build()));
        when(productoClient.obtenerProducto(2L)).thenReturn(Optional.of(ProductoResponse.builder()
                .id(2L).nombre("Mouse Logitech MX Master 3").precioUnitario(99.90).stock(35).build()));

        OrdenDetalleDTO detalle = ordenService.obtenerDetalleOrden(1001L);

        assertThat(detalle.getId()).isEqualTo(1001L);
        assertThat(detalle.getProductos()).hasSize(2);
        assertThat(detalle.getProductos().get(0).getCantidad()).isEqualTo(2);
        assertThat(detalle.getProductos().get(0).getSubtotal()).isEqualTo(2501.00);
        assertThat(detalle.getProductos().get(0).getProducto().getNombre()).isEqualTo("Notebook Lenovo ThinkPad");
        assertThat(detalle.getProductos().get(1).getSubtotal()).isEqualTo(499.50);
        assertThat(detalle.getTotal()).isEqualTo(3000.50);
    }

    @Test
    @DisplayName("obtenerDetalleOrden lanza OrdenNoEncontradaException cuando no existe")
    void testObtenerDetalleOrdenInexistente() {
        when(ordenRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenService.obtenerDetalleOrden(9999L))
                .isInstanceOf(OrdenNoEncontradaException.class)
                .hasMessage("No existe la orden con id 9999");
    }
}
