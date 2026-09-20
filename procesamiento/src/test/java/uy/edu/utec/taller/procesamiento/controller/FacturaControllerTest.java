package uy.edu.utec.taller.procesamiento.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.procesamiento.client.dto.LineaOrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.OrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.ProductoResponse;
import uy.edu.utec.taller.procesamiento.service.FacturacionService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FacturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FacturacionService facturacionService;

    @BeforeEach
    void datos() {
        OrdenResponse orden = OrdenResponse.builder()
                .id(1L)
                .email("cliente@email.com")
                .direccionEnvio("Av. Italia 3333, Maldonado")
                .telefono("+59899111222")
                .estado("Created")
                .productos(List.of(new LineaOrdenResponse(1L, 2), new LineaOrdenResponse(2L, 5)))
                .build();
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        cantidades.put(1L, 2);
        cantidades.put(2L, 5);
        facturacionService.registrarListaParaEntrega(orden, cantidades, Map.of(
                1L, ProductoResponse.builder().id(1L).nombre("Notebook Lenovo ThinkPad").precioUnitario(1250.50).stock(15).build(),
                2L, ProductoResponse.builder().id(2L).nombre("Mouse Logitech MX Master 3").precioUnitario(99.90).stock(40).build()));
        facturacionService.registrarSinStock(2L, "Producto 1: stock disponible 0, cantidad solicitada 1");
    }

    @Test
    @DisplayName("GET /api/facturas debe retornar 200 OK y la lista de facturas con sus ítems y total")
    void testListarFacturas() throws Exception {
        mockMvc.perform(get("/api/facturas").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ordenId", is(1)))
                .andExpect(jsonPath("$[0].email", is("cliente@email.com")))
                .andExpect(jsonPath("$[0].items", hasSize(2)))
                .andExpect(jsonPath("$[0].items[0].productoId", is(1)))
                .andExpect(jsonPath("$[0].items[0].descripcion", is("Notebook Lenovo ThinkPad")))
                .andExpect(jsonPath("$[0].items[0].precioUnitario", is(1250.50)))
                .andExpect(jsonPath("$[0].items[0].cantidad", is(2)))
                .andExpect(jsonPath("$[0].items[0].subtotal", is(2501.00)))
                .andExpect(jsonPath("$[0].total", is(3000.50)));
    }

    @Test
    @DisplayName("GET /api/facturas?ordenId= filtra por orden")
    void testListarFacturasPorOrden() throws Exception {
        mockMvc.perform(get("/api/facturas").param("ordenId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/facturas").param("ordenId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/facturas/{id} debe retornar 200 OK cuando existe y 404 cuando no existe")
    void testObtenerFactura() throws Exception {
        Long id = facturacionService.listarFacturas(1L).getFirst().getId();

        mockMvc.perform(get("/api/facturas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.total", is(3000.50)));

        mockMvc.perform(get("/api/facturas/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(404)))
                .andExpect(jsonPath("$.mensaje", is("No existe la factura con id 9999")));
    }

    @Test
    @DisplayName("GET /api/procesamientos lista las órdenes procesadas con su resultado")
    void testListarProcesamientos() throws Exception {
        mockMvc.perform(get("/api/procesamientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ordenId", is(1)))
                .andExpect(jsonPath("$[0].resultado", is("Ready to Delivery")))
                .andExpect(jsonPath("$[1].ordenId", is(2)))
                .andExpect(jsonPath("$[1].resultado", is("No Stock")))
                .andExpect(jsonPath("$[1].detalle", is("Producto 1: stock disponible 0, cantidad solicitada 1")));
    }

    @Test
    @DisplayName("GET /api/facturas/{id} con un id no numérico retorna 400 Bad Request con mensaje de error")
    void testIdNoNumerico() throws Exception {
        mockMvc.perform(get("/api/facturas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(400)))
                .andExpect(jsonPath("$.mensaje", is("El parámetro 'id' tiene un valor inválido: 'abc'")));
    }
}
