package uy.edu.utec.taller.procesamiento.client;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.utec.taller.procesamiento.client.dto.ProductoResponse;
import uy.edu.utec.taller.procesamiento.exception.ServicioExternoException;

/**
 * Cliente HTTP del microservicio de Productos: consulta de producto y ajuste de stock.
 */
@Component
public class ProductoClient {

    private final RestClient restClient;

    public ProductoClient(@Value("${productos.api.base-url:http://localhost:5001}") String baseUrl) {
        this.restClient = HttpClientes.crear(baseUrl);
    }

    /**
     * @return el producto, o vacío si el servicio responde 404.
     * @throws ServicioExternoException si el servicio no responde o devuelve un error inesperado.
     */
    public Optional<ProductoResponse> obtenerProducto(Long productoId) {
        try {
            return restClient.get()
                    .uri("/api/productos/{id}", productoId)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return Optional.ofNullable(response.bodyTo(ProductoResponse.class));
                        }
                        if (response.getStatusCode().value() == 404) {
                            return Optional.<ProductoResponse>empty();
                        }
                        throw new ServicioExternoException("El servicio de productos respondió "
                                + response.getStatusCode() + " al consultar el producto " + productoId);
                    });
        } catch (RestClientException ex) {
            throw new ServicioExternoException("No se pudo contactar al servicio de productos", ex);
        }
    }

    /**
     * Fija el stock de un producto con {@code PATCH /api/productos/{id}}.
     *
     * @throws ServicioExternoException si el servicio no responde o devuelve un error.
     */
    public void actualizarStock(Long productoId, int nuevoStock) {
        try {
            restClient.patch()
                    .uri("/api/productos/{id}", productoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("stock", nuevoStock))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ServicioExternoException(
                    "No se pudo actualizar el stock del producto " + productoId, ex);
        }
    }
}
