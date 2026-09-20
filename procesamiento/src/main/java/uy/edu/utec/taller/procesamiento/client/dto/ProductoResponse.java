package uy.edu.utec.taller.procesamiento.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Double precioUnitario;
    private Integer stock;
}
