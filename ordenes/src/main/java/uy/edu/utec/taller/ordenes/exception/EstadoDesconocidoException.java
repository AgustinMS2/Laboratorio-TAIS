package uy.edu.utec.taller.ordenes.exception;

public class EstadoDesconocidoException extends RuntimeException {

    public EstadoDesconocidoException(String valor) {
        super("El estado '" + valor + "' no existe; valores válidos: Created, Ready to Delivery, No Stock, "
                + "Confirmed, Shipped, Delivered, Cancelled");
    }
}
