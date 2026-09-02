package uy.edu.utec.taller.ordenes.exception;

/**
 * Se lanza cuando el microservicio de Productos no puede ser consultado
 * (no responde o devuelve un error inesperado).
 */
public class ProductoServicioException extends RuntimeException {

    public ProductoServicioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ProductoServicioException(String mensaje) {
        super(mensaje);
    }
}
