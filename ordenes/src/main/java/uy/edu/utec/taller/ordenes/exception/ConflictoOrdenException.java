package uy.edu.utec.taller.ordenes.exception;

import java.util.List;

/**
 * Conflicto al registrar una orden: algún producto no existe o no tiene stock
 * suficiente. Se traduce a {@code 409 Conflict}.
 */
public abstract class ConflictoOrdenException extends RuntimeException {

    private final transient List<String> detalles;

    protected ConflictoOrdenException(String mensaje, List<String> detalles) {
        super(mensaje);
        this.detalles = List.copyOf(detalles);
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
