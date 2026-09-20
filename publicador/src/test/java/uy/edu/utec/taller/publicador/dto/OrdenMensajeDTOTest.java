package uy.edu.utec.taller.publicador.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uy.edu.utec.taller.publicador.client.dto.OrdenResponse;

class OrdenMensajeDTOTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("el mensaje publicado tiene exactamente id, estado y fechaCreacion, con la fecha intacta")
    void testFormatoDelMensaje() throws Exception {
        OrdenResponse orden = OrdenResponse.builder()
                .id(1L).estado("Created").fechaCreacion("2026-06-21T14:30:00-03:00").build();

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(OrdenMensajeDTO.fromOrden(orden)));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder("id", "estado", "fechaCreacion");
        assertThat(json.get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("estado").asString()).isEqualTo("Created");
        assertThat(json.get("fechaCreacion").asString()).isEqualTo("2026-06-21T14:30:00-03:00");
    }

    @Test
    @DisplayName("la respuesta de órdenes se lee ignorando los campos que no interesan")
    void testLecturaDeLaRespuestaDeOrdenes() throws Exception {
        String json = """
                {"id":7,"email":"a@b.com","direccionEnvio":"x","telefono":"1","estado":"Created",
                 "fechaCreacion":"2026-09-19T20:18:15.668994-03:00","productos":[{"productoId":1,"cantidad":2}]}
                """;

        OrdenResponse orden = jsonMapper.readValue(json, OrdenResponse.class);

        assertThat(orden.getId()).isEqualTo(7L);
        assertThat(orden.getEstado()).isEqualTo("Created");
        assertThat(orden.getFechaCreacion()).isEqualTo("2026-09-19T20:18:15.668994-03:00");
    }
}
