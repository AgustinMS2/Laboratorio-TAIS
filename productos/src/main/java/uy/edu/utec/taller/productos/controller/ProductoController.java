package uy.edu.utec.taller.productos.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utec.taller.productos.dto.ProductoCreadoDTO;
import uy.edu.utec.taller.productos.dto.ProductoCreateDTO;
import uy.edu.utec.taller.productos.dto.ProductoDTO;
import uy.edu.utec.taller.productos.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listarProductos() {
        List<ProductoDTO> productos = productoService.listarProductos();
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtenerProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerProducto(id));
    }

    @PostMapping
    public ResponseEntity<ProductoCreadoDTO> crearProducto(@Valid @RequestBody ProductoCreateDTO productoCreate) {
        ProductoCreadoDTO creado = productoService.crearProducto(productoCreate);
        URI location = URI.create("/api/productos/" + creado.getId());
        return ResponseEntity.created(location).body(creado);
    }
}
