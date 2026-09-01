package uy.edu.utec.taller.ordenes.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import uy.edu.utec.taller.ordenes.domain.EstadoOrden;
import uy.edu.utec.taller.ordenes.domain.LineaOrden;
import uy.edu.utec.taller.ordenes.domain.Orden;
import uy.edu.utec.taller.ordenes.repository.OrdenRepository;

@SpringBootTest
@AutoConfigureMockMvc
class OrdenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdenRepository ordenRepository;

    @BeforeEach
    void setUp() {
        ordenRepository.deleteAll();

        Orden orden = new Orden();
        orden.setEmail("cliente@email.com");
        orden.setDireccionEnvio("Av. Italia 3333, Maldonado");
        orden.setTelefono("+59899111222");
        orden.setEstado(EstadoOrden.Created);
        orden.setFechaCreacion(OffsetDateTime.parse("2026-06-21T14:30:00-03:00"));
        orden.setProductos(List.of(new LineaOrden(1L, 2), new LineaOrden(2L, 5)));
        ordenRepository.save(orden);
    }

    @Test
    @WithMockUser
    void listarOrdenes_devuelve200ConListaDeOrdenes() throws Exception {
        mockMvc.perform(get("/api/ordenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("cliente@email.com"))
                .andExpect(jsonPath("$[0].estado").value("Created"))
                .andExpect(jsonPath("$[0].productos", hasSize(2)))
                .andExpect(jsonPath("$[0].productos[0].productoId").value(1))
                .andExpect(jsonPath("$[0].productos[0].cantidad").value(2));
    }
}
