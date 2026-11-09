package com.mich.mensajeria.domain.exception;

public final class ExcepcionesDominio {

    private ExcepcionesDominio() {
    }

    public static class ValidacionException extends RuntimeException {
        public ValidacionException(String mensaje) {
            super(mensaje);
        }
    }
}
