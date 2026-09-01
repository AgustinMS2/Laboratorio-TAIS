package uy.edu.utec.taller.ordenes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uy.edu.utec.taller.ordenes.repository.OrdenRepository;
import uy.edu.utec.taller.ordenes.web.dto.OrdenResponse;

@Service
public class OrdenService {

    private final OrdenRepository ordenRepository;

    public OrdenService(OrdenRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar() {
        return ordenRepository.findAll().stream().map(OrdenResponse::from).toList();
    }
}
