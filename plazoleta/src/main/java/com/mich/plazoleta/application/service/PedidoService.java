package com.mich.plazoleta.application.service;

import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Pedido;

public interface PedidoService {

    Pedido crear(Pedido pedido, Long idCliente, String correoCliente);

    Pagina<Pedido> listarDeCliente(Long idCliente, Pagina.Solicitud solicitud);

    Pagina<Pedido> listarDeRestaurante(Long idEmpleado, String estado, Pagina.Solicitud solicitud);

    Pedido asignar(Long idPedido, Long idEmpleado, String correoEmpleado);

    /**
     * Marca el pedido listo. Con sinSms=false genera el PIN y lo envía por SMS; con sinSms=true es el atajo sin SMS:
     * no hay PIN y la entrega no lo pide. Queda registrado en el pedido y en la trazabilidad.
     */
    Pedido marcarListo(Long idPedido, Long idEmpleado, String correoEmpleado, boolean sinSms);

    Pedido reenviarPin(Long idPedido, Long idEmpleado);

    Pedido entregar(Long idPedido, String pin, Long idEmpleado, String correoEmpleado);

    Pedido cancelar(Long idPedido, Long idCliente, String correoCliente);
}
