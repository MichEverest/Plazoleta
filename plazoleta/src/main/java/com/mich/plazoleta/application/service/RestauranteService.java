package com.mich.plazoleta.application.service;

import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.domain.model.Usuario;

public interface RestauranteService {

    Restaurante crear(Restaurante restaurante);

    Restaurante consultarDePropietario(Long idPropietario);

    Pagina<Restaurante> listar(Pagina.Solicitud solicitud);

    Long crearEmpleado(Usuario empleado, Long idPropietario);
}
