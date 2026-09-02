package uy.edu.utec.taller.ordenes.exception;

import java.util.List;

public class ProductosInexistentesException extends ConflictoOrdenException {

    public ProductosInexistentesException(List<String> detalles) {
        super("Uno o más productos solicitados no existen", detalles);
    }
}
