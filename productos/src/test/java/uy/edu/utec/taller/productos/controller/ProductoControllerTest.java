package uy.edu.utec.taller.productos.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/productos debe retornar 200 OK y la lista de productos")
    void testListarProductos() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre", is("Notebook Lenovo ThinkPad")))
                .andExpect(jsonPath("$[0].descripcion", is("Notebook Intel i7 16GB RAM")))
                .andExpect(jsonPath("$[0].precioUnitario", is(1250.50)))
                .andExpect(jsonPath("$[0].stock", is(15)))
                .andExpect(jsonPath("$[0].imagenes", hasSize(2)))
                .andExpect(jsonPath("$[0].imagenes[0]", is("https://cdn.local/img/thinkpad-1.jpg")))
                .andExpect(jsonPath("$[1].nombre", is("Mouse Logitech MX Master 3")))
                .andExpect(jsonPath("$[1].precioUnitario", is(99.90)))
                .andExpect(jsonPath("$[1].stock", is(40)))
                .andExpect(jsonPath("$[1].imagenes", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/productos/{id} debe retornar 200 OK y el producto cuando existe")
    void testObtenerProductoExistente() throws Exception {
        mockMvc.perform(get("/api/productos/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Notebook Lenovo ThinkPad")))
                .andExpect(jsonPath("$.descripcion", is("Notebook Intel i7 16GB RAM")))
                .andExpect(jsonPath("$.precioUnitario", is(1250.50)))
                .andExpect(jsonPath("$.stock", is(15)))
                .andExpect(jsonPath("$.imagenes", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/productos/{id} debe retornar 404 Not Found y mensaje de error cuando no existe")
    void testObtenerProductoInexistente() throws Exception {
        mockMvc.perform(get("/api/productos/9999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("No existe el producto con id 9999")));
    }
}
