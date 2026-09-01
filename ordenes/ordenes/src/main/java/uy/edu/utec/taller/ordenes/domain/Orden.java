package uy.edu.utec.taller.ordenes.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orden")
@Getter
@Setter
@NoArgsConstructor
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 250)
    private String direccionEnvio;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.Created;

    @Column(nullable = false)
    private OffsetDateTime fechaCreacion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "orden_id", nullable = false)
    private List<LineaOrden> productos = new ArrayList<>();

    @PrePersist
    void asignarFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = OffsetDateTime.now();
        }
    }
}
