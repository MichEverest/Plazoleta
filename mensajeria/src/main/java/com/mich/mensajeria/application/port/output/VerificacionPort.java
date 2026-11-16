package com.mich.mensajeria.application.port.output;

/**
 * Proveedor que genera, envía y valida él mismo el código de entrega (por ejemplo Twilio Verify).
 * Cuando está disponible se usa en lugar de enviar el PIN propio de Plazoleta como texto.
 */
public interface VerificacionPort {

    boolean disponible();

    String proveedor();

    /**
     * Pide al proveedor generar un código y enviarlo por SMS al celular (10 dígitos, sin indicativo).
     */
    void enviarCodigo(String celular);

    /**
     * true si el código es el último que el proveedor envió a ese celular y sigue vigente.
     */
    boolean comprobarCodigo(String celular, String codigo);
}
