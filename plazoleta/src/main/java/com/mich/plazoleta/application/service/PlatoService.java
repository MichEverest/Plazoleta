package com.mich.plazoleta.application.service;

import com.mich.plazoleta.domain.model.Categoria;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Plato;

import java.util.List;

public interface PlatoService {

    Plato crear(Plato plato, Long idPropietario);

    Plato actualizar(Long idPlato, Integer precio, String descripcion, Long idPropietario);

    Plato cambiarEstado(Long idPlato, Boolean activo, Long idPropietario);

    Pagina<Plato> listarPorRestaurante(Long idRestaurante, Long idCategoria, Pagina.Solicitud solicitud,
                                       Long idUsuario, String rol);

    List<Categoria> listarCategorias();
}
