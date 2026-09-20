package uy.edu.utec.taller.procesamiento.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.utec.taller.procesamiento.model.Procesamiento;

@Repository
public interface ProcesamientoRepository extends JpaRepository<Procesamiento, Long> {

    Optional<Procesamiento> findByOrdenId(Long ordenId);
}
