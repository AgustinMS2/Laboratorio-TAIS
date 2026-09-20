package uy.edu.utec.taller.ordenes.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utec.taller.ordenes.dto.MensajeDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenCreadaDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenCreateDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDetalleDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenEstadoUpdateDTO;
import uy.edu.utec.taller.ordenes.exception.EstadoDesconocidoException;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;
import uy.edu.utec.taller.ordenes.service.OrdenService;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final OrdenService ordenService;

    @GetMapping
    public ResponseEntity<List<OrdenDTO>> listarOrdenes(@RequestParam(required = false) String estado) {
        if (estado == null) {
            return ResponseEntity.ok(ordenService.listarOrdenes());
        }
        EstadoOrden filtro = EstadoOrden.buscar(estado)
                .orElseThrow(() -> new EstadoDesconocidoException(estado));
        return ResponseEntity.ok(ordenService.listarOrdenesPorEstado(filtro));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenDTO> obtenerOrden(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtenerOrden(id));
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<OrdenDetalleDTO> obtenerDetalleOrden(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtenerDetalleOrden(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<MensajeDTO> actualizarEstado(@PathVariable Long id,
            @Valid @RequestBody OrdenEstadoUpdateDTO cambio) {
        ordenService.actualizarEstadoProcesamiento(id, cambio.getEstado());
        return ResponseEntity.ok(new MensajeDTO("Estado de la orden actualizado a " + cambio.getEstado().getValor()));
    }

    @PostMapping
    public ResponseEntity<OrdenCreadaDTO> crearOrden(@Valid @RequestBody OrdenCreateDTO ordenCreate) {
        OrdenCreadaDTO creada = ordenService.crearOrden(ordenCreate);
        URI location = URI.create("/api/ordenes/" + creada.getId());
        return ResponseEntity.created(location).body(creada);
    }
}
