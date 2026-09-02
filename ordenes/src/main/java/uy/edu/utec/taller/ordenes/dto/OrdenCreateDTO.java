package uy.edu.utec.taller.ordenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCreateDTO {

    @NotBlank(message = "El campo email es obligatorio")
    @Email(message = "El campo email no tiene un formato válido")
    @Size(max = 150, message = "El campo email no puede superar los 150 caracteres")
    private String email;

    @NotBlank(message = "El campo direccionEnvio es obligatorio")
    @Size(max = 250, message = "El campo direccionEnvio no puede superar los 250 caracteres")
    private String direccionEnvio;

    @NotBlank(message = "El campo telefono es obligatorio")
    @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$",
            message = "El campo telefono no tiene un formato válido")
    private String telefono;

    @NotEmpty(message = "La orden debe incluir al menos un producto")
    @Valid
    private List<LineaOrdenCreateDTO> productos;
}
