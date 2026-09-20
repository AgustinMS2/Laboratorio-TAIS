package uy.edu.utec.taller.publicador.model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * Registro (en memoria) de que una orden se publicó en el broker: cuándo fue la última vez y cuántas
 * veces. Sirve para no republicar una orden en cada ciclo y para reintentarla si sigue pendiente.
 * Se pierde al reiniciar el servicio, lo que solo provoca una republicación (el procesador es idempotente).
 */
@Value
@Builder
public class Publicacion {

    Long ordenId;
    OffsetDateTime ultimaPublicacion;
    int veces;
}
