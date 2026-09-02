package uy.edu.utec.taller.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.ordenes.client.dto.ProductoResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineaOrdenDetalleDTO {

    private Integer cantidad;
    private Double subtotal;
    private ProductoResponse producto;
}
