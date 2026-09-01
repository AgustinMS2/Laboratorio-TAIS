package uy.edu.utec.taller.ordenes.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.utec.taller.ordenes.model.Orden;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    @Override
    @EntityGraph(attributePaths = "productos")
    List<Orden> findAll();
}
