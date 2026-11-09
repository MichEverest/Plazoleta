package com.mich.trazabilidad.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;

public final class Eficiencia {

    private Eficiencia() {
    }

    public record Tramo(Trazabilidad inicio, Trazabilidad fin) {

        public long duracionSegundos() {
            return Duration.between(inicio.getFecha(), fin.getFecha()).getSeconds();
        }
    }

    public record PedidoEntregado(Long idPedido, LocalDateTime fechaInicio, LocalDateTime fechaEntrega,
                                  long duracionSegundos, boolean superaPromedio) {
    }

    public record Reporte(double promedioSegundos, Pagina<PedidoEntregado> pedidos) {
    }

    public record Empleado(int posicion, Long idEmpleado, String correoEmpleado, double promedioSegundos,
                           long pedidosAtendidos) {
    }
}
