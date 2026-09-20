package uy.edu.utec.taller.publicador.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Orden tal como la devuelve {@code GET /api/ordenes?estado=Created} del servicio de órdenes. Solo
 * interesan los tres datos que viajan en el mensaje. {@code fechaCreacion} se conserva como texto
 * para publicarla exactamente como la emitió el servicio de órdenes (con su offset horario).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdenResponse {

    private Long id;
    private String estado;
    private String fechaCreacion;
}
