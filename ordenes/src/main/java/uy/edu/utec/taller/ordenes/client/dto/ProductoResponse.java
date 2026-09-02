package uy.edu.utec.taller.ordenes.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representación del recurso Producto tal como lo expone el microservicio de
 * Productos en {@code GET /api/productos/{id}}.
 */
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
    private List<String> imagenes;
}
