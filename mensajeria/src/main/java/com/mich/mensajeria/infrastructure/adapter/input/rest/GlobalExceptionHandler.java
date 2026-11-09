package com.mich.mensajeria.infrastructure.adapter.input.rest;

import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ValidacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MENSAJE_CUERPO_INVALIDO = "El cuerpo de la solicitud no es válido";
    private static final String MENSAJE_ERROR_INTERNO = "Error interno del servidor";

    public record ErrorResponse(String mensaje, int codigo, LocalDateTime fecha) {

        public ErrorResponse(String mensaje, HttpStatus status) {
            this(mensaje, status.value(), LocalDateTime.now());
        }
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(RuntimeException ex) {
        return responder(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCuerpoInvalido() {
        return responder(MENSAJE_CUERPO_INVALIDO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse errorWeb) {
            return responder(errorWeb.getBody().getDetail(), HttpStatus.valueOf(errorWeb.getStatusCode().value()));
        }
        LOGGER.error(MENSAJE_ERROR_INTERNO, ex);
        return responder(MENSAJE_ERROR_INTERNO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> responder(String mensaje, HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorResponse(mensaje, status));
    }
}
