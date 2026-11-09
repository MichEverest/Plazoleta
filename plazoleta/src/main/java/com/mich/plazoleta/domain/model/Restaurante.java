package com.mich.plazoleta.domain.model;

public record Restaurante(Long id, String nombre, String nit, String direccion, String telefono, String urlLogo,
                          Long idPropietario) {
}
