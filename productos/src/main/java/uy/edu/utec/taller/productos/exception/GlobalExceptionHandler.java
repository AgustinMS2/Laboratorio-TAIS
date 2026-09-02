package uy.edu.utec.taller.productos.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uy.edu.utec.taller.productos.dto.ErrorDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ErrorDTO> manejarProductoNoEncontrado(ProductoNoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorDTO.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
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
}
