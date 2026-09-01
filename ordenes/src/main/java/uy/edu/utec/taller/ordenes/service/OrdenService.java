package uy.edu.utec.taller.ordenes.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.ordenes.dto.OrdenDTO;
import uy.edu.utec.taller.ordenes.repository.OrdenRepository;

@Service
@RequiredArgsConstructor
public class OrdenService {

    private final OrdenRepository ordenRepository;

    @Transactional(readOnly = true)
    public List<OrdenDTO> listarOrdenes() {
        return ordenRepository.findAll()
                .stream()
                .map(OrdenDTO::fromEntity)
                .toList();
    }
}
