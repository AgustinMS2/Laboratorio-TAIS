package uy.edu.utec.taller.ordenes.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

public enum EstadoOrden {
    Created("Created"),
    ReadyToDelivery("Ready to Delivery"),
    NoStock("No Stock"),
    Confirmed("Confirmed"),
    Shipped("Shipped"),
    Delivered("Delivered"),
    Cancelled("Cancelled");

    private final String valor;

    EstadoOrden(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoOrden desdeValor(String valor) {
        return buscar(valor)
                .orElseThrow(() -> new IllegalArgumentException("Estado de orden desconocido: " + valor));
    }

    public static Optional<EstadoOrden> buscar(String valor) {
        return Arrays.stream(values())
                .filter(estado -> estado.valor.equalsIgnoreCase(valor))
                .findFirst();
    }
}
