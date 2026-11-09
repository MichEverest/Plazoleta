package com.mich.trazabilidad.application.usecase;

import com.mich.trazabilidad.application.port.output.PlazoletaPort;
import com.mich.trazabilidad.application.port.output.TrazabilidadRepositoryPort;
import com.mich.trazabilidad.application.service.TrazabilidadService;
import com.mich.trazabilidad.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.trazabilidad.domain.exception.ExcepcionesDominio.ProhibidoException;
import com.mich.trazabilidad.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.trazabilidad.domain.model.Eficiencia;
import com.mich.trazabilidad.domain.model.Pagina;
import com.mich.trazabilidad.domain.model.Trazabilidad;
import com.mich.trazabilidad.domain.model.Trazabilidad.Estado;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TrazabilidadUseCase implements TrazabilidadService {

    private static final String MENSAJE_CAMPO_OBLIGATORIO = "El campo %s es obligatorio";
    private static final String MENSAJE_SIN_TRAZABILIDAD = "No hay trazabilidad para el pedido %d";
    private static final String MENSAJE_PEDIDO_AJENO = "El pedido no pertenece al cliente autenticado";

    private final TrazabilidadRepositoryPort trazabilidadRepositoryPort;
    private final PlazoletaPort plazoletaPort;

    public TrazabilidadUseCase(TrazabilidadRepositoryPort trazabilidadRepositoryPort, PlazoletaPort plazoletaPort) {
        this.trazabilidadRepositoryPort = trazabilidadRepositoryPort;
        this.plazoletaPort = plazoletaPort;
    }

    @Override
    public void registrar(Trazabilidad trazabilidad) {
        requerido(trazabilidad.getIdPedido(), "idPedido");
        requerido(trazabilidad.getIdCliente(), "idCliente");
        requerido(trazabilidad.getCorreoCliente(), "correoCliente");
        requerido(trazabilidad.getIdRestaurante(), "idRestaurante");
        requerido(trazabilidad.getEstadoNuevo(), "estadoNuevo");
        trazabilidad.setFecha(LocalDateTime.now());
        trazabilidadRepositoryPort.guardar(trazabilidad);
    }

    @Override
    public List<Trazabilidad.Historial> consultarHistorial(Long idPedido, Long idCliente) {
        List<Trazabilidad> registros = trazabilidadRepositoryPort.listarPorPedido(idPedido);
        if (registros.isEmpty()) {
            throw new NoEncontradoException(String.format(MENSAJE_SIN_TRAZABILIDAD, idPedido));
        }
        if (!registros.get(0).getIdCliente().equals(idCliente)) {
            throw new ProhibidoException(MENSAJE_PEDIDO_AJENO);
        }
        List<Trazabilidad.Historial> historial = new ArrayList<>();
        for (int i = 0; i < registros.size(); i++) {
            Trazabilidad actual = registros.get(i);
            LocalDateTime fin = i + 1 < registros.size() ? registros.get(i + 1).getFecha() : finDelEstadoActual(actual);
            historial.add(new Trazabilidad.Historial(actual.getEstadoAnterior(), actual.getEstadoNuevo(),
                    actual.getFecha(), actual.getCorreoEmpleado(),
                    fin == null ? null : Duration.between(actual.getFecha(), fin).getSeconds()));
        }
        return historial;
    }

    @Override
    public Eficiencia.Reporte calcularEficienciaPedidos(Pagina.Solicitud solicitud) {
        List<Eficiencia.Tramo> tramos = tramosDePedidosEntregados(Estado.PENDIENTE, Estado.ENTREGADO);
        double promedio = promedio(tramos);
        List<Eficiencia.PedidoEntregado> pedidos = tramos.stream()
                .map(tramo -> new Eficiencia.PedidoEntregado(tramo.inicio().getIdPedido(), tramo.inicio().getFecha(),
                        tramo.fin().getFecha(), tramo.duracionSegundos(), tramo.duracionSegundos() > promedio))
                .toList();
        return new Eficiencia.Reporte(promedio, solicitud.paginar(pedidos));
    }

    @Override
    public Pagina<Eficiencia.Empleado> calcularRankingEmpleados(Pagina.Solicitud solicitud) {
        List<List<Eficiencia.Tramo>> tramosPorEmpleado = tramosDePedidosEntregados(Estado.EN_PREPARACION, Estado.LISTO)
                .stream()
                .collect(Collectors.groupingBy(tramo -> tramo.inicio().getIdEmpleado(), LinkedHashMap::new,
                        Collectors.toList()))
                .values().stream()
                .sorted(Comparator.comparingDouble(this::promedio))
                .toList();
        List<Eficiencia.Empleado> ranking = new ArrayList<>();
        for (List<Eficiencia.Tramo> tramos : tramosPorEmpleado) {
            Trazabilidad asignacion = tramos.get(0).inicio();
            ranking.add(new Eficiencia.Empleado(ranking.size() + 1, asignacion.getIdEmpleado(),
                    asignacion.getCorreoEmpleado(), promedio(tramos), tramos.size()));
        }
        return solicitud.paginar(ranking);
    }

    private List<Eficiencia.Tramo> tramosDePedidosEntregados(Estado desde, Estado hasta) {
        Long idRestaurante = plazoletaPort.obtenerIdRestauranteDelPropietario();
        List<Eficiencia.Tramo> tramos = new ArrayList<>();
        for (List<Trazabilidad> registros : trazabilidadRepositoryPort.listarPorRestaurante(idRestaurante).stream()
                .collect(Collectors.groupingBy(Trazabilidad::getIdPedido, LinkedHashMap::new, Collectors.toList()))
                .values()) {
            Optional<Trazabilidad> inicio = buscarEstado(registros, desde);
            Optional<Trazabilidad> fin = buscarEstado(registros, hasta);
            if (inicio.isPresent() && fin.isPresent() && buscarEstado(registros, Estado.ENTREGADO).isPresent()) {
                tramos.add(new Eficiencia.Tramo(inicio.get(), fin.get()));
            }
        }
        return tramos;
    }

    private Optional<Trazabilidad> buscarEstado(List<Trazabilidad> registros, Estado estado) {
        return registros.stream().filter(registro -> registro.getEstadoNuevo() == estado).findFirst();
    }

    private double promedio(List<Eficiencia.Tramo> tramos) {
        return tramos.stream().mapToLong(Eficiencia.Tramo::duracionSegundos).average().orElse(0);
    }

    private LocalDateTime finDelEstadoActual(Trazabilidad ultimo) {
        return ultimo.getEstadoNuevo().esFinal() ? null : LocalDateTime.now();
    }

    private void requerido(Object valor, String campo) {
        if (valor == null || valor instanceof String texto && texto.isBlank()) {
            throw new ValidacionException(String.format(MENSAJE_CAMPO_OBLIGATORIO, campo));
        }
    }
}
