package uy.edu.utec.taller.procesamiento.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.procesamiento.model.FacturaItem;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaItemDTO {

    private Long productoId;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public static FacturaItemDTO fromEntity(FacturaItem item) {
        return FacturaItemDTO.builder()
                .productoId(item.getProductoId())
                .descripcion(item.getDescripcion())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .subtotal(item.getSubtotal())
                .build();
    }
}
