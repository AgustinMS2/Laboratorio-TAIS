package uy.edu.utec.taller.productos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.utec.taller.productos.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
