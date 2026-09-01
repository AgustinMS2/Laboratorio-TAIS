package uy.edu.utec.taller.ordenes.config;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uy.edu.utec.taller.ordenes.domain.EstadoOrden;
import uy.edu.utec.taller.ordenes.domain.LineaOrden;
import uy.edu.utec.taller.ordenes.domain.Orden;
import uy.edu.utec.taller.ordenes.repository.OrdenRepository;

@Configuration
@Profile("!test")
public class DataInitializer {

    @Bean
    CommandLineRunner seedOrdenes(OrdenRepository ordenRepository) {
        return args -> {
            if (ordenRepository.count() > 0) {
                return;
            }

            Orden orden1 = new Orden();
            orden1.setEmail("cliente@email.com");
            orden1.setDireccionEnvio("Av. Italia 3333, Maldonado");
            orden1.setTelefono("+59899111222");
            orden1.setEstado(EstadoOrden.Created);
            orden1.setFechaCreacion(OffsetDateTime.parse("2026-06-21T14:30:00-03:00"));
            orden1.setProductos(List.of(new LineaOrden(1L, 2), new LineaOrden(2L, 5)));

            Orden orden2 = new Orden();
            orden2.setEmail("otra.clienta@email.com");
            orden2.setDireccionEnvio("Bvar. Artigas 1234, Montevideo");
            orden2.setTelefono("+59891234567");
            orden2.setEstado(EstadoOrden.Confirmed);
            orden2.setFechaCreacion(OffsetDateTime.parse("2026-07-02T09:15:00-03:00"));
            orden2.setProductos(List.of(new LineaOrden(3L, 1)));

            ordenRepository.saveAll(List.of(orden1, orden2));
        };
    }
}
