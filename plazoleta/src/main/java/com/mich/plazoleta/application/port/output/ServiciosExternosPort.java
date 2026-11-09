package com.mich.plazoleta.application.port.output;

import com.mich.plazoleta.domain.model.Pedido;
import com.mich.plazoleta.domain.model.Usuario;

import java.util.Optional;

public interface ServiciosExternosPort {

    Optional<Usuario> buscarUsuario(Long id);

    Long crearEmpleado(Usuario empleado);

    void enviarSms(String celular, String mensaje);

    void registrarCambioEstado(Pedido.CambioEstado cambio);
}
