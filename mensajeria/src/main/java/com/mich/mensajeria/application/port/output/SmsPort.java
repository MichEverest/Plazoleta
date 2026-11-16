package com.mich.mensajeria.application.port.output;

import com.mich.mensajeria.domain.model.EstadoEnvio;

public interface SmsPort {

    /**
     * Envía el SMS a un celular colombiano de 10 dígitos (sin indicativo). Si no puede entregarlo lanza una excepción:
     * nunca lo descarta en silencio.
     */
    void enviar(String celular, String mensaje);

    EstadoEnvio estado();

    /**
     * Nombre legible de por dónde salen los SMS (para mostrarlo en las consolas).
     */
    String proveedor();
}
