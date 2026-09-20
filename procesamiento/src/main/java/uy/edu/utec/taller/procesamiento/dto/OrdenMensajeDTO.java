package uy.edu.utec.taller.procesamiento.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje JSON recibido del broker: {@code {"id": 1, "estado": "Created", "fechaCreacion": "..."}}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdenMensajeDTO {

    private Long id;
    private String estado;
    private OffsetDateTime fechaCreacion;
}
