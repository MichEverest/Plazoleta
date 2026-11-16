package com.mich.mensajeria.domain.exception;

public final class ExcepcionesDominio {

    private ExcepcionesDominio() {
    }

    public static class ValidacionException extends RuntimeException {
        public ValidacionException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ProveedorSmsException extends RuntimeException {
        public ProveedorSmsException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SmsNoConfiguradoException extends RuntimeException {
        public SmsNoConfiguradoException(String mensaje) {
            super(mensaje);
        }
    }
}
