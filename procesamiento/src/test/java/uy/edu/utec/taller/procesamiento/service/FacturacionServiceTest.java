package uy.edu.utec.taller.procesamiento.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.utec.taller.procesamiento.client.dto.LineaOrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.OrdenResponse;
import uy.edu.utec.taller.procesamiento.client.dto.ProductoResponse;
import uy.edu.utec.taller.procesamiento.dto.FacturaDTO;
import uy.edu.utec.taller.procesamiento.model.Procesamiento;
import uy.edu.utec.taller.procesamiento.model.ResultadoProcesamiento;

@SpringBootTest
@Transactional
class FacturacionServiceTest {

    @Autowired
    private FacturacionService facturacionService;

    private static OrdenResponse orden(long id) {
        return OrdenResponse.builder()
                .id(id)
                .email("cliente@email.com")
                .direccionEnvio("Av. Italia 3333, Maldonado")
                .telefono("+59899111222")
                .estado("Created")
                .productos(List.of(new LineaOrdenResponse(1L, 2), new LineaOrdenResponse(2L, 5)))
                .build();
    }

    @Test
    @DisplayName("la factura tiene un ítem por producto con su precio unitario y el total es la suma de los subtotales")
    void testFacturaConItemsYTotal() {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        cantidades.put(1L, 2);
        cantidades.put(2L, 5);
        Map<Long, ProductoResponse> productos = Map.of(
                1L, ProductoResponse.builder().id(1L).nombre("Notebook Lenovo ThinkPad").precioUnitario(1250.50).stock(15).build(),
                2L, ProductoResponse.builder().id(2L).nombre("Mouse Logitech MX Master 3").precioUnitario(99.90).stock(40).build());

        Procesamiento procesamiento = facturacionService.registrarListaParaEntrega(orden(1L), cantidades, productos);

        assertThat(procesamiento.getResultado()).isEqualTo(ResultadoProcesamiento.ReadyToDelivery);
        assertThat(procesamiento.isOrdenActualizada()).isFalse();

        List<FacturaDTO> facturas = facturacionService.listarFacturas(1L);
        assertThat(facturas).hasSize(1);
        FacturaDTO factura = facturas.getFirst();
        assertThat(factura.getEmail()).isEqualTo("cliente@email.com");
        assertThat(factura.getItems()).hasSize(2);
        assertThat(factura.getItems().get(0).getDescripcion()).isEqualTo("Notebook Lenovo ThinkPad");
        assertThat(factura.getItems().get(0).getPrecioUnitario()).isEqualByComparingTo("1250.50");
        assertThat(factura.getItems().get(0).getCantidad()).isEqualTo(2);
        assertThat(factura.getItems().get(0).getSubtotal()).isEqualByComparingTo("2501.00");
        assertThat(factura.getItems().get(1).getSubtotal()).isEqualByComparingTo("499.50");
        assertThat(factura.getTotal()).isEqualByComparingTo(new BigDecimal("3000.50"));
    }

    @Test
    @DisplayName("registrarSinStock guarda el resultado No Stock y marcarOrdenActualizada lo confirma")
    void testSinStockYMarcaActualizada() {
        Procesamiento procesamiento = facturacionService.registrarSinStock(7L, "Producto 2: no existe");

        assertThat(facturacionService.buscarProcesamiento(7L)).isPresent();
        assertThat(procesamiento.getResultado()).isEqualTo(ResultadoProcesamiento.NoStock);
        assertThat(procesamiento.isOrdenActualizada()).isFalse();

        facturacionService.marcarOrdenActualizada(procesamiento.getId());

        assertThat(facturacionService.buscarProcesamiento(7L).orElseThrow().isOrdenActualizada()).isTrue();
        assertThat(facturacionService.listarFacturas(7L)).isEmpty();
    }
}
