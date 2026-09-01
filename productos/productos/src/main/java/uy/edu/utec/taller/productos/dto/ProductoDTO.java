package uy.edu.utec.taller.productos.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.productos.model.Producto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Double precioUnitario;
    private Integer stock;
    private List<String> imagenes;

    public static ProductoDTO fromEntity(Producto producto) {
        if (producto == null) {
            return null;
        }
        return ProductoDTO.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precioUnitario(producto.getPrecioUnitario())
                .stock(producto.getStock())
                .imagenes(producto.getImagenes())
                .build();
    }
}
