package uy.edu.utec.taller.ordenes.exception;

public class OrdenNoEncontradaException extends RuntimeException {

    public OrdenNoEncontradaException(Long id) {
        super("No existe la orden con id " + id);
    }
}
