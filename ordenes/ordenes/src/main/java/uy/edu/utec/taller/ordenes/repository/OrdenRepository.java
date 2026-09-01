package uy.edu.utec.taller.ordenes.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import uy.edu.utec.taller.ordenes.domain.Orden;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    @Override
    @EntityGraph(attributePaths = "productos")
    List<Orden> findAll();
}
