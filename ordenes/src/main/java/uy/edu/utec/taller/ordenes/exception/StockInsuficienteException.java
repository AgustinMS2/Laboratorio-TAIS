package uy.edu.utec.taller.ordenes.exception;

import java.util.List;

public class StockInsuficienteException extends ConflictoOrdenException {

    public StockInsuficienteException(List<String> detalles) {
        super("Stock insuficiente para uno o más productos solicitados", detalles);
    }
}
