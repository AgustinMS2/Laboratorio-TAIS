package uy.edu.utec.taller.ordenes.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;
import uy.edu.utec.taller.ordenes.model.Orden;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDTO {

    private Long id;
    private String email;
    private String direccionEnvio;
    private String telefono;
    private EstadoOrden estado;
    private OffsetDateTime fechaCreacion;
    private List<LineaOrdenDTO> productos;

    public static OrdenDTO fromEntity(Orden orden) {
        if (orden == null) {
            return null;
        }
        return OrdenDTO.builder()
                .id(orden.getId())
                .email(orden.getEmail())
                .direccionEnvio(orden.getDireccionEnvio())
                .telefono(orden.getTelefono())
                .estado(orden.getEstado())
                .fechaCreacion(orden.getFechaCreacion())
                .productos(orden.getProductos().stream()
                        .map(LineaOrdenDTO::fromEntity)
                        .toList())
                .build();
    }
}
