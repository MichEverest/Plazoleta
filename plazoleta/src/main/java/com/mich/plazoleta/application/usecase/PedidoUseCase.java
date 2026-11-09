package com.mich.plazoleta.application.usecase;

import com.mich.plazoleta.application.port.output.PedidoRepositoryPort;
import com.mich.plazoleta.application.port.output.PlatoRepositoryPort;
import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import com.mich.plazoleta.application.port.output.ServiciosExternosPort;
import com.mich.plazoleta.application.service.PedidoService;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ConflictoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ProhibidoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Pedido;
import com.mich.plazoleta.domain.model.Pedido.Estado;
import com.mich.plazoleta.domain.model.Plato;
import com.mich.plazoleta.domain.model.Usuario;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PedidoUseCase implements PedidoService {

    private static final List<Estado> ESTADOS_EN_CURSO =
            List.of(Estado.PENDIENTE, Estado.EN_PREPARACION, Estado.LISTO);
    private static final Map<Estado, String> MENSAJES_NO_CANCELABLE = Map.of(
            Estado.EN_PREPARACION, "Lo sentimos, tu pedido ya está en preparación y no puede cancelarse",
            Estado.LISTO, "Lo sentimos, tu pedido ya está listo para entregar y no puede cancelarse",
            Estado.ENTREGADO, "Lo sentimos, tu pedido ya fue entregado y no puede cancelarse",
            Estado.CANCELADO, "Tu pedido ya se encuentra cancelado");
    private static final SecureRandom GENERADOR_PIN = new SecureRandom();
    private static final int LIMITE_PIN = 1_000_000;
    private static final String FORMATO_PIN = "%06d";

    private static final String MENSAJE_SIN_PLATOS = "El pedido debe tener al menos un plato";
    private static final String MENSAJE_CANTIDAD_INVALIDA = "La cantidad de cada plato debe ser mayor a 0";
    private static final String MENSAJE_PLATO_REPETIDO = "El plato con id %d está repetido en el pedido";
    private static final String MENSAJE_RESTAURANTE_NO_EXISTE = "No existe el restaurante con id %d";
    private static final String MENSAJE_PLATO_NO_EXISTE = "No existe el plato con id %d";
    private static final String MENSAJE_PLATO_OTRO_RESTAURANTE = "El plato con id %d no pertenece al restaurante del pedido";
    private static final String MENSAJE_PLATO_INACTIVO = "El plato con id %d no está disponible";
    private static final String MENSAJE_PEDIDO_EN_CURSO = "Ya tienes un pedido en curso; espera a que finalice para hacer otro";
    private static final String MENSAJE_ESTADO_INVALIDO = "Estado de pedido inválido: %s";
    private static final String MENSAJE_PEDIDO_NO_EXISTE = "No existe el pedido con id %d";
    private static final String MENSAJE_EMPLEADO_SIN_RESTAURANTE = "El empleado no está vinculado a ningún restaurante";
    private static final String MENSAJE_PEDIDO_OTRO_RESTAURANTE = "El pedido no pertenece al restaurante del empleado";
    private static final String MENSAJE_PEDIDO_AJENO = "El pedido no pertenece al cliente autenticado";
    private static final String MENSAJE_TRANSICION_INVALIDA = "El pedido está en estado %s y esta acción requiere el estado %s";
    private static final String MENSAJE_PIN_INCORRECTO = "El PIN no coincide con el del pedido";
    private static final String MENSAJE_CLIENTE_NO_EXISTE = "No existe el cliente con id %d";
    private static final String MENSAJE_SMS_PEDIDO_LISTO = "Tu pedido #%d está listo. Presenta este PIN para reclamarlo: %s";

    private final PedidoRepositoryPort pedidoRepositoryPort;
    private final PlatoRepositoryPort platoRepositoryPort;
    private final RestauranteRepositoryPort restauranteRepositoryPort;
    private final ServiciosExternosPort serviciosExternosPort;

    public PedidoUseCase(PedidoRepositoryPort pedidoRepositoryPort, PlatoRepositoryPort platoRepositoryPort,
                         RestauranteRepositoryPort restauranteRepositoryPort,
                         ServiciosExternosPort serviciosExternosPort) {
        this.pedidoRepositoryPort = pedidoRepositoryPort;
        this.platoRepositoryPort = platoRepositoryPort;
        this.restauranteRepositoryPort = restauranteRepositoryPort;
        this.serviciosExternosPort = serviciosExternosPort;
    }

    @Override
    public Pedido crear(Pedido pedido, Long idCliente, String correoCliente) {
        Validaciones.requerido(pedido.getIdRestaurante(), "idRestaurante");
        if (pedido.getPlatos() == null || pedido.getPlatos().isEmpty()) {
            throw new ValidacionException(MENSAJE_SIN_PLATOS);
        }
        if (restauranteRepositoryPort.buscarPorId(pedido.getIdRestaurante()).isEmpty()) {
            throw new NoEncontradoException(String.format(MENSAJE_RESTAURANTE_NO_EXISTE, pedido.getIdRestaurante()));
        }
        validarPlatos(pedido);
        if (pedidoRepositoryPort.existePedidoDeCliente(idCliente, ESTADOS_EN_CURSO)) {
            throw new ConflictoException(MENSAJE_PEDIDO_EN_CURSO);
        }
        pedido.setIdCliente(idCliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(Estado.PENDIENTE);
        Pedido creado = pedidoRepositoryPort.guardar(pedido);
        registrarCambio(creado, null, correoCliente, null, null);
        return creado;
    }

    @Override
    public Pagina<Pedido> listarDeCliente(Long idCliente, Pagina.Solicitud solicitud) {
        return pedidoRepositoryPort.listarPorCliente(idCliente, solicitud);
    }

    @Override
    public Pagina<Pedido> listarDeRestaurante(Long idEmpleado, String estado, Pagina.Solicitud solicitud) {
        Estado filtro = estado == null || estado.isBlank() ? null : convertirEstado(estado);
        return pedidoRepositoryPort.listarPorRestaurante(buscarRestauranteDeEmpleado(idEmpleado), filtro, solicitud);
    }

    @Override
    public Pedido asignar(Long idPedido, Long idEmpleado, String correoEmpleado) {
        Pedido pedido = buscarPedidoDelRestaurante(idPedido, idEmpleado);
        validarEstado(pedido, Estado.PENDIENTE);
        pedido.setIdEmpleado(idEmpleado);
        return cambiarEstado(pedido, Estado.EN_PREPARACION, buscarCliente(pedido).correo(), idEmpleado, correoEmpleado);
    }

    @Override
    public Pedido marcarListo(Long idPedido, Long idEmpleado, String correoEmpleado) {
        Pedido pedido = buscarPedidoDelRestaurante(idPedido, idEmpleado);
        validarEstado(pedido, Estado.EN_PREPARACION);
        pedido.setPin(String.format(FORMATO_PIN, GENERADOR_PIN.nextInt(LIMITE_PIN)));
        Usuario cliente = buscarCliente(pedido);
        Pedido listo = cambiarEstado(pedido, Estado.LISTO, cliente.correo(), idEmpleado, correoEmpleado);
        serviciosExternosPort.enviarSms(cliente.celular(), String.format(MENSAJE_SMS_PEDIDO_LISTO, pedido.getId(), pedido.getPin()));
        return listo;
    }

    @Override
    public Pedido entregar(Long idPedido, String pin, Long idEmpleado, String correoEmpleado) {
        Validaciones.requerido(pin, "pin");
        Pedido pedido = buscarPedidoDelRestaurante(idPedido, idEmpleado);
        validarEstado(pedido, Estado.LISTO);
        if (!pin.equals(pedido.getPin())) {
            throw new ValidacionException(MENSAJE_PIN_INCORRECTO);
        }
        return cambiarEstado(pedido, Estado.ENTREGADO, buscarCliente(pedido).correo(), idEmpleado, correoEmpleado);
    }

    @Override
    public Pedido cancelar(Long idPedido, Long idCliente, String correoCliente) {
        Pedido pedido = buscarPedido(idPedido);
        if (!pedido.getIdCliente().equals(idCliente)) {
            throw new ProhibidoException(MENSAJE_PEDIDO_AJENO);
        }
        if (pedido.getEstado() != Estado.PENDIENTE) {
            throw new ConflictoException(MENSAJES_NO_CANCELABLE.get(pedido.getEstado()));
        }
        return cambiarEstado(pedido, Estado.CANCELADO, correoCliente, null, null);
    }

    private void validarPlatos(Pedido pedido) {
        Set<Long> idsPlatos = new HashSet<>();
        for (Pedido.Detalle detalle : pedido.getPlatos()) {
            Validaciones.requerido(detalle.idPlato(), "idPlato");
            if (detalle.cantidad() == null || detalle.cantidad() <= 0) {
                throw new ValidacionException(MENSAJE_CANTIDAD_INVALIDA);
            }
            if (!idsPlatos.add(detalle.idPlato())) {
                throw new ValidacionException(String.format(MENSAJE_PLATO_REPETIDO, detalle.idPlato()));
            }
            validarPlatoDisponible(detalle.idPlato(), pedido.getIdRestaurante());
        }
    }

    private void validarPlatoDisponible(Long idPlato, Long idRestaurante) {
        Plato plato = platoRepositoryPort.buscarPorId(idPlato)
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_PLATO_NO_EXISTE, idPlato)));
        if (!plato.getIdRestaurante().equals(idRestaurante)) {
            throw new ValidacionException(String.format(MENSAJE_PLATO_OTRO_RESTAURANTE, idPlato));
        }
        if (!Boolean.TRUE.equals(plato.getActivo())) {
            throw new ValidacionException(String.format(MENSAJE_PLATO_INACTIVO, idPlato));
        }
    }

    private Estado convertirEstado(String estado) {
        return Arrays.stream(Estado.values())
                .filter(valor -> valor.name().equals(estado))
                .findFirst()
                .orElseThrow(() -> new ValidacionException(String.format(MENSAJE_ESTADO_INVALIDO, estado)));
    }

    private Long buscarRestauranteDeEmpleado(Long idEmpleado) {
        return restauranteRepositoryPort.buscarRestauranteDeEmpleado(idEmpleado)
                .orElseThrow(() -> new ProhibidoException(MENSAJE_EMPLEADO_SIN_RESTAURANTE));
    }

    private Pedido buscarPedido(Long idPedido) {
        return pedidoRepositoryPort.buscarPorId(idPedido)
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_PEDIDO_NO_EXISTE, idPedido)));
    }

    private Pedido buscarPedidoDelRestaurante(Long idPedido, Long idEmpleado) {
        Long idRestaurante = buscarRestauranteDeEmpleado(idEmpleado);
        Pedido pedido = buscarPedido(idPedido);
        if (!pedido.getIdRestaurante().equals(idRestaurante)) {
            throw new ProhibidoException(MENSAJE_PEDIDO_OTRO_RESTAURANTE);
        }
        return pedido;
    }

    private Usuario buscarCliente(Pedido pedido) {
        return serviciosExternosPort.buscarUsuario(pedido.getIdCliente())
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_CLIENTE_NO_EXISTE, pedido.getIdCliente())));
    }

    private void validarEstado(Pedido pedido, Estado requerido) {
        if (pedido.getEstado() != requerido) {
            throw new ConflictoException(String.format(MENSAJE_TRANSICION_INVALIDA, pedido.getEstado(), requerido));
        }
    }

    private Pedido cambiarEstado(Pedido pedido, Estado nuevo, String correoCliente,
                                 Long idEmpleado, String correoEmpleado) {
        Estado anterior = pedido.getEstado();
        pedido.setEstado(nuevo);
        Pedido actualizado = pedidoRepositoryPort.actualizar(pedido);
        registrarCambio(actualizado, anterior, correoCliente, idEmpleado, correoEmpleado);
        return actualizado;
    }

    private void registrarCambio(Pedido pedido, Estado anterior, String correoCliente,
                                 Long idEmpleado, String correoEmpleado) {
        serviciosExternosPort.registrarCambioEstado(new Pedido.CambioEstado(pedido.getId(), pedido.getIdCliente(),
                correoCliente, pedido.getIdRestaurante(), anterior, pedido.getEstado(), idEmpleado, correoEmpleado));
    }
}
