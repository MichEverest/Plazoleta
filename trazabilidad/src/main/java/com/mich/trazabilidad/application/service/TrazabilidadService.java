package com.mich.trazabilidad.application.service;

import com.mich.trazabilidad.domain.model.Eficiencia;
import com.mich.trazabilidad.domain.model.Pagina;
import com.mich.trazabilidad.domain.model.Trazabilidad;

import java.util.List;

public interface TrazabilidadService {

    void registrar(Trazabilidad trazabilidad);

    List<Trazabilidad.Historial> consultarHistorial(Long idPedido, Long idCliente);

    Eficiencia.Reporte calcularEficienciaPedidos(Pagina.Solicitud solicitud);

    Pagina<Eficiencia.Empleado> calcularRankingEmpleados(Pagina.Solicitud solicitud);
}
