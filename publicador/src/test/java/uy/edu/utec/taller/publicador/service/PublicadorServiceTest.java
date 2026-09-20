package uy.edu.utec.taller.publicador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.utec.taller.publicador.client.OrdenClient;
import uy.edu.utec.taller.publicador.client.dto.OrdenResponse;
import uy.edu.utec.taller.publicador.dto.OrdenMensajeDTO;
import uy.edu.utec.taller.publicador.dto.PublicacionDTO;
import uy.edu.utec.taller.publicador.exception.MensajeriaException;
import uy.edu.utec.taller.publicador.exception.ServicioExternoException;
import uy.edu.utec.taller.publicador.messaging.MqttOrdenPublisher;

@ExtendWith(MockitoExtension.class)
class PublicadorServiceTest {

    /** Reloj cuyo tiempo se avanza a mano, para probar los reintentos sin esperar. */
    static class RelojMutable extends Clock {
        private Instant ahora = Instant.parse("2026-09-19T20:00:00Z");

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Mock
    private OrdenClient ordenClient;

    @Mock
    private MqttOrdenPublisher publisher;

    private RelojMutable reloj;
    private PublicadorService service;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable();
        service = new PublicadorService(ordenClient, publisher, reloj, 120, 50);
    }

    private static OrdenResponse orden(long id) {
        return OrdenResponse.builder().id(id).estado("Created").fechaCreacion("2026-06-21T14:30:00-03:00").build();
    }

    @Test
    @DisplayName("publica cada orden en Created con id, estado y fecha tal como los emitió órdenes")
    void testPublicaPendientes() {
        when(ordenClient.listarPorEstado("Created")).thenReturn(List.of(orden(1L), orden(2L)));

        int publicadas = service.publicarPendientes();

        assertThat(publicadas).isEqualTo(2);
        ArgumentCaptor<OrdenMensajeDTO> mensajes = ArgumentCaptor.forClass(OrdenMensajeDTO.class);
        verify(publisher, times(2)).publicar(mensajes.capture());
        OrdenMensajeDTO primero = mensajes.getAllValues().getFirst();
        assertThat(primero.getId()).isEqualTo(1L);
        assertThat(primero.getEstado()).isEqualTo("Created");
        assertThat(primero.getFechaCreacion()).isEqualTo("2026-06-21T14:30:00-03:00");
    }

    @Test
    @DisplayName("no republica una orden publicada hace menos de reintento-segundos")
    void testNoRepublicaDentroDeLaVentana() {
        when(ordenClient.listarPorEstado("Created")).thenReturn(List.of(orden(1L)));

        service.publicarPendientes();
        reloj.avanzar(Duration.ofSeconds(60));
        int segunda = service.publicarPendientes();

        assertThat(segunda).isZero();
        verify(publisher, times(1)).publicar(any());
    }

    @Test
    @DisplayName("republica una orden que sigue en Created pasado reintento-segundos y cuenta las veces")
    void testRepublicaPasadaLaVentana() {
        when(ordenClient.listarPorEstado("Created")).thenReturn(List.of(orden(1L)));

        service.publicarPendientes();
        reloj.avanzar(Duration.ofSeconds(121));
        int segunda = service.publicarPendientes();

        assertThat(segunda).isEqualTo(1);
        verify(publisher, times(2)).publicar(any());
        List<PublicacionDTO> registro = service.listarPublicaciones();
        assertThat(registro).hasSize(1);
        assertThat(registro.getFirst().getOrdenId()).isEqualTo(1L);
        assertThat(registro.getFirst().getVeces()).isEqualTo(2);
    }

    @Test
    @DisplayName("si el broker no responde, no registra la orden como publicada y corta la pasada")
    void testBrokerCaido() {
        when(ordenClient.listarPorEstado("Created")).thenReturn(List.of(orden(1L), orden(2L)));
        doThrow(new MensajeriaException("broker caído", new RuntimeException()))
                .when(publisher).publicar(any(OrdenMensajeDTO.class));

        int publicadas = service.publicarPendientes();

        assertThat(publicadas).isZero();
        assertThat(service.listarPublicaciones()).isEmpty();
        verify(publisher, times(1)).publicar(any());
    }

    @Test
    @DisplayName("tras una caída del broker, la siguiente pasada publica las órdenes pendientes")
    void testSeRecuperaCuandoVuelveElBroker() {
        when(ordenClient.listarPorEstado("Created")).thenReturn(List.of(orden(1L)));
        doThrow(new MensajeriaException("broker caído", new RuntimeException()))
                .doNothing()
                .when(publisher).publicar(any(OrdenMensajeDTO.class));

        assertThat(service.publicarPendientes()).isZero();
        assertThat(service.publicarPendientes()).isEqualTo(1);
        assertThat(service.listarPublicaciones()).hasSize(1);
    }

    @Test
    @DisplayName("si el servicio de órdenes no responde, propaga el error sin publicar nada")
    void testOrdenesCaido() {
        when(ordenClient.listarPorEstado("Created")).thenThrow(new ServicioExternoException("órdenes caído"));

        assertThatThrownBy(() -> service.publicarPendientes()).isInstanceOf(ServicioExternoException.class);

        verify(publisher, never()).publicar(any());
    }

    @Test
    @DisplayName("respeta el tamaño de lote por pasada")
    void testRespetaElLote() {
        service = new PublicadorService(ordenClient, publisher, reloj, 120, 2);
        when(ordenClient.listarPorEstado("Created")).thenReturn(List.of(orden(1L), orden(2L), orden(3L)));

        assertThat(service.publicarPendientes()).isEqualTo(2);
        // la tercera se publica en la pasada siguiente (las dos primeras aún están dentro de la ventana)
        assertThat(service.publicarPendientes()).isEqualTo(1);
        verify(publisher, times(3)).publicar(any());
    }
}
