package uy.edu.utec.taller.ordenes.config;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;
import uy.edu.utec.taller.ordenes.model.LineaOrden;
import uy.edu.utec.taller.ordenes.model.Orden;
import uy.edu.utec.taller.ordenes.repository.OrdenRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final OrdenRepository ordenRepository;

    @Override
    public void run(String... args) {
        if (ordenRepository.count() == 0) {
            Orden orden1 = Orden.builder()
                    .email("cliente@email.com")
                    .direccionEnvio("Av. Italia 3333, Maldonado")
                    .telefono("+59899111222")
                    .estado(EstadoOrden.Created)
                    .fechaCreacion(OffsetDateTime.parse("2026-06-21T14:30:00-03:00"))
                    .productos(List.of(
                            LineaOrden.builder().productoId(1L).cantidad(2).build(),
                            LineaOrden.builder().productoId(2L).cantidad(5).build()
                    ))
                    .build();

            Orden orden2 = Orden.builder()
                    .email("otra.clienta@email.com")
                    .direccionEnvio("Bvar. Artigas 1234, Montevideo")
                    .telefono("+59891234567")
                    .estado(EstadoOrden.Confirmed)
                    .fechaCreacion(OffsetDateTime.parse("2026-07-02T09:15:00-03:00"))
                    .productos(List.of(
                            LineaOrden.builder().productoId(3L).cantidad(1).build()
                    ))
                    .build();

            ordenRepository.saveAll(List.of(orden1, orden2));
        }
    }
}
