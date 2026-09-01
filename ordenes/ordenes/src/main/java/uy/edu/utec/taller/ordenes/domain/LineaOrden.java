package uy.edu.utec.taller.ordenes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "linea_orden")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineaOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private Integer cantidad;

    public LineaOrden(Long productoId, Integer cantidad) {
        this.productoId = productoId;
        this.cantidad = cantidad;
    }
}
