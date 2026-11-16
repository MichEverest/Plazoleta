package com.mich.mensajeria.domain.model;

/**
 * Cómo está configurado el envío de SMS.
 */
public enum EstadoEnvio {
    /** Los SMS llegan de verdad al celular (Twilio configurado). */
    REAL,
    /** Falta configurar Twilio: los envíos se rechazan con un error claro. */
    NO_CONFIGURADO,
    /** Solo se escriben en la consola. Únicamente si se pide a propósito con sms.provider=log. */
    SIMULADO
}
