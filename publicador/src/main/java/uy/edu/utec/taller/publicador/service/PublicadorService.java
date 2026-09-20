package uy.edu.utec.taller.publicador.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uy.edu.utec.taller.publicador.client.OrdenClient;
import uy.edu.utec.taller.publicador.client.dto.OrdenResponse;
import uy.edu.utec.taller.publicador.dto.OrdenMensajeDTO;
import uy.edu.utec.taller.publicador.dto.PublicacionDTO;
import uy.edu.utec.taller.publicador.exception.MensajeriaException;
import uy.edu.utec.taller.publicador.messaging.MqttOrdenPublisher;
import uy.edu.utec.taller.publicador.model.Publicacion;

/**
 * Publica en el broker las órdenes que están listas para procesar (estado {@code Created}).
 *
 * <p>En cada pasada consulta al servicio de órdenes las que están en {@code Created} y publica las
 * que nunca publicó o cuya última publicación es más vieja que {@code reintento-segundos} (siguen en
 * {@code Created}: el procesador no las resolvió, p. ej. porque estaba caído). El procesador es
 * idempotente, por lo que un mensaje duplicado es inocuo.
 */
@Service
public class PublicadorService {

    private static final Logger log = LoggerFactory.getLogger(PublicadorService.class);

    static final String ESTADO_LISTA_PARA_PROCESAR = "Created";
    private static final int MAX_REGISTROS = 1000;

    private final OrdenClient ordenClient;
    private final MqttOrdenPublisher publisher;
    private final Clock clock;
    private final Duration reintento;
    private final int lote;

    /** Última publicación por orden; acotado para no crecer sin límite. */
    private final Map<Long, Publicacion> registro = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Publicacion> eldest) {
                    return size() > MAX_REGISTROS;
                }
            });

    public PublicadorService(
            OrdenClient ordenClient,
            MqttOrdenPublisher publisher,
            Clock clock,
            @Value("${mensajeria.publicador.reintento-segundos:120}") long reintentoSegundos,
            @Value("${mensajeria.publicador.lote:50}") int lote) {
        this.ordenClient = ordenClient;
        this.publisher = publisher;
        this.clock = clock;
        this.reintento = Duration.ofSeconds(reintentoSegundos);
        this.lote = lote;
    }

    /**
     * @return cantidad de órdenes publicadas en esta pasada.
     * @throws uy.edu.utec.taller.publicador.exception.ServicioExternoException si no puede consultar las órdenes.
     */
    public int publicarPendientes() {
        List<OrdenResponse> pendientes = ordenClient.listarPorEstado(ESTADO_LISTA_PARA_PROCESAR);
        OffsetDateTime ahora = OffsetDateTime.now(clock);

        int publicadas = 0;
        for (OrdenResponse orden : pendientes) {
            if (publicadas >= lote) {
                break;
            }
            Publicacion previa = registro.get(orden.getId());
            if (previa != null && previa.getUltimaPublicacion().plus(reintento).isAfter(ahora)) {
                continue;
            }
            try {
                publisher.publicar(OrdenMensajeDTO.fromOrden(orden));
            } catch (MensajeriaException ex) {
                // Broker caído: no insistir con el resto; se reintenta en la próxima pasada.
                log.warn("{} ({} orden(es) quedan pendientes)", ex.getMessage(), pendientes.size() - publicadas);
                break;
            }
            registro.put(orden.getId(), Publicacion.builder()
                    .ordenId(orden.getId())
                    .ultimaPublicacion(ahora)
                    .veces(previa == null ? 1 : previa.getVeces() + 1)
                    .build());
            publicadas++;
        }
        return publicadas;
    }

    /** Órdenes que este servicio publicó (desde que arrancó), por id de orden. */
    public List<PublicacionDTO> listarPublicaciones() {
        List<Publicacion> copia;
        synchronized (registro) {
            copia = new ArrayList<>(registro.values());
        }
        return copia.stream()
                .sorted(Comparator.comparing(Publicacion::getOrdenId))
                .map(PublicacionDTO::fromModel)
                .toList();
    }
}
