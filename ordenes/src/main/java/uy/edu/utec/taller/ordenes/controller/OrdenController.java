package uy.edu.utec.taller.ordenes.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utec.taller.ordenes.dto.OrdenCreadaDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenCreateDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDTO;
import uy.edu.utec.taller.ordenes.dto.OrdenDetalleDTO;
import uy.edu.utec.taller.ordenes.service.OrdenService;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final OrdenService ordenService;

    @GetMapping
    public ResponseEntity<List<OrdenDTO>> listarOrdenes() {
        List<OrdenDTO> ordenes = ordenService.listarOrdenes();
        return ResponseEntity.ok(ordenes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenDTO> obtenerOrden(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtenerOrden(id));
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<OrdenDetalleDTO> obtenerDetalleOrden(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtenerDetalleOrden(id));
    }

    @PostMapping
    public ResponseEntity<OrdenCreadaDTO> crearOrden(@Valid @RequestBody OrdenCreateDTO ordenCreate) {
        OrdenCreadaDTO creada = ordenService.crearOrden(ordenCreate);
        URI location = URI.create("/api/ordenes/" + creada.getId());
        return ResponseEntity.created(location).body(creada);
    }
}
