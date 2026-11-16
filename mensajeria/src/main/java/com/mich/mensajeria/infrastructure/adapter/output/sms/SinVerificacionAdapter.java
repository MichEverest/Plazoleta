package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.VerificacionPort;

/**
 * No hay proveedor de verificación: el PIN propio de Plazoleta se envía como texto con el SmsPort configurado.
 */
public class SinVerificacionAdapter implements VerificacionPort {

    private static final String MENSAJE = "No hay un proveedor de verificación de códigos configurado";

    @Override
    public boolean disponible() {
        return false;
    }

    @Override
    public String proveedor() {
        return "ninguno";
    }

    @Override
    public void enviarCodigo(String celular) {
        throw new IllegalStateException(MENSAJE);
    }

    @Override
    public boolean comprobarCodigo(String celular, String codigo) {
        throw new IllegalStateException(MENSAJE);
    }
}
