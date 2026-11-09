package com.mich.plazoleta.domain.model;

public record Usuario(Long id, String nombre, String apellido, String numeroDocumento, String celular,
                      String correo, String clave, Rol rol) {

    public enum Rol {
        ADMINISTRADOR,
        PROPIETARIO,
        EMPLEADO,
        CLIENTE
    }
}
