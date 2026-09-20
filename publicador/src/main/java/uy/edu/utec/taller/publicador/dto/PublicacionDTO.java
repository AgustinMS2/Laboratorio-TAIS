package uy.edu.utec.taller.publicador.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.edu.utec.taller.publicador.model.Publicacion;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionDTO {

    private Long ordenId;
    private OffsetDateTime ultimaPublicacion;
    private int veces;

    public static PublicacionDTO fromModel(Publicacion publicacion) {
        return PublicacionDTO.builder()
                .ordenId(publicacion.getOrdenId())
                .ultimaPublicacion(publicacion.getUltimaPublicacion())
                .veces(publicacion.getVeces())
                .build();
    }
}
