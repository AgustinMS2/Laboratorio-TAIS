package uy.edu.utec.taller.ordenes.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.ordenes.client.ProductoClient;
import uy.edu.utec.taller.ordenes.client.dto.ProductoResponse;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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

    @Test
    @DisplayName("POST /api/ordenes debe retornar 201 Created con el id generado y la cabecera Location")
    void testCrearOrden() throws Exception {
        String body = """
                {
                  "email": "cliente@email.com",
                  "direccionEnvio": "Av. Italia 3333, Maldonado",
                  "telefono": "+59899111222",
                  "productos": [
                    { "productoId": 1, "cantidad": 2 },
                    { "productoId": 2, "cantidad": 3 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", matchesPattern("/api/ordenes/\\d+")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/ordenes debe retornar 409 Conflict cuando algún producto no existe")
    void testCrearOrdenProductoInexistente() throws Exception {
        String body = """
                {
                  "email": "cliente@email.com",
                  "direccionEnvio": "Av. Italia 3333, Maldonado",
                  "telefono": "+59899111222",
                  "productos": [ { "productoId": 99, "cantidad": 1 } ]
                }
                """;

        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", is("Uno o más productos solicitados no existen")))
                .andExpect(jsonPath("$.detalles[0]", is("No existe el producto con id 99")));
    }

    @Test
    @DisplayName("POST /api/ordenes debe retornar 409 Conflict cuando el stock es insuficiente")
    void testCrearOrdenStockInsuficiente() throws Exception {
        String body = """
                {
                  "email": "cliente@email.com",
                  "direccionEnvio": "Av. Italia 3333, Maldonado",
                  "telefono": "+59899111222",
                  "productos": [ { "productoId": 1, "cantidad": 999 } ]
                }
                """;

        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", is("Stock insuficiente para uno o más productos solicitados")))
                .andExpect(jsonPath("$.detalles[0]", is("Producto 1: stock disponible 13, cantidad solicitada 999")));
    }

    @Test
    @DisplayName("POST /api/ordenes debe retornar 400 Bad Request cuando el cuerpo es inválido")
    void testCrearOrdenInvalida() throws Exception {
        String body = """
                {
                  "email": "no-es-un-email",
                  "telefono": "+59899111222",
                  "productos": []
                }
                """;

        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.detalles", hasSize(greaterThanOrEqualTo(1))));
    }

    private String estadoJson(String estado) {
        return "{ \"estado\": \"" + estado + "\" }";
    }

    @Test
    @DisplayName("PATCH /api/ordenes/{id}/estado pasa una orden Created a 'Ready to Delivery'")
    void testActualizarEstadoReadyToDelivery() throws Exception {
        mockMvc.perform(patch("/api/ordenes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoJson("Ready to Delivery")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", is("Estado de la orden actualizado a Ready to Delivery")));

        mockMvc.perform(get("/api/ordenes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("Ready to Delivery")));
    }

    @Test
    @DisplayName("PATCH /api/ordenes/{id}/estado pasa una orden Created a 'No Stock'")
    void testActualizarEstadoNoStock() throws Exception {
        mockMvc.perform(patch("/api/ordenes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoJson("No Stock")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ordenes/1"))
                .andExpect(jsonPath("$.estado", is("No Stock")));
    }

    @Test
    @DisplayName("PATCH /api/ordenes/{id}/estado es idempotente si la orden ya tiene ese estado")
    void testActualizarEstadoIdempotente() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(patch("/api/ordenes/1/estado")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(estadoJson("Ready to Delivery")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PATCH /api/ordenes/{id}/estado retorna 409 si la orden ya no está en Created")
    void testActualizarEstadoOrdenYaProcesada() throws Exception {
        // La orden 2 está sembrada como Confirmed
        mockMvc.perform(patch("/api/ordenes/2/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoJson("Ready to Delivery")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(409)))
                .andExpect(jsonPath("$.mensaje", is("La orden 2 no puede pasar de 'Confirmed' a 'Ready to Delivery'")));
    }

    @Test
    @DisplayName("PATCH /api/ordenes/{id}/estado retorna 400 si el estado no es un resultado de procesamiento")
    void testActualizarEstadoNoPermitido() throws Exception {
        mockMvc.perform(patch("/api/ordenes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoJson("Shipped")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)));
    }

    @Test
    @DisplayName("PATCH /api/ordenes/{id}/estado retorna 400 si el estado es desconocido o falta")
    void testActualizarEstadoInvalido() throws Exception {
        mockMvc.perform(patch("/api/ordenes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoJson("Volando")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/ordenes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("PATCH /api/ordenes/{id}/estado retorna 404 si la orden no existe")
    void testActualizarEstadoOrdenInexistente() throws Exception {
        mockMvc.perform(patch("/api/ordenes/9999/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoJson("No Stock")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", is("No existe la orden con id 9999")));
    }

    @Test
    @DisplayName("POST con Content-Type distinto de application/json retorna 400 Bad Request con mensaje de error")
    void testContentTypeNoJson() throws Exception {
        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("El Content-Type de la solicitud debe ser application/json")));
    }

    @Test
    @DisplayName("GET con un id no numérico retorna 400 Bad Request con mensaje de error")
    void testIdNoNumerico() throws Exception {
        mockMvc.perform(get("/api/ordenes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("El parámetro 'id' tiene un valor inválido: 'abc'")));
    }

    @Test
    @DisplayName("GET /api/ordenes?estado=Created devuelve solo las órdenes en ese estado (lo usa el publicador)")
    void testListarOrdenesPorEstado() throws Exception {
        mockMvc.perform(get("/api/ordenes").param("estado", "Created"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].estado", is("Created")));

        mockMvc.perform(get("/api/ordenes").param("estado", "Confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)));

        mockMvc.perform(get("/api/ordenes").param("estado", "Ready to Delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/ordenes?estado=<desconocido> retorna 400 Bad Request con mensaje de error")
    void testListarOrdenesEstadoDesconocido() throws Exception {
        mockMvc.perform(get("/api/ordenes").param("estado", "Volando"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("Volando")));
    }
}
