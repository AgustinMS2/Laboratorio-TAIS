package uy.edu.utec.taller.productos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.utec.taller.productos.dto.ProductoDTO;
import uy.edu.utec.taller.productos.model.Producto;
import uy.edu.utec.taller.productos.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    @Test
    @DisplayName("listarProductos debe retornar lista mapeada de DTOs")
    void testListarProductos() {
        Producto producto = Producto.builder()
                .id(1L)
                .nombre("Test Product")
                .descripcion("Description")
                .precioUnitario(100.0)
                .stock(10)
                .imagenes(List.of("https://cdn.local/img/test.jpg"))
                .build();

        when(productoRepository.findAll()).thenReturn(List.of(producto));

        List<ProductoDTO> resultado = productoService.listarProductos();

        assertThat(resultado).hasSize(1);
        ProductoDTO dto = resultado.getFirst();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNombre()).isEqualTo("Test Product");
        assertThat(dto.getDescripcion()).isEqualTo("Description");
        assertThat(dto.getPrecioUnitario()).isEqualTo(100.0);
        assertThat(dto.getStock()).isEqualTo(10);
        assertThat(dto.getImagenes()).containsExactly("https://cdn.local/img/test.jpg");
    }

    @Test
    @DisplayName("listarProductos retorna lista vacía si no hay productos")
    void testListarProductosVacio() {
        when(productoRepository.findAll()).thenReturn(List.of());

        List<ProductoDTO> resultado = productoService.listarProductos();

        assertThat(resultado).isEmpty();
    }
}
