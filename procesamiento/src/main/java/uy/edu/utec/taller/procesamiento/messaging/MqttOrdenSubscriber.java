package uy.edu.utec.taller.procesamiento.messaging;

import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import uy.edu.utec.taller.procesamiento.dto.OrdenMensajeDTO;
import uy.edu.utec.taller.procesamiento.exception.ServicioExternoException;
import uy.edu.utec.taller.procesamiento.service.ProcesamientoService;

/**
 * Se suscribe al tópico de órdenes en Mosquitto (MQTT) y procesa cada mensaje.
 *
 * <ul>
 *   <li>Sesión persistente ({@code cleanSession=false}) con clientId fijo y QoS 1: si el
 *       procesador está caído, el broker retiene los mensajes y los entrega al volver.</li>
 *   <li>La conexión se (re)establece desde un {@code @Scheduled}, de modo que el servicio arranca
 *       aunque el broker todavía no esté disponible.</li>
 *   <li>Los mensajes de un mismo cliente llegan en serie, así que las órdenes se procesan de a una.</li>
 * </ul>
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "mensajeria.suscriptor.enabled", havingValue = "true", matchIfMissing = true)
public class MqttOrdenSubscriber implements MqttCallback, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MqttOrdenSubscriber.class);

    private final JsonMapper jsonMapper;
    private final ProcesamientoService procesamientoService;
    private final String brokerUrl;
    private final String topic;
    private final int qos;
    private final int maxIntentos;
    private final long esperaMs;
    private final MqttClient client;
    private final MqttConnectOptions opciones;

    public MqttOrdenSubscriber(
            JsonMapper jsonMapper,
            ProcesamientoService procesamientoService,
            @Value("${mensajeria.mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
            @Value("${mensajeria.mqtt.topic:ordenes/procesar}") String topic,
            @Value("${mensajeria.mqtt.qos:1}") int qos,
            @Value("${mensajeria.mqtt.client-id:procesamiento}") String clientId,
            @Value("${mensajeria.suscriptor.reintentos:3}") int maxIntentos,
            @Value("${mensajeria.suscriptor.espera-ms:2000}") long esperaMs) throws MqttException {
        this.jsonMapper = jsonMapper;
        this.procesamientoService = procesamientoService;
        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.qos = qos;
        this.maxIntentos = maxIntentos;
        this.esperaMs = esperaMs;
        this.client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        this.client.setCallback(this);
        this.opciones = new MqttConnectOptions();
        this.opciones.setCleanSession(false);
        this.opciones.setConnectionTimeout(3);
        this.opciones.setKeepAliveInterval(30);
    }

    /** Conecta y suscribe si no hay conexión; se ejecuta periódicamente para reconectar tras una caída. */
    @Scheduled(fixedDelayString = "${mensajeria.suscriptor.reconexion-ms:5000}", initialDelay = 0)
    public synchronized void asegurarConexion() {
        if (client.isConnected()) {
            return;
        }
        try {
            client.connect(opciones);
            client.subscribe(topic, qos);
            log.info("Suscripto a '{}' en {} (QoS {})", topic, brokerUrl, qos);
        } catch (MqttException ex) {
            log.warn("No se pudo conectar/suscribir al broker {}: {}. Se reintenta.", brokerUrl, ex.getMessage());
        }
    }

    @Override
    public void messageArrived(String topico, MqttMessage mensaje) {
        String contenido = new String(mensaje.getPayload(), StandardCharsets.UTF_8);
        OrdenMensajeDTO orden;
        try {
            orden = jsonMapper.readValue(contenido, OrdenMensajeDTO.class);
        } catch (JacksonException ex) {
            log.error("Mensaje inválido descartado en '{}': {}", topico, contenido);
            return;
        }
        if (orden.getId() == null) {
            log.error("Mensaje sin id descartado en '{}': {}", topico, contenido);
            return;
        }
        log.info("Mensaje recibido en '{}': orden {} ({})", topico, orden.getId(), orden.getEstado());
        procesarConReintentos(orden);
    }

    /**
     * Reintenta ante fallos transitorios de otros servicios. Nunca lanza: una excepción en el
     * callback haría que Paho cierre la conexión. Si se agotan los intentos, la orden sigue en
     * {@code Created} y el servicio de órdenes la vuelve a publicar más adelante.
     */
    void procesarConReintentos(OrdenMensajeDTO orden) {
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                procesamientoService.procesar(orden);
                return;
            } catch (ServicioExternoException ex) {
                log.warn("Orden {}: intento {}/{} fallido: {}", orden.getId(), intento, maxIntentos, ex.getMessage());
                if (intento < maxIntentos && !dormir(esperaMs * intento)) {
                    return;
                }
            } catch (RuntimeException ex) {
                log.error("Orden {}: error inesperado al procesar; se descarta este mensaje", orden.getId(), ex);
                return;
            }
        }
        log.error("Orden {} sin procesar tras {} intentos; se reintentará cuando órdenes la vuelva a publicar",
                orden.getId(), maxIntentos);
    }

    private boolean dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void connectionLost(Throwable causa) {
        log.warn("Conexión con el broker perdida: {}. Se reconectará automáticamente.", causa.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Solo se reciben mensajes; no hay entregas propias que confirmar.
    }

    @Override
    public void destroy() {
        try {
            if (client.isConnected()) {
                client.disconnect(1000);
            }
            client.close();
        } catch (MqttException ex) {
            log.debug("Error al cerrar la conexión MQTT", ex);
        }
    }
}
