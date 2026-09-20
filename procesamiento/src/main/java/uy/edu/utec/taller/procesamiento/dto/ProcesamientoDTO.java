package uy.edu.utec.taller.procesamiento.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.procesamiento.model.Procesamiento;
import uy.edu.utec.taller.procesamiento.model.ResultadoProcesamiento;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesamientoDTO {

    private Long id;
    private Long ordenId;
    private ResultadoProcesamiento resultado;
    private OffsetDateTime fechaProcesamiento;
    private boolean ordenActualizada;
    private String detalle;

    public static ProcesamientoDTO fromEntity(Procesamiento p) {
        return ProcesamientoDTO.builder()
                .id(p.getId())
                .ordenId(p.getOrdenId())
                .resultado(p.getResultado())
                .fechaProcesamiento(p.getFechaProcesamiento())
                .ordenActualizada(p.isOrdenActualizada())
                .detalle(p.getDetalle())
                .build();
    }
}
