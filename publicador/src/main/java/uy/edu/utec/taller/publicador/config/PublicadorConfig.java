package uy.edu.utec.taller.publicador.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicadorConfig {

    /** Reloj inyectable: los tests lo reemplazan para simular el paso del tiempo (reintentos). */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
