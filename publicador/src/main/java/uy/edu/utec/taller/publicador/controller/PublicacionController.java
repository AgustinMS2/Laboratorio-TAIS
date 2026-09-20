package uy.edu.utec.taller.publicador.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utec.taller.publicador.dto.PublicacionDTO;
import uy.edu.utec.taller.publicador.service.PublicadorService;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicadorService publicadorService;

    /** Órdenes publicadas en el broker por este servicio: última publicación y cantidad de veces. */
    @GetMapping
    public ResponseEntity<List<PublicacionDTO>> listarPublicaciones() {
        return ResponseEntity.ok(publicadorService.listarPublicaciones());
    }
}
