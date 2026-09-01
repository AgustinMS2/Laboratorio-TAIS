package uy.edu.utec.taller.productos.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uy.edu.utec.taller.productos.model.Producto;
import uy.edu.utec.taller.productos.repository.ProductoRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductoRepository productoRepository;

    @Override
    public void run(String... args) {
        if (productoRepository.count() == 0) {
            Producto p1 = Producto.builder()
                    .nombre("Notebook Lenovo ThinkPad")
                    .descripcion("Notebook Intel i7 16GB RAM")
                    .precioUnitario(1250.50)
                    .stock(15)
                    .imagenes(List.of(
                            "https://cdn.local/img/thinkpad-1.jpg",
                            "https://cdn.local/img/thinkpad-2.jpg"
                    ))
                    .build();

            Producto p2 = Producto.builder()
                    .nombre("Mouse Logitech MX Master 3")
                    .descripcion("Mouse inalámbrico ergonómico")
                    .precioUnitario(99.90)
                    .stock(40)
                    .imagenes(List.of(
                            "https://cdn.local/img/mx-master-1.jpg"
                    ))
                    .build();

            productoRepository.saveAll(List.of(p1, p2));
        }
    }
}
