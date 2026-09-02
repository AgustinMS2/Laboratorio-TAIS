package uy.edu.utec.taller.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDTO {

    private Integer codigo;
    private String mensaje;
    private List<String> detalles;

    public static ErrorDTO of(int codigo, String mensaje) {
        return ErrorDTO.builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .build();
    }
}
