package uy.edu.utec.taller.productos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.utec.taller.productos.dto.ProductoCreadoDTO;
import uy.edu.utec.taller.productos.dto.ProductoCreateDTO;
import uy.edu.utec.taller.productos.dto.ProductoDTO;
import uy.edu.utec.taller.productos.dto.ProductoPatchDTO;
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

    @Test
    @DisplayName("crearProducto guarda la entidad y devuelve el id generado")
    void testCrearProducto() {
        ProductoCreateDTO nuevo = ProductoCreateDTO.builder()
                .nombre("Monitor Dell 27")
                .descripcion("4K IPS")
                .precioUnitario(340.0)
                .stock(8)
                .imagenes(List.of("https://cdn.local/img/monitor.jpg"))
                .build();

        Producto persistido = Producto.builder()
                .id(50L)
                .nombre("Monitor Dell 27")
                .descripcion("4K IPS")
                .precioUnitario(340.0)
                .stock(8)
                .imagenes(List.of("https://cdn.local/img/monitor.jpg"))
                .build();

        when(productoRepository.save(any(Producto.class))).thenReturn(persistido);

        ProductoCreadoDTO creado = productoService.crearProducto(nuevo);

        assertThat(creado.getId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("reemplazarProducto sobrescribe todos los campos cuando el producto existe")
    void testReemplazarProductoExistente() {
        Producto existente = Producto.builder()
                .id(3L)
                .nombre("Nombre viejo")
                .descripcion("Descripción vieja")
                .precioUnitario(10.0)
                .stock(1)
                .imagenes(new ArrayList<>(List.of("https://cdn.local/img/vieja.jpg")))
                .build();

        when(productoRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoCreateDTO datos = ProductoCreateDTO.builder()
                .nombre("Nombre nuevo")
                .descripcion("Descripción nueva")
                .precioUnitario(20.0)
                .stock(5)
                .imagenes(List.of("https://cdn.local/img/nueva-1.jpg", "https://cdn.local/img/nueva-2.jpg"))
                .build();

        productoService.reemplazarProducto(3L, datos);

        assertThat(existente.getNombre()).isEqualTo("Nombre nuevo");
        assertThat(existente.getDescripcion()).isEqualTo("Descripción nueva");
        assertThat(existente.getPrecioUnitario()).isEqualTo(20.0);
        assertThat(existente.getStock()).isEqualTo(5);
        assertThat(existente.getImagenes())
                .containsExactly("https://cdn.local/img/nueva-1.jpg", "https://cdn.local/img/nueva-2.jpg");
    }

    @Test
    @DisplayName("reemplazarProducto lanza ProductoNoEncontradoException cuando no existe")
    void testReemplazarProductoInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        ProductoCreateDTO datos = ProductoCreateDTO.builder()
                .nombre("X")
                .precioUnitario(1.0)
                .stock(0)
                .build();

        assertThatThrownBy(() -> productoService.reemplazarProducto(99L, datos))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage("No existe el producto con id 99");
    }

    @Test
    @DisplayName("actualizarParcialProducto modifica sólo los campos presentes")
    void testActualizarParcialProducto() {
        Producto existente = Producto.builder()
                .id(4L)
                .nombre("Nombre original")
                .descripcion("Descripción original")
                .precioUnitario(100.0)
                .stock(10)
                .imagenes(new ArrayList<>(List.of("https://cdn.local/img/original.jpg")))
                .build();

        when(productoRepository.findById(4L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoPatchDTO cambios = ProductoPatchDTO.builder()
                .precioUnitario(129.99)
                .stock(20)
                .build();

        productoService.actualizarParcialProducto(4L, cambios);

        assertThat(existente.getNombre()).isEqualTo("Nombre original");
        assertThat(existente.getDescripcion()).isEqualTo("Descripción original");
        assertThat(existente.getPrecioUnitario()).isEqualTo(129.99);
        assertThat(existente.getStock()).isEqualTo(20);
        assertThat(existente.getImagenes()).containsExactly("https://cdn.local/img/original.jpg");
    }

    @Test
    @DisplayName("actualizarParcialProducto lanza ProductoNoEncontradoException cuando no existe")
    void testActualizarParcialProductoInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        ProductoPatchDTO cambios = ProductoPatchDTO.builder().stock(5).build();

        assertThatThrownBy(() -> productoService.actualizarParcialProducto(99L, cambios))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage("No existe el producto con id 99");
    }
}
