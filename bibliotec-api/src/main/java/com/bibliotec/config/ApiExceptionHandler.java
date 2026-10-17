package com.bibliotec.config;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import com.bibliotec.dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> manejarEstado(
            ResponseStatusException exception,
            HttpServletRequest request) {
        int estado = exception.getStatusCode().value();
        String mensaje = exception.getReason() == null
            ? "No se pudo completar la operación"
            : exception.getReason();
        return ResponseEntity.status(estado).body(new ApiError(
            LocalDateTime.now(), estado, exception.getStatusCode().toString(),
            mensaje, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining(", "));
        return crear(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarJsonInvalido(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return crear(HttpStatus.BAD_REQUEST, "El cuerpo JSON no es válido", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> manejarIntegridad(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return crear(HttpStatus.CONFLICT,
            "La operación entra en conflicto con datos existentes", request);
    }

    private ResponseEntity<ApiError> crear(
            HttpStatus estado,
            String mensaje,
            HttpServletRequest request) {
        return ResponseEntity.status(estado).body(new ApiError(
            LocalDateTime.now(), estado.value(), estado.getReasonPhrase(),
            mensaje, request.getRequestURI()));
    }
}
