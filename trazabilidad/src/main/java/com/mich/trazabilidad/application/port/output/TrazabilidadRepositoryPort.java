package com.mich.trazabilidad.application.port.output;

import com.mich.trazabilidad.domain.model.Trazabilidad;

import java.util.List;

public interface TrazabilidadRepositoryPort {

    void guardar(Trazabilidad trazabilidad);

    List<Trazabilidad> listarPorPedido(Long idPedido);

    List<Trazabilidad> listarPorRestaurante(Long idRestaurante);
}
