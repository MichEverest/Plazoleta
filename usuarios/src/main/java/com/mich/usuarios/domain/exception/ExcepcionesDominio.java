package com.mich.usuarios.domain.exception;

public final class ExcepcionesDominio {

    private ExcepcionesDominio() {
    }

    public static class ValidacionException extends RuntimeException {
        public ValidacionException(String mensaje) {
            super(mensaje);
        }
    }

    public static class NoAutorizadoException extends RuntimeException {
        public NoAutorizadoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class NoEncontradoException extends RuntimeException {
        public NoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ConflictoException extends RuntimeException {
        public ConflictoException(String mensaje) {
            super(mensaje);
        }
    }
}
