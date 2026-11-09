package com.mich.plazoleta.application.service;

import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Pedido;

public interface PedidoService {

    Pedido crear(Pedido pedido, Long idCliente, String correoCliente);

    Pagina<Pedido> listarDeCliente(Long idCliente, Pagina.Solicitud solicitud);

    Pagina<Pedido> listarDeRestaurante(Long idEmpleado, String estado, Pagina.Solicitud solicitud);

    Pedido asignar(Long idPedido, Long idEmpleado, String correoEmpleado);

    Pedido marcarListo(Long idPedido, Long idEmpleado, String correoEmpleado);

    Pedido entregar(Long idPedido, String pin, Long idEmpleado, String correoEmpleado);

    Pedido cancelar(Long idPedido, Long idCliente, String correoCliente);
}
