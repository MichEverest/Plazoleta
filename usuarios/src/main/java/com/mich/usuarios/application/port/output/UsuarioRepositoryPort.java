package com.mich.usuarios.application.port.output;

import com.mich.usuarios.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {

    Usuario guardar(Usuario usuario);

    boolean existeCorreo(String correo);

    boolean existeNumeroDocumento(String numeroDocumento);

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorCorreo(String correo);
}
