package uy.edu.utec.taller.procesamiento.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Orden tal como la devuelve {@code GET /api/ordenes/{id}} del servicio de órdenes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdenResponse {

    private Long id;
    private String email;
    private String direccionEnvio;
    private String telefono;
    /** Texto del estado tal como viaja en la API, p. ej. {@code "Created"}. */
    private String estado;
    private OffsetDateTime fechaCreacion;
    private List<LineaOrdenResponse> productos;
}
