package uy.edu.utec.taller.ordenes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrdenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/ordenes debe retornar 200 OK y la lista de órdenes")
    void testListarOrdenes() throws Exception {
        mockMvc.perform(get("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email", is("cliente@email.com")))
                .andExpect(jsonPath("$[0].direccionEnvio", is("Av. Italia 3333, Maldonado")))
                .andExpect(jsonPath("$[0].telefono", is("+59899111222")))
                .andExpect(jsonPath("$[0].estado", is("Created")))
                .andExpect(jsonPath("$[0].productos", hasSize(2)))
                .andExpect(jsonPath("$[0].productos[0].productoId", is(1)))
                .andExpect(jsonPath("$[0].productos[0].cantidad", is(2)))
                .andExpect(jsonPath("$[1].estado", is("Confirmed")))
                .andExpect(jsonPath("$[1].productos", hasSize(1)));
    }
}
