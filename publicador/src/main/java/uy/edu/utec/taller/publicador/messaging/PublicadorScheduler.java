package uy.edu.utec.taller.publicador.messaging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uy.edu.utec.taller.publicador.exception.ServicioExternoException;
import uy.edu.utec.taller.publicador.service.PublicadorService;

/**
 * Dispara periódicamente la publicación de las órdenes pendientes. Se puede desactivar con
 * {@code mensajeria.publicador.enabled=false} (así lo hacen los tests). Nunca lanza: si el servicio
 * de órdenes o el broker no están, se registra y se reintenta en el siguiente ciclo.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mensajeria.publicador.enabled", havingValue = "true", matchIfMissing = true)
public class PublicadorScheduler {

    private static final Logger log = LoggerFactory.getLogger(PublicadorScheduler.class);

    private final PublicadorService publicadorService;

    @Scheduled(fixedDelayString = "${mensajeria.publicador.intervalo-ms:2000}",
            initialDelayString = "${mensajeria.publicador.intervalo-ms:2000}")
    public void publicarPendientes() {
        try {
            publicadorService.publicarPendientes();
        } catch (ServicioExternoException ex) {
            log.warn("No se pudieron consultar las órdenes pendientes: {}. Se reintenta.", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Error inesperado al publicar órdenes pendientes", ex);
        }
    }
}
