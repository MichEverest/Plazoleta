package com.mich.plazoleta.application.port.output;

import com.mich.plazoleta.domain.model.Pedido;
import com.mich.plazoleta.domain.model.Usuario;

import java.util.Optional;

public interface ServiciosExternosPort {

    Optional<Usuario> buscarUsuario(Long id);

    Long crearEmpleado(Usuario empleado);

    /**
     * Envía al cliente el PIN de su pedido. Devuelve true si el proveedor genera y valida su propio código (Twilio Verify):
     * en ese caso el PIN propuesto no se usa y la entrega se comprueba con {@link #verificarPinExterno}.
     */
    boolean enviarPin(String celular, String mensaje, String pin);

    /**
     * Pregunta al proveedor si el código que dio el cliente es el que le llegó por SMS.
     */
    boolean verificarPinExterno(String celular, String codigo);

    void registrarCambioEstado(Pedido.CambioEstado cambio);
}
