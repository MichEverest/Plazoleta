package com.mich.plazoleta.application.port.output;

import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Restaurante;

import java.util.Optional;

public interface RestauranteRepositoryPort {

    Restaurante guardar(Restaurante restaurante);

    boolean existeNit(String nit);

    boolean existeDePropietario(Long idPropietario);

    Optional<Restaurante> buscarPorId(Long id);

    Optional<Restaurante> buscarPorPropietario(Long idPropietario);

    Pagina<Restaurante> listar(Pagina.Solicitud solicitud);

    void vincularEmpleado(Long idRestaurante, Long idEmpleado);

    Optional<Long> buscarRestauranteDeEmpleado(Long idEmpleado);
}
