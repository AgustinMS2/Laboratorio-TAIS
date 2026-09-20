package uy.edu.utec.taller.procesamiento.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/**
 * Resultado de procesar una orden. Coincide con los estados que se informan al servicio de
 * órdenes: {@code Ready to Delivery} (hubo stock, se descontó y se facturó) o {@code No Stock}.
 */
public enum ResultadoProcesamiento {
    ReadyToDelivery("Ready to Delivery"),
    NoStock("No Stock");

    private final String valor;

    ResultadoProcesamiento(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ResultadoProcesamiento desdeValor(String valor) {
        return Arrays.stream(values())
                .filter(r -> r.valor.equalsIgnoreCase(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Resultado desconocido: " + valor));
    }
}
