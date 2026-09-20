package uy.edu.utec.taller.publicador.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.publicador.client.dto.OrdenResponse;

/**
 * Mensaje JSON publicado en el broker por cada orden lista para procesar:
 * {@code {"id": 1, "estado": "Created", "fechaCreacion": "2026-06-21T14:30:00-03:00"}}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenMensajeDTO {

    private Long id;
    private String estado;
    private String fechaCreacion;

    public static OrdenMensajeDTO fromOrden(OrdenResponse orden) {
        return OrdenMensajeDTO.builder()
                .id(orden.getId())
                .estado(orden.getEstado())
                .fechaCreacion(orden.getFechaCreacion())
                .build();
    }
}
