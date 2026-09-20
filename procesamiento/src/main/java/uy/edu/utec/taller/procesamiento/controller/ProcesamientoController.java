package uy.edu.utec.taller.procesamiento.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utec.taller.procesamiento.dto.ProcesamientoDTO;
import uy.edu.utec.taller.procesamiento.service.FacturacionService;

@RestController
@RequestMapping("/api/procesamientos")
@RequiredArgsConstructor
public class ProcesamientoController {

    private final FacturacionService facturacionService;

    /** Órdenes ya procesadas y su resultado ({@code Ready to Delivery} o {@code No Stock}). */
    @GetMapping
    public ResponseEntity<List<ProcesamientoDTO>> listarProcesamientos() {
        return ResponseEntity.ok(facturacionService.listarProcesamientos());
    }
}
