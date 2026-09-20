package uy.edu.utec.taller.procesamiento.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.utec.taller.procesamiento.model.Factura;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    @Override
    @EntityGraph(attributePaths = "items")
    List<Factura> findAll();

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Factura> findById(Long id);

    @EntityGraph(attributePaths = "items")
    List<Factura> findByOrdenId(Long ordenId);
}
