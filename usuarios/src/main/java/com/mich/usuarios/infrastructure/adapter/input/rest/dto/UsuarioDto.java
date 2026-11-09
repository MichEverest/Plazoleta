package com.mich.usuarios.infrastructure.adapter.input.rest.dto;

import com.mich.usuarios.domain.model.Usuario;

import java.time.LocalDate;

public final class UsuarioDto {

    private UsuarioDto() {
    }

    public record Request(String nombre, String apellido, String numeroDocumento, String celular,
                          LocalDate fechaNacimiento, String correo, String clave) {

        public Usuario toDomain() {
            return new Usuario(null, nombre, apellido, numeroDocumento, celular, fechaNacimiento, correo, clave, null);
        }
    }

    public record Response(Long id, String nombre, String correo, String celular, String rol) {

        public static Response de(Usuario usuario) {
            return new Response(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getCelular(),
                    usuario.getRol().name());
        }
    }

    public record Creado(Long id) {
    }

    public record Login(String correo, String clave) {
    }

    public record Sesion(String token, Long id, String rol) {

        public static Sesion de(Usuario.Sesion sesion) {
            return new Sesion(sesion.token(), sesion.usuario().getId(), sesion.usuario().getRol().name());
        }
    }
}
