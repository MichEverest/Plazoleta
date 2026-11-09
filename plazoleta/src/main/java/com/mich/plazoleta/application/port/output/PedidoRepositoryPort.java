package com.mich.plazoleta.application.port.output;

import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Pedido;

import java.util.List;
import java.util.Optional;

public interface PedidoRepositoryPort {

    Pedido guardar(Pedido pedido);

    Pedido actualizar(Pedido pedido);

    Optional<Pedido> buscarPorId(Long id);

    boolean existePedidoDeCliente(Long idCliente, List<Pedido.Estado> estados);

    Pagina<Pedido> listarPorCliente(Long idCliente, Pagina.Solicitud solicitud);

    Pagina<Pedido> listarPorRestaurante(Long idRestaurante, Pedido.Estado estado, Pagina.Solicitud solicitud);
}
