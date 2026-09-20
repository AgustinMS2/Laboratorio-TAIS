package uy.edu.utec.taller.publicador.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uy.edu.utec.taller.publicador.client.OrdenClient;
import uy.edu.utec.taller.publicador.client.dto.OrdenResponse;
import uy.edu.utec.taller.publicador.messaging.MqttOrdenPublisher;
import uy.edu.utec.taller.publicador.service.PublicadorService;

@SpringBootTest
@AutoConfigureMockMvc
class PublicacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PublicadorService publicadorService;

    @MockitoBean
    private OrdenClient ordenClient;

    @MockitoBean
    private MqttOrdenPublisher publisher;

    @Test
    @DisplayName("GET /api/publicaciones debe retornar 200 OK y las órdenes que el servicio publicó")
    void testListarPublicaciones() throws Exception {
        when(ordenClient.listarPorEstado("Created")).thenReturn(List.of(
                OrdenResponse.builder().id(5L).estado("Created").fechaCreacion("2026-06-21T14:30:00-03:00").build(),
                OrdenResponse.builder().id(3L).estado("Created").fechaCreacion("2026-06-21T14:31:00-03:00").build()));
        publicadorService.publicarPendientes();

        mockMvc.perform(get("/api/publicaciones").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ordenId", is(3)))
                .andExpect(jsonPath("$[0].veces", is(1)))
                .andExpect(jsonPath("$[1].ordenId", is(5)));
    }
}
