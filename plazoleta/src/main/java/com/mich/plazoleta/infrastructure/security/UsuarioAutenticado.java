package com.mich.plazoleta.infrastructure.security;

import org.springframework.security.core.context.SecurityContextHolder;

public record UsuarioAutenticado(Long id, String correo, String rol) {

    public static UsuarioAutenticado actual() {
        return (UsuarioAutenticado) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
