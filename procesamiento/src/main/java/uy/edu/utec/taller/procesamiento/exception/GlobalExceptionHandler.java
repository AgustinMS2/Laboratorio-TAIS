package uy.edu.utec.taller.procesamiento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uy.edu.utec.taller.procesamiento.dto.ErrorDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FacturaNoEncontradaException.class)
    public ResponseEntity<ErrorDTO> manejarFacturaNoEncontrada(FacturaNoEncontradaException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorDTO.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /**
     * Content-Type distinto de application/json: el enunciado pide 400 con mensaje de error
     * (en lugar del 415 por defecto de Spring).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorDTO> manejarContentTypeNoSoportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorDTO.of(HttpStatus.BAD_REQUEST.value(),
                        "El Content-Type de la solicitud debe ser application/json"));
    }

    /** Parámetro de ruta o de consulta con un valor de tipo incorrecto (p. ej. un id no numérico). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> manejarTipoDeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorDTO.of(HttpStatus.BAD_REQUEST.value(),
                        "El parámetro '" + ex.getName() + "' tiene un valor inválido: '" + ex.getValue() + "'"));
    }
}
