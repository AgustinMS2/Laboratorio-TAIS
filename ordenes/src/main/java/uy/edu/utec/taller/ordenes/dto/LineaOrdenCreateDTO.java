package uy.edu.utec.taller.ordenes.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaOrdenCreateDTO {

    @NotNull(message = "El campo productoId es obligatorio")
    private Long productoId;

    @NotNull(message = "El campo cantidad es obligatorio")
    @Min(value = 1, message = "El campo cantidad debe ser mayor o igual a 1")
    private Integer cantidad;
}
