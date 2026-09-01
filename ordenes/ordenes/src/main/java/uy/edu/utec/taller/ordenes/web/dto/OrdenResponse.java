package uy.edu.utec.taller.ordenes.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

import uy.edu.utec.taller.ordenes.domain.EstadoOrden;
import uy.edu.utec.taller.ordenes.domain.Orden;

public record OrdenResponse(
        Long id,
        String email,
        String direccionEnvio,
        String telefono,
        EstadoOrden estado,
        OffsetDateTime fechaCreacion,
        List<LineaOrdenResponse> productos) {

    public static OrdenResponse from(Orden orden) {
        List<LineaOrdenResponse> lineas = orden.getProductos().stream()
                .map(l -> new LineaOrdenResponse(l.getProductoId(), l.getCantidad()))
                .toList();
        return new OrdenResponse(
                orden.getId(),
                orden.getEmail(),
                orden.getDireccionEnvio(),
                orden.getTelefono(),
                orden.getEstado(),
                orden.getFechaCreacion(),
                lineas);
    }
}
