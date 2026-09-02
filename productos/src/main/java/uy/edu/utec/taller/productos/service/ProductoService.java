package uy.edu.utec.taller.productos.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.productos.dto.ProductoCreadoDTO;
import uy.edu.utec.taller.productos.dto.ProductoCreateDTO;
import uy.edu.utec.taller.productos.dto.ProductoDTO;
import uy.edu.utec.taller.productos.exception.ProductoNoEncontradoException;
import uy.edu.utec.taller.productos.model.Producto;
import uy.edu.utec.taller.productos.repository.ProductoRepository;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarProductos() {
        return productoRepository.findAll()
                .stream()
                .map(ProductoDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerProducto(Long id) {
        return productoRepository.findById(id)
                .map(ProductoDTO::fromEntity)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    @Transactional
    public ProductoCreadoDTO crearProducto(ProductoCreateDTO productoCreate) {
        Producto guardado = productoRepository.save(productoCreate.toEntity());
        return ProductoCreadoDTO.builder()
                .id(guardado.getId())
                .build();
    }
}
