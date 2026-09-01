package uy.edu.utec.taller.ordenes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.ordenes.model.LineaOrden;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaOrdenDTO {

    private Long productoId;
    private Integer cantidad;

    public static LineaOrdenDTO fromEntity(LineaOrden linea) {
        if (linea == null) {
            return null;
        }
        return LineaOrdenDTO.builder()
                .productoId(linea.getProductoId())
                .cantidad(linea.getCantidad())
                .build();
    }
}
