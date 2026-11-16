package com.mich.usuarios.application.service;

import com.mich.usuarios.domain.model.Usuario;

import java.util.List;

public interface UsuarioService {

    Usuario crearPropietario(Usuario usuario);

    Usuario crearEmpleado(Usuario usuario);

    Usuario crearCliente(Usuario usuario);

    Usuario consultarPorId(Long id);

    List<Usuario> listar();

    Usuario.Sesion iniciarSesion(String correo, String clave);
}
