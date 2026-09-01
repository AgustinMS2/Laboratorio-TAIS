package uy.edu.utec.taller.ordenes.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uy.edu.utec.taller.ordenes.service.OrdenService;
import uy.edu.utec.taller.ordenes.web.dto.OrdenResponse;

@RestController
@RequestMapping(path = "/api/ordenes", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping
    public List<OrdenResponse> listarOrdenes() {
        return ordenService.listar();
    }
}
