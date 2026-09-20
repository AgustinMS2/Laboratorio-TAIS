package uy.edu.utec.taller.procesamiento.exception;

public class FacturaNoEncontradaException extends RuntimeException {

    public FacturaNoEncontradaException(Long id) {
        super("No existe la factura con id " + id);
    }
}
