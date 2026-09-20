package uy.edu.utec.taller.procesamiento.client;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.utec.taller.procesamiento.client.dto.OrdenResponse;
import uy.edu.utec.taller.procesamiento.exception.ServicioExternoException;
import uy.edu.utec.taller.procesamiento.model.ResultadoProcesamiento;

/**
 * Cliente HTTP del microservicio de Órdenes: lee la orden y le informa el resultado del procesamiento.
 */
@Component
public class OrdenClient {

    private final RestClient restClient;

    public OrdenClient(@Value("${ordenes.api.base-url:http://localhost:5002}") String baseUrl) {
        this.restClient = HttpClientes.crear(baseUrl);
    }

    /**
     * @return la orden, o vacío si el servicio responde 404.
     * @throws ServicioExternoException si el servicio no responde o devuelve un error inesperado.
     */
    public Optional<OrdenResponse> obtenerOrden(Long ordenId) {
        try {
            return restClient.get()
                    .uri("/api/ordenes/{id}", ordenId)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return Optional.ofNullable(response.bodyTo(OrdenResponse.class));
                        }
                        if (response.getStatusCode().value() == 404) {
                            return Optional.<OrdenResponse>empty();
                        }
                        throw new ServicioExternoException("El servicio de órdenes respondió "
                                + response.getStatusCode() + " al consultar la orden " + ordenId);
                    });
        } catch (RestClientException ex) {
            throw new ServicioExternoException("No se pudo contactar al servicio de órdenes", ex);
        }
    }

    /**
     * Informa el resultado con {@code PATCH /api/ordenes/{id}/estado}.
     *
     * @return {@code true} si la orden quedó en ese estado; {@code false} si el servicio de órdenes
     *         respondió 409 (la orden ya estaba en otro estado y no se puede cambiar).
     * @throws ServicioExternoException si el servicio no responde o devuelve otro error.
     */
    public boolean actualizarEstado(Long ordenId, ResultadoProcesamiento resultado) {
        try {
            return restClient.patch()
                    .uri("/api/ordenes/{id}/estado", ordenId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("estado", resultado.getValor()))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return true;
                        }
                        if (response.getStatusCode().value() == 409) {
                            return false;
                        }
                        throw new ServicioExternoException("El servicio de órdenes respondió "
                                + response.getStatusCode() + " al actualizar el estado de la orden " + ordenId);
                    });
        } catch (RestClientException ex) {
            throw new ServicioExternoException(
                    "No se pudo actualizar el estado de la orden " + ordenId, ex);
        }
    }
}
