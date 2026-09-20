package uy.edu.utec.taller.procesamiento.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Factura de una orden que quedó en {@code Ready to Delivery}: un ítem por producto solicitado
 * y el total (suma de los subtotales). Guarda una copia de los datos de facturación del cliente.
 */
@Entity
@Table(name = "facturas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long ordenId;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 250)
    private String direccionEnvio;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "factura_id", nullable = false)
    @Builder.Default
    private List<FacturaItem> items = new ArrayList<>();
}
