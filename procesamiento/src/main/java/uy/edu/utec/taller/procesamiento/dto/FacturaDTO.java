package uy.edu.utec.taller.procesamiento.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.procesamiento.model.Factura;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDTO {

    private Long id;
    private Long ordenId;
    private OffsetDateTime fecha;
    private String email;
    private String direccionEnvio;
    private String telefono;
    private List<FacturaItemDTO> items;
    private BigDecimal total;

    public static FacturaDTO fromEntity(Factura factura) {
        return FacturaDTO.builder()
                .id(factura.getId())
                .ordenId(factura.getOrdenId())
                .fecha(factura.getFecha())
                .email(factura.getEmail())
                .direccionEnvio(factura.getDireccionEnvio())
                .telefono(factura.getTelefono())
                .items(factura.getItems().stream().map(FacturaItemDTO::fromEntity).toList())
                .total(factura.getTotal())
                .build();
    }
}
