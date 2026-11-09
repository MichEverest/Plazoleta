package com.mich.trazabilidad.domain.exception;

public final class ExcepcionesDominio {

    private ExcepcionesDominio() {
    }

    public static class ValidacionException extends RuntimeException {
        public ValidacionException(String mensaje) {
            super(mensaje);
        }
    }

    public static class NoEncontradoException extends RuntimeException {
        public NoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ProhibidoException extends RuntimeException {
        public ProhibidoException(String mensaje) {
            super(mensaje);
        }
    }
}
