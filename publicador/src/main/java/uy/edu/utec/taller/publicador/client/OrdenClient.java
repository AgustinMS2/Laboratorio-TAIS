package uy.edu.utec.taller.publicador.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.utec.taller.publicador.client.dto.OrdenResponse;
import uy.edu.utec.taller.publicador.exception.ServicioExternoException;

/**
 * Cliente HTTP del microservicio de Órdenes: obtiene las órdenes que están listas para procesar.
 */
@Component
public class OrdenClient {

    private final RestClient restClient;

    public OrdenClient(@Value("${ordenes.api.base-url:http://localhost:5002}") String baseUrl) {
        this.restClient = HttpClientes.crear(baseUrl);
    }

    /**
     * Lista las órdenes en el estado indicado con {@code GET /api/ordenes?estado=...}.
     *
     * @throws ServicioExternoException si el servicio de órdenes no responde o devuelve un error.
     */
    public List<OrdenResponse> listarPorEstado(String estado) {
        try {
            return restClient.get()
                    .uri("/api/ordenes?estado={estado}", estado)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new ServicioExternoException("El servicio de órdenes respondió "
                                    + response.getStatusCode() + " al listar las órdenes en estado " + estado);
                        }
                        List<OrdenResponse> ordenes = response.bodyTo(new ParameterizedTypeReference<>() { });
                        return ordenes == null ? List.<OrdenResponse>of() : ordenes;
                    });
        } catch (RestClientException ex) {
            throw new ServicioExternoException("No se pudo contactar al servicio de órdenes", ex);
        }
    }
}
