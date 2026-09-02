package uy.edu.utec.taller.ordenes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uy.edu.utec.taller.ordenes.client.ProductoClient;
import uy.edu.utec.taller.ordenes.client.dto.ProductoResponse;

@SpringBootTest
@AutoConfigureMockMvc
class OrdenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoClient productoClient;

    @BeforeEach
    void stubProductoClient() {
        when(productoClient.obtenerProducto(anyLong())).thenReturn(Optional.empty());
        when(productoClient.obtenerProducto(1L)).thenReturn(Optional.of(ProductoResponse.builder()
                .id(1L).nombre("Notebook Lenovo ThinkPad").descripcion("Notebook Intel i7 16GB RAM")
                .precioUnitario(1250.50).stock(13).build()));
        when(productoClient.obtenerProducto(2L)).thenReturn(Optional.of(ProductoResponse.builder()
                .id(2L).nombre("Mouse Logitech MX Master 3").descripcion("Mouse inalámbrico ergonómico")
                .precioUnitario(99.90).stock(35).build()));
    }

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

    @Test
    @DisplayName("GET /api/ordenes/{id} debe retornar 200 OK y la orden cuando existe")
    void testObtenerOrdenExistente() throws Exception {
        mockMvc.perform(get("/api/ordenes/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("cliente@email.com")))
                .andExpect(jsonPath("$.direccionEnvio", is("Av. Italia 3333, Maldonado")))
                .andExpect(jsonPath("$.telefono", is("+59899111222")))
                .andExpect(jsonPath("$.estado", is("Created")))
                .andExpect(jsonPath("$.productos", hasSize(2)))
                .andExpect(jsonPath("$.productos[0].productoId", is(1)))
                .andExpect(jsonPath("$.productos[0].cantidad", is(2)));
    }

    @Test
    @DisplayName("GET /api/ordenes/{id} debe retornar 404 Not Found y mensaje de error cuando no existe")
    void testObtenerOrdenInexistente() throws Exception {
        mockMvc.perform(get("/api/ordenes/9999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("No existe la orden con id 9999")));
    }

    @Test
    @DisplayName("GET /api/ordenes/{id}/detalle debe retornar 200 OK con el detalle completo de cada producto")
    void testObtenerDetalleOrdenExistente() throws Exception {
        mockMvc.perform(get("/api/ordenes/1/detalle")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("cliente@email.com")))
                .andExpect(jsonPath("$.total", is(3000.50)))
                .andExpect(jsonPath("$.productos", hasSize(2)))
                .andExpect(jsonPath("$.productos[0].cantidad", is(2)))
                .andExpect(jsonPath("$.productos[0].subtotal", is(2501.00)))
                .andExpect(jsonPath("$.productos[0].producto.nombre", is("Notebook Lenovo ThinkPad")))
                .andExpect(jsonPath("$.productos[0].producto.stock", is(13)))
                .andExpect(jsonPath("$.productos[1].subtotal", is(499.50)))
                .andExpect(jsonPath("$.productos[1].producto.nombre", is("Mouse Logitech MX Master 3")));
    }

    @Test
    @DisplayName("GET /api/ordenes/{id}/detalle debe retornar 404 Not Found cuando la orden no existe")
    void testObtenerDetalleOrdenInexistente() throws Exception {
        mockMvc.perform(get("/api/ordenes/9999/detalle")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("No existe la orden con id 9999")));
    }
}
