package com.mich.plazoleta.infrastructure.adapter.input.rest;

import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ConflictoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ProhibidoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MENSAJE_CUERPO_INVALIDO = "El cuerpo de la solicitud no es válido";
    private static final String MENSAJE_PARAMETRO_INVALIDO = "El parámetro %s tiene un valor inválido";
    private static final String MENSAJE_ERROR_INTERNO = "Error interno del servidor";
    private static final String MENSAJE_SERVICIO_NO_DISPONIBLE = "Un servicio externo no está disponible";
    private static final String MENSAJE_ERROR_SERVICIO_EXTERNO = "Error en un servicio externo";
    private static final String CAMPO_MENSAJE = "mensaje";

    public record ErrorResponse(String mensaje, int codigo, LocalDateTime fecha) {

        public ErrorResponse(String mensaje, HttpStatus status) {
            this(mensaje, status.value(), LocalDateTime.now());
        }
    }

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(RuntimeException ex) {
        return responder(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RuntimeException ex) {
        return responder(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProhibidoException.class)
    public ResponseEntity<ErrorResponse> handleProhibido(RuntimeException ex) {
        return responder(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> handleConflicto(RuntimeException ex) {
        return responder(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleServicioExterno(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            LOGGER.error(MENSAJE_SERVICIO_NO_DISPONIBLE, ex);
            return responder(MENSAJE_SERVICIO_NO_DISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
        }
        return responder(extraerMensaje(ex.contentUTF8()), status);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCuerpoInvalido() {
        return responder(MENSAJE_CUERPO_INVALIDO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleParametroInvalido(TypeMismatchException ex) {
        return responder(String.format(MENSAJE_PARAMETRO_INVALIDO, ex.getPropertyName()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse errorWeb) {
            return responder(errorWeb.getBody().getDetail(), HttpStatus.valueOf(errorWeb.getStatusCode().value()));
        }
        LOGGER.error(MENSAJE_ERROR_INTERNO, ex);
        return responder(MENSAJE_ERROR_INTERNO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String extraerMensaje(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return MENSAJE_ERROR_SERVICIO_EXTERNO;
        }
        try {
            return objectMapper.readTree(cuerpo).path(CAMPO_MENSAJE).asString(MENSAJE_ERROR_SERVICIO_EXTERNO);
        } catch (JacksonException ex) {
            return MENSAJE_ERROR_SERVICIO_EXTERNO;
        }
    }

    private ResponseEntity<ErrorResponse> responder(String mensaje, HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorResponse(mensaje, status));
    }
}
