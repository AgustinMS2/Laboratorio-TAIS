package uy.edu.utec.taller.productos.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.productos.validation.AlMenosUnCampoPresente;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AlMenosUnCampoPresente
public class ProductoPatchDTO {

    @Size(min = 1, max = 100, message = "El campo nombre debe tener entre 1 y 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "El campo descripcion no puede superar los 500 caracteres")
    private String descripcion;

    @Positive(message = "El campo precioUnitario debe ser mayor que 0")
    private Double precioUnitario;

    @PositiveOrZero(message = "El campo stock no puede ser negativo")
    private Integer stock;

    private List<@Size(max = 500, message = "Cada link de imagen no puede superar los 500 caracteres") String> imagenes;

    public boolean sinCambios() {
        return nombre == null
                && descripcion == null
                && precioUnitario == null
                && stock == null
                && imagenes == null;
    }
}
