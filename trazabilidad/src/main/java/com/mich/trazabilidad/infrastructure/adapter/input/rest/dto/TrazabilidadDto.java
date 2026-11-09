package com.mich.trazabilidad.infrastructure.adapter.input.rest.dto;

import com.mich.trazabilidad.domain.model.Eficiencia;
import com.mich.trazabilidad.domain.model.Trazabilidad;
import com.mich.trazabilidad.domain.model.Trazabilidad.Estado;

import java.time.LocalDateTime;

public final class TrazabilidadDto {

    private TrazabilidadDto() {
    }

    public record Request(Long idPedido, Long idCliente, String correoCliente, Long idRestaurante,
                          Estado estadoAnterior, Estado estadoNuevo, Long idEmpleado, String correoEmpleado) {

        public Trazabilidad toDomain() {
            return new Trazabilidad(null, idPedido, idCliente, correoCliente, idRestaurante, null, estadoAnterior,
                    estadoNuevo, idEmpleado, correoEmpleado);
        }
    }

    public record Historial(Estado estadoAnterior, Estado estadoNuevo, LocalDateTime fecha, String correoEmpleado,
                            Long duracionSegundos) {

        public static Historial de(Trazabilidad.Historial historial) {
            return new Historial(historial.estadoAnterior(), historial.estadoNuevo(), historial.fecha(),
                    historial.correoEmpleado(), historial.duracionSegundos());
        }
    }

    public record PedidoEntregado(Long idPedido, LocalDateTime fechaInicio, LocalDateTime fechaEntrega,
                                  long duracionSegundos, boolean superaPromedio) {

        public static PedidoEntregado de(Eficiencia.PedidoEntregado pedido) {
            return new PedidoEntregado(pedido.idPedido(), pedido.fechaInicio(), pedido.fechaEntrega(),
                    pedido.duracionSegundos(), pedido.superaPromedio());
        }
    }

    public record ReporteEficiencia(double promedioSegundos, PaginaDto<PedidoEntregado> pedidos) {

        public static ReporteEficiencia de(Eficiencia.Reporte reporte) {
            return new ReporteEficiencia(reporte.promedioSegundos(),
                    PaginaDto.de(reporte.pedidos(), PedidoEntregado::de));
        }
    }

    public record RankingEmpleado(int posicion, Long idEmpleado, String correoEmpleado, double promedioSegundos,
                                  long pedidosAtendidos) {

        public static RankingEmpleado de(Eficiencia.Empleado empleado) {
            return new RankingEmpleado(empleado.posicion(), empleado.idEmpleado(), empleado.correoEmpleado(),
                    empleado.promedioSegundos(), empleado.pedidosAtendidos());
        }
    }
}
