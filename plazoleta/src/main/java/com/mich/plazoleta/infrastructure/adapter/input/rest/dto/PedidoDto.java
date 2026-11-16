package com.mich.plazoleta.infrastructure.adapter.input.rest.dto;

import com.mich.plazoleta.domain.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;

public final class PedidoDto {

    private PedidoDto() {
    }

    public record Request(Long idRestaurante, List<DetalleRequest> platos) {

        public Pedido toDomain() {
            List<Pedido.Detalle> detalles = platos == null ? null : platos.stream()
                    .map(plato -> new Pedido.Detalle(plato.idPlato(), null, plato.cantidad()))
                    .toList();
            return new Pedido(null, null, null, null, null, idRestaurante, null, detalles);
        }
    }

    public record DetalleRequest(Long idPlato, Integer cantidad) {
    }

    /**
     * notificacion: cómo se avisó al cliente que el pedido está listo, "SMS" o "SIN_SMS" (atajo); null antes de LISTO.
     */
    public record Response(Long id, Long idCliente, LocalDateTime fecha, String estado, Long idEmpleado,
                           Long idRestaurante, String notificacion, List<DetalleResponse> platos) {

        public static Response de(Pedido pedido) {
            boolean avisado = pedido.getEstado() == Pedido.Estado.LISTO || pedido.getEstado() == Pedido.Estado.ENTREGADO;
            String notificacion = avisado ? (pedido.isSinSms() ? "SIN_SMS" : "SMS") : null;
            return new Response(pedido.getId(), pedido.getIdCliente(), pedido.getFecha(), pedido.getEstado().name(),
                    pedido.getIdEmpleado(), pedido.getIdRestaurante(), notificacion, pedido.getPlatos().stream()
                    .map(detalle -> new DetalleResponse(detalle.idPlato(), detalle.nombrePlato(), detalle.cantidad()))
                    .toList());
        }
    }

    public record DetalleResponse(Long idPlato, String nombre, Integer cantidad) {
    }

    public record Entrega(String pin) {
    }
}
