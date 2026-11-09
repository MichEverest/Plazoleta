package com.mich.plazoleta.application.port.output;

import com.mich.plazoleta.domain.model.Categoria;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Plato;

import java.util.List;
import java.util.Optional;

public interface PlatoRepositoryPort {

    Plato guardar(Plato plato);

    Optional<Plato> buscarPorId(Long id);

    Pagina<Plato> listarPorRestaurante(Long idRestaurante, Long idCategoria, boolean soloActivos,
                                       Pagina.Solicitud solicitud);

    List<Categoria> listarCategorias();

    boolean existeCategoria(Long id);
}
