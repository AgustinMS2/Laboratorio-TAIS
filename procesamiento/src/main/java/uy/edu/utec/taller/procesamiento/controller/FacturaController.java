package uy.edu.utec.taller.procesamiento.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utec.taller.procesamiento.dto.FacturaDTO;
import uy.edu.utec.taller.procesamiento.service.FacturacionService;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturacionService facturacionService;

    /** Lista las facturas; con {@code ?ordenId=} devuelve solo las de esa orden. */
    @GetMapping
    public ResponseEntity<List<FacturaDTO>> listarFacturas(@RequestParam(required = false) Long ordenId) {
        return ResponseEntity.ok(facturacionService.listarFacturas(ordenId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaDTO> obtenerFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturacionService.obtenerFactura(id));
    }
}
