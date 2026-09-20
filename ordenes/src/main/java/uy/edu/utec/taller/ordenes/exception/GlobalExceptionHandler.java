package uy.edu.utec.taller.ordenes.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uy.edu.utec.taller.ordenes.dto.ErrorDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrdenNoEncontradaException.class)
    public ResponseEntity<ErrorDTO> manejarOrdenNoEncontrada(OrdenNoEncontradaException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorDTO.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(ConflictoOrdenException.class)
    public ResponseEntity<ErrorDTO> manejarConflictoOrden(ConflictoOrdenException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .codigo(HttpStatus.CONFLICT.value())
                .mensaje(ex.getMessage())
                .detalles(ex.getDetalles())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(EstadoDesconocidoException.class)
    public ResponseEntity<ErrorDTO> manejarEstadoDesconocido(EstadoDesconocidoException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorDTO.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(EstadoNoPermitidoException.class)
    public ResponseEntity<ErrorDTO> manejarEstadoNoPermitido(EstadoNoPermitidoException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorDTO.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                detalles.add(error.getField() + ": " + error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                detalles.add(error.getDefaultMessage()));
        detalles.sort(String::compareTo);
        ErrorDTO error = ErrorDTO.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje("Datos de entrada inválidos")
                .detalles(detalles)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorDTO.of(HttpStatus.BAD_REQUEST.value(),
                        "El cuerpo de la solicitud no es un JSON válido"));
    }

    @ExceptionHandler(ProductoServicioException.class)
    public ResponseEntity<ErrorDTO> manejarProductoServicioNoDisponible(ProductoServicioException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ErrorDTO.of(HttpStatus.BAD_GATEWAY.value(), ex.getMessage()));
    }


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
