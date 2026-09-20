package uy.edu.utec.taller.procesamiento.exception;

/**
 * Fallo transitorio al hablar con otro microservicio (caído, timeout, 5xx). El suscriptor
 * reintenta el procesamiento cuando ocurre.
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
