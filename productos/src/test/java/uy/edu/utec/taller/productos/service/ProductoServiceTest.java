package uy.edu.utec.taller.productos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.utec.taller.productos.dto.ProductoDTO;
import uy.edu.utec.taller.productos.exception.ProductoNoEncontradoException;
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

    @Test
    @DisplayName("obtenerProducto devuelve el DTO cuando el producto existe")
    void testObtenerProductoExistente() {
        Producto producto = Producto.builder()
                .id(7L)
                .nombre("Teclado Mecánico")
                .descripcion("Switches azules")
                .precioUnitario(75.0)
                .stock(5)
                .imagenes(List.of("https://cdn.local/img/teclado.jpg"))
                .build();

        when(productoRepository.findById(7L)).thenReturn(Optional.of(producto));

        ProductoDTO dto = productoService.obtenerProducto(7L);

        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getNombre()).isEqualTo("Teclado Mecánico");
        assertThat(dto.getImagenes()).containsExactly("https://cdn.local/img/teclado.jpg");
    }

    @Test
    @DisplayName("obtenerProducto lanza ProductoNoEncontradoException cuando no existe")
    void testObtenerProductoInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerProducto(99L))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage("No existe el producto con id 99");
    }
}
