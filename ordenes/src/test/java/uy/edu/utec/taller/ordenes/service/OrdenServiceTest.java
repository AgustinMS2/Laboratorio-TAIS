package uy.edu.utec.taller.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.utec.taller.ordenes.dto.OrdenDTO;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;
import uy.edu.utec.taller.ordenes.model.LineaOrden;
import uy.edu.utec.taller.ordenes.model.Orden;
import uy.edu.utec.taller.ordenes.repository.OrdenRepository;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenRepository;

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
}
