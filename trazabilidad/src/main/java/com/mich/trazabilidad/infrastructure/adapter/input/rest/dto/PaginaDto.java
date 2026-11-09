package com.mich.trazabilidad.infrastructure.adapter.input.rest.dto;

import com.mich.trazabilidad.domain.model.Pagina;

import java.util.List;
import java.util.function.Function;

public record PaginaDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <D, T> PaginaDto<T> de(Pagina<D> pagina, Function<D, T> mapper) {
        return new PaginaDto<>(pagina.contenido().stream().map(mapper).toList(), pagina.pagina(), pagina.tamano(),
                pagina.totalElementos(), pagina.totalPaginas());
    }
}
