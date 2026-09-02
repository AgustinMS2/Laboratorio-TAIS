package uy.edu.utec.taller.ordenes.client;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.utec.taller.ordenes.client.dto.ProductoResponse;
import uy.edu.utec.taller.ordenes.exception.ProductoServicioException;

/**
 * Cliente HTTP del microservicio de Productos.
 */
@Component
public class ProductoClient {

    private static final Logger log = LoggerFactory.getLogger(ProductoClient.class);

    private final RestClient restClient;

    public ProductoClient(@Value("${productos.api.base-url:http://localhost:5001}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    /**
     * Obtiene un producto por su id.
     *
     * @return el producto, o {@link Optional#empty()} si el servicio de productos
     *         responde 404 (el producto no existe).
     * @throws ProductoServicioException si el servicio de productos no responde o
     *         devuelve un error inesperado.
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
                            return Optional.empty();
                        }
                        throw new ProductoServicioException(
                                "El servicio de productos respondió " + response.getStatusCode()
                                        + " al consultar el producto " + productoId);
                    });
        } catch (RestClientException ex) {
            log.warn("Fallo al consultar el producto {} en el servicio de productos", productoId, ex);
            throw new ProductoServicioException(
                    "No se pudo contactar al servicio de productos", ex);
        }
    }

    /**
     * Ajusta el stock de un producto vía {@code PATCH /api/productos/{id}}.
     *
     * @throws ProductoServicioException si el servicio de productos no responde o
     *         devuelve un error.
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
            log.warn("No se pudo actualizar el stock del producto {}", productoId, ex);
            throw new ProductoServicioException(
                    "No se pudo actualizar el stock del producto " + productoId, ex);
        }
    }
}
