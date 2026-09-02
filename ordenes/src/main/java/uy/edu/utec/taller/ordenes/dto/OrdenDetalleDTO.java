package uy.edu.utec.taller.ordenes.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDetalleDTO {

    private Long id;
    private String email;
    private String direccionEnvio;
    private String telefono;
    private EstadoOrden estado;
    private OffsetDateTime fechaCreacion;
    private Double total;
    private List<LineaOrdenDetalleDTO> productos;
}
