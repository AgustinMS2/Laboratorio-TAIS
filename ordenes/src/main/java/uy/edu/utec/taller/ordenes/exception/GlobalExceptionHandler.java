package uy.edu.utec.taller.ordenes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(ProductoServicioException.class)
    public ResponseEntity<ErrorDTO> manejarProductoServicioNoDisponible(ProductoServicioException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ErrorDTO.of(HttpStatus.BAD_GATEWAY.value(), ex.getMessage()));
    }
}
