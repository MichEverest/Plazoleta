package com.mich.usuarios.application.port.output;

import com.mich.usuarios.domain.model.Usuario;

public interface SeguridadPort {

    String encriptarClave(String clave);

    boolean claveCoincide(String clave, String claveEncriptada);

    String generarToken(Usuario usuario);
}
