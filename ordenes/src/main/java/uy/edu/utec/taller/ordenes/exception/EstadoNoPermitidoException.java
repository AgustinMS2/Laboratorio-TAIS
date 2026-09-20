package uy.edu.utec.taller.ordenes.exception;

import uy.edu.utec.taller.ordenes.model.EstadoOrden;

public class EstadoNoPermitidoException extends RuntimeException {

    public EstadoNoPermitidoException(EstadoOrden estado) {
        super("El estado '" + estado.getValor() + "' no se puede asignar por esta vía; "
                + "solo 'Ready to Delivery' o 'No Stock'");
    }
}
