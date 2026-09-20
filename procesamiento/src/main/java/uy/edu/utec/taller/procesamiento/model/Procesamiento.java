package uy.edu.utec.taller.procesamiento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registro de que una orden ya fue procesada. Es la base de la idempotencia: {@code ordenId}
 * es único, así que un mensaje duplicado nunca procesa dos veces la misma orden.
 */
@Entity
@Table(name = "procesamientos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Procesamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long ordenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoProcesamiento resultado;

    @Column(nullable = false)
    private OffsetDateTime fechaProcesamiento;

    /** {@code true} cuando el nuevo estado ya se informó al servicio de órdenes. */
    @Column(nullable = false)
    private boolean ordenActualizada;

    @Column(length = 500)
    private String detalle;
}
