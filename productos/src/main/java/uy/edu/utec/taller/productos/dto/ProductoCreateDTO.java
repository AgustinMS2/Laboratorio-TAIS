package uy.edu.utec.taller.productos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
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
public class ProductoCreateDTO {

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 100, message = "El campo nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "El campo descripcion no puede superar los 500 caracteres")
    private String descripcion;

    @NotNull(message = "El campo precioUnitario es obligatorio")
    @Positive(message = "El campo precioUnitario debe ser mayor que 0")
    private Double precioUnitario;

    @NotNull(message = "El campo stock es obligatorio")
    @PositiveOrZero(message = "El campo stock no puede ser negativo")
    private Integer stock;

    private List<@Size(max = 500, message = "Cada link de imagen no puede superar los 500 caracteres") String> imagenes;

    public Producto toEntity() {
        return Producto.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .precioUnitario(precioUnitario)
                .stock(stock)
                .imagenes(imagenes == null ? new ArrayList<>() : new ArrayList<>(imagenes))
                .build();
    }
}
