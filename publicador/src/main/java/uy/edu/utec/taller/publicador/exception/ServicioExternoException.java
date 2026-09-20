package uy.edu.utec.taller.publicador.exception;

/**
 * Fallo al hablar con el servicio de órdenes (caído, timeout, error inesperado). La pasada de
 * publicación se aborta y se reintenta en el siguiente ciclo.
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
