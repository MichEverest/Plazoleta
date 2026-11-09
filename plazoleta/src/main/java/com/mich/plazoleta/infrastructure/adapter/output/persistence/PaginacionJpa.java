package com.mich.plazoleta.infrastructure.adapter.output.persistence;

import com.mich.plazoleta.domain.model.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

final class PaginacionJpa {

    private PaginacionJpa() {
    }

    static PageRequest solicitud(Pagina.Solicitud solicitud, Sort orden) {
        return PageRequest.of(solicitud.pagina(), solicitud.tamano(), orden);
    }

    static <E, T> Pagina<T> pagina(Page<E> page, Function<E, T> mapper) {
        return new Pagina<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
