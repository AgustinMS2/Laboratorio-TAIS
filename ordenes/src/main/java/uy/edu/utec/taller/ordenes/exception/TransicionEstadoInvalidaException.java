package uy.edu.utec.taller.ordenes.exception;

import java.util.List;
import uy.edu.utec.taller.ordenes.model.EstadoOrden;

public class TransicionEstadoInvalidaException extends ConflictoOrdenException {

    public TransicionEstadoInvalidaException(Long id, EstadoOrden actual, EstadoOrden pedido) {
        super("La orden " + id + " no puede pasar de '" + actual.getValor() + "' a '" + pedido.getValor() + "'",
                List.of("Solo una orden en estado 'Created' puede pasar a 'Ready to Delivery' o 'No Stock'"));
    }
}
