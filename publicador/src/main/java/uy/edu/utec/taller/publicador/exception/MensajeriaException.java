package uy.edu.utec.taller.publicador.exception;

/**
 * No se pudo publicar un mensaje en el broker (caído, inaccesible o sin confirmar).
 */
public class MensajeriaException extends RuntimeException {

    public MensajeriaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
