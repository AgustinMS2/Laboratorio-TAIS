package uy.edu.utec.taller.publicador.client;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Construye el {@link RestClient} con timeouts, para que un servicio colgado no deje bloqueado al publicador.
 */
final class HttpClientes {

    private HttpClientes() {
    }

    static RestClient crear(String baseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
