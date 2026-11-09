package com.mich.plazoleta.application.usecase;

import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;

import java.util.regex.Pattern;

final class Validaciones {

    private static final Pattern PATRON_NUMERICO = Pattern.compile("^\\d+$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\+?\\d{1,12}$|^\\d{13}$");
    private static final String MENSAJE_CAMPO_OBLIGATORIO = "El campo %s es obligatorio";

    private Validaciones() {
    }

    static void requerido(Object valor, String campo) {
        if (valor == null || valor instanceof String texto && texto.isBlank()) {
            throw new ValidacionException(String.format(MENSAJE_CAMPO_OBLIGATORIO, campo));
        }
    }

    static boolean esNumerico(String valor) {
        return PATRON_NUMERICO.matcher(valor).matches();
    }

    static boolean esTelefonoValido(String valor) {
        return PATRON_TELEFONO.matcher(valor).matches();
    }
}
