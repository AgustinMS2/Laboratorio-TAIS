package uy.edu.utec.taller.publicador.messaging;

import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import uy.edu.utec.taller.publicador.dto.OrdenMensajeDTO;
import uy.edu.utec.taller.publicador.exception.MensajeriaException;

/**
 * Publica en el broker MQTT (Eclipse Mosquitto) el mensaje JSON de cada orden lista para procesar.
 * La conexión es perezosa: crear el bean no requiere que el broker esté arriba. Se publica con
 * QoS 1 (al menos una vez) y la llamada bloquea hasta recibir el PUBACK del broker.
 */
@Component
public class MqttOrdenPublisher implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MqttOrdenPublisher.class);

    private final JsonMapper jsonMapper;
    private final String topic;
    private final int qos;
    private final MqttClient client;
    private final MqttConnectOptions opciones;

    public MqttOrdenPublisher(
            JsonMapper jsonMapper,
            @Value("${mensajeria.mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
            @Value("${mensajeria.mqtt.topic:ordenes/procesar}") String topic,
            @Value("${mensajeria.mqtt.qos:1}") int qos) throws MqttException {
        this.jsonMapper = jsonMapper;
        this.topic = topic;
        this.qos = qos;
        this.client = new MqttClient(brokerUrl, "publicador-" + UUID.randomUUID(), new MemoryPersistence());
        this.opciones = new MqttConnectOptions();
        this.opciones.setCleanSession(true);
        this.opciones.setAutomaticReconnect(true);
        this.opciones.setConnectionTimeout(3);
        this.opciones.setKeepAliveInterval(30);
    }

    /**
     * Publica la orden y espera la confirmación del broker.
     *
     * @throws MensajeriaException si el broker no está disponible o no confirma la publicación.
     */
    public synchronized void publicar(OrdenMensajeDTO mensaje) {
        try {
            if (!client.isConnected()) {
                client.connect(opciones);
            }
            MqttMessage payload = new MqttMessage(jsonMapper.writeValueAsBytes(mensaje));
            payload.setQos(qos);
            payload.setRetained(false);
            client.publish(topic, payload);
            log.info("Orden {} publicada en '{}' (estado {})", mensaje.getId(), topic, mensaje.getEstado());
        } catch (MqttException | JacksonException ex) {
            throw new MensajeriaException("No se pudo publicar la orden " + mensaje.getId()
                    + " en el broker de mensajería", ex);
        }
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
