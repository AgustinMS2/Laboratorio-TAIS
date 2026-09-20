package uy.edu.utec.taller.procesamiento.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import uy.edu.utec.taller.procesamiento.dto.OrdenMensajeDTO;
import uy.edu.utec.taller.procesamiento.exception.ServicioExternoException;
import uy.edu.utec.taller.procesamiento.service.ProcesamientoService;

@ExtendWith(MockitoExtension.class)
class MqttOrdenSubscriberTest {

    @Mock
    private ProcesamientoService procesamientoService;

    private MqttOrdenSubscriber subscriber;

    @BeforeEach
    void setUp() throws Exception {
        // El constructor no se conecta al broker; espera 0 ms para que los reintentos no demoren el test.
        subscriber = new MqttOrdenSubscriber(JsonMapper.builder().build(), procesamientoService,
                "tcp://localhost:1883", "ordenes/procesar", 1, "procesamiento-test", 3, 0);
    }

    private static MqttMessage mqtt(String json) {
        return new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("un mensaje con id, estado y fechaCreacion se convierte y se procesa")
    void testMensajeValido() {
        subscriber.messageArrived("ordenes/procesar",
                mqtt("{\"id\": 1, \"estado\": \"Created\", \"fechaCreacion\": \"2026-06-21T14:30:00-03:00\"}"));

        ArgumentCaptor<OrdenMensajeDTO> captor = ArgumentCaptor.forClass(OrdenMensajeDTO.class);
        verify(procesamientoService).procesar(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getEstado()).isEqualTo("Created");
        // Jackson normaliza el offset (queda en UTC): se compara el instante, que es el mismo.
        assertThat(captor.getValue().getFechaCreacion().toInstant())
                .isEqualTo(OffsetDateTime.parse("2026-06-21T14:30:00-03:00").toInstant());
    }

    @Test
    @DisplayName("un mensaje que no es JSON válido o no trae id se descarta sin procesar")
    void testMensajeInvalido() {
        subscriber.messageArrived("ordenes/procesar", mqtt("esto no es json"));
        subscriber.messageArrived("ordenes/procesar", mqtt("{\"estado\": \"Created\"}"));

        verify(procesamientoService, never()).procesar(any());
    }

    @Test
    @DisplayName("ante fallos transitorios de otros servicios reintenta hasta tener éxito")
    void testReintentaHastaExito() {
        doThrow(new ServicioExternoException("ordenes caído"))
                .doThrow(new ServicioExternoException("ordenes caído"))
                .doNothing()
                .when(procesamientoService).procesar(any());

        subscriber.messageArrived("ordenes/procesar", mqtt("{\"id\": 5, \"estado\": \"Created\"}"));

        verify(procesamientoService, times(3)).procesar(any());
    }

    @Test
    @DisplayName("si se agotan los reintentos no lanza excepción (no debe romper la conexión MQTT)")
    void testAgotaReintentosSinLanzar() {
        doThrow(new ServicioExternoException("ordenes caído")).when(procesamientoService).procesar(any());

        assertThatCode(() -> subscriber.messageArrived("ordenes/procesar", mqtt("{\"id\": 5}")))
                .doesNotThrowAnyException();

        verify(procesamientoService, times(3)).procesar(any());
    }

    @Test
    @DisplayName("un error inesperado se registra, no se reintenta y no se propaga")
    void testErrorInesperadoNoSePropaga() {
        doThrow(new IllegalStateException("bug")).when(procesamientoService).procesar(any());

        assertThatCode(() -> subscriber.messageArrived("ordenes/procesar", mqtt("{\"id\": 5}")))
                .doesNotThrowAnyException();

        verify(procesamientoService, times(1)).procesar(any());
    }

    @Test
    @DisplayName("un mensaje duplicado se entrega otra vez al servicio, que es quien lo trata de forma idempotente")
    void testDuplicadoSeEntrega() {
        doNothing().when(procesamientoService).procesar(any());

        subscriber.messageArrived("ordenes/procesar", mqtt("{\"id\": 5, \"estado\": \"Created\"}"));
        subscriber.messageArrived("ordenes/procesar", mqtt("{\"id\": 5, \"estado\": \"Created\"}"));

        verify(procesamientoService, times(2)).procesar(any());
    }
}
