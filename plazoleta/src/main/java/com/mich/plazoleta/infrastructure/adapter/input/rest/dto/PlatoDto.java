package com.mich.plazoleta.infrastructure.adapter.input.rest.dto;

import com.mich.plazoleta.domain.model.Plato;

public final class PlatoDto {

    private PlatoDto() {
    }

    public record Request(String nombre, Integer precio, String descripcion, String urlImagen, Long idCategoria,
                          Long idRestaurante) {

        public Plato toDomain() {
            return new Plato(null, nombre, idCategoria, descripcion, precio, urlImagen, null, idRestaurante);
        }
    }

    public record Actualizacion(Integer precio, String descripcion) {
    }

    public record Estado(Boolean activo) {
    }

    public record Response(Long id, String nombre, Long idCategoria, String descripcion, Integer precio,
                           String urlImagen, Boolean activo, Long idRestaurante) {

        public static Response de(Plato plato) {
            return new Response(plato.getId(), plato.getNombre(), plato.getIdCategoria(), plato.getDescripcion(),
                    plato.getPrecio(), plato.getUrlImagen(), plato.getActivo(), plato.getIdRestaurante());
        }
    }

    public record Categoria(Long id, String nombre, String descripcion) {

        public static Categoria de(com.mich.plazoleta.domain.model.Categoria categoria) {
            return new Categoria(categoria.id(), categoria.nombre(), categoria.descripcion());
        }
    }
}
