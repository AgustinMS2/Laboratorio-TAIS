package uy.edu.utec.taller.ordenes.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenEstadoUpdateDTO {

    @NotNull(message = "El campo estado es obligatorio")
    private EstadoOrden estado;
}
