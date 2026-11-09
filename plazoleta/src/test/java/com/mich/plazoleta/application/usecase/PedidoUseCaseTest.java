package com.mich.plazoleta.application.usecase;

import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ConflictoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ProhibidoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Pedido;
import com.mich.plazoleta.domain.model.Pedido.Estado;
import com.mich.plazoleta.domain.model.Plato;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.domain.model.Usuario;
import com.mich.plazoleta.application.port.output.PedidoRepositoryPort;
import com.mich.plazoleta.application.port.output.PlatoRepositoryPort;
import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import com.mich.plazoleta.application.port.output.ServiciosExternosPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoUseCaseTest {

    private static final Long ID_CLIENTE = 3L;
    private static final Long ID_EMPLEADO = 7L;
    private static final Long ID_RESTAURANTE = 1L;
    private static final String CORREO_CLIENTE = "jean@plazoleta.com";
    private static final String CORREO_EMPLEADO = "empleado@plazoleta.com";
    private static final String CELULAR_CLIENTE = "+573000000003";

    @Mock
    private PedidoRepositoryPort pedidoRepositoryPort;

    @Mock
    private PlatoRepositoryPort platoRepositoryPort;

    @Mock
    private RestauranteRepositoryPort restauranteRepositoryPort;

    @Mock
    private ServiciosExternosPort serviciosExternosPort;

    @InjectMocks
    private PedidoUseCase pedidoUseCase;

    @Test
    void crearPedidoQuedaPendienteYRegistraTrazabilidad() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 2));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(plato(ID_RESTAURANTE, true)));
        when(pedidoRepositoryPort.guardar(pedido)).thenReturn(pedido);

        Pedido creado = pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE);

        assertEquals(Estado.PENDIENTE, creado.getEstado());
        assertEquals(ID_CLIENTE, creado.getIdCliente());
        verify(serviciosExternosPort).registrarCambioEstado(new Pedido.CambioEstado(null, ID_CLIENTE, CORREO_CLIENTE,
                ID_RESTAURANTE, null, Estado.PENDIENTE, null, null));
    }

    @Test
    void crearPedidoFallaSinPlatos() {
        Pedido pedido = nuevoPedido();

        assertThrows(ValidacionException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void crearPedidoFallaSiRestauranteNoExiste() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 1));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void crearPedidoFallaSiCantidadNoEsPositiva() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 0));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.of(restaurante()));

        assertThrows(ValidacionException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void crearPedidoFallaSiPlatoEstaRepetido() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 1), new Pedido.Detalle(10L, null, 2));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(plato(ID_RESTAURANTE, true)));

        assertThrows(ValidacionException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void crearPedidoFallaSiPlatoEsDeOtroRestaurante() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 1));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(plato(99L, true)));

        assertThrows(ValidacionException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void crearPedidoFallaSiPlatoEstaInactivo() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 1));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(plato(ID_RESTAURANTE, false)));

        assertThrows(ValidacionException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void crearPedidoFallaSiPlatoNoExiste() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 1));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.buscarPorId(10L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void crearPedidoFallaSiClienteTienePedidoEnCurso() {
        Pedido pedido = nuevoPedido(new Pedido.Detalle(10L, null, 1));
        when(restauranteRepositoryPort.buscarPorId(ID_RESTAURANTE)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(plato(ID_RESTAURANTE, true)));
        when(pedidoRepositoryPort.existePedidoDeCliente(eq(ID_CLIENTE), anyList())).thenReturn(true);

        assertThrows(ConflictoException.class, () -> pedidoUseCase.crear(pedido, ID_CLIENTE, CORREO_CLIENTE));
        verify(pedidoRepositoryPort, never()).guardar(any());
    }

    @Test
    void listarPedidosClienteDelegaEnPersistencia() {
        Pagina.Solicitud paginacion = new Pagina.Solicitud(0, 5);
        Pagina<Pedido> pagina = new Pagina<>(List.of(), 0, 5, 0, 0);
        when(pedidoRepositoryPort.listarPorCliente(ID_CLIENTE, paginacion)).thenReturn(pagina);

        assertEquals(pagina, pedidoUseCase.listarDeCliente(ID_CLIENTE, paginacion));
    }

    @Test
    void listarPedidosRestauranteFiltraPorEstadoDelRestauranteDelEmpleado() {
        Pagina.Solicitud paginacion = new Pagina.Solicitud(0, 5);
        Pagina<Pedido> pagina = new Pagina<>(List.of(), 0, 5, 0, 0);
        when(restauranteRepositoryPort.buscarRestauranteDeEmpleado(ID_EMPLEADO)).thenReturn(Optional.of(ID_RESTAURANTE));
        when(pedidoRepositoryPort.listarPorRestaurante(ID_RESTAURANTE, Estado.PENDIENTE, paginacion)).thenReturn(pagina);

        assertEquals(pagina, pedidoUseCase.listarDeRestaurante(ID_EMPLEADO, "PENDIENTE", paginacion));
    }

    @Test
    void listarPedidosRestauranteFallaConEstadoInvalido() {
        assertThrows(ValidacionException.class,
                () -> pedidoUseCase.listarDeRestaurante(ID_EMPLEADO, "PERDIDO", new Pagina.Solicitud(0, 5)));
    }

    @Test
    void listarPedidosRestauranteFallaSiEmpleadoNoTieneRestaurante() {
        when(restauranteRepositoryPort.buscarRestauranteDeEmpleado(ID_EMPLEADO)).thenReturn(Optional.empty());

        assertThrows(ProhibidoException.class,
                () -> pedidoUseCase.listarDeRestaurante(ID_EMPLEADO, null, new Pagina.Solicitud(0, 5)));
    }

    @Test
    void asignarPedidoGuardaEmpleadoYPasaAEnPreparacion() {
        Pedido pedido = pedidoExistente(Estado.PENDIENTE);
        prepararPedidoDelRestaurante(pedido);
        when(serviciosExternosPort.buscarUsuario(ID_CLIENTE)).thenReturn(Optional.of(cliente()));
        when(pedidoRepositoryPort.actualizar(pedido)).thenReturn(pedido);

        Pedido asignado = pedidoUseCase.asignar(20L, ID_EMPLEADO, CORREO_EMPLEADO);

        assertEquals(Estado.EN_PREPARACION, asignado.getEstado());
        assertEquals(ID_EMPLEADO, asignado.getIdEmpleado());
        verify(serviciosExternosPort).registrarCambioEstado(new Pedido.CambioEstado(20L, ID_CLIENTE, CORREO_CLIENTE,
                ID_RESTAURANTE, Estado.PENDIENTE, Estado.EN_PREPARACION, ID_EMPLEADO, CORREO_EMPLEADO));
    }

    @Test
    void asignarPedidoFallaSiNoEstaPendiente() {
        prepararPedidoDelRestaurante(pedidoExistente(Estado.EN_PREPARACION));

        assertThrows(ConflictoException.class, () -> pedidoUseCase.asignar(20L, ID_EMPLEADO, CORREO_EMPLEADO));
        verify(pedidoRepositoryPort, never()).actualizar(any());
    }

    @Test
    void asignarPedidoFallaSiEsDeOtroRestaurante() {
        Pedido pedido = pedidoExistente(Estado.PENDIENTE);
        pedido.setIdRestaurante(99L);
        prepararPedidoDelRestaurante(pedido);

        assertThrows(ProhibidoException.class, () -> pedidoUseCase.asignar(20L, ID_EMPLEADO, CORREO_EMPLEADO));
    }

    @Test
    void asignarPedidoFallaSiNoExiste() {
        when(restauranteRepositoryPort.buscarRestauranteDeEmpleado(ID_EMPLEADO)).thenReturn(Optional.of(ID_RESTAURANTE));
        when(pedidoRepositoryPort.buscarPorId(20L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> pedidoUseCase.asignar(20L, ID_EMPLEADO, CORREO_EMPLEADO));
    }

    @Test
    void marcarPedidoListoGeneraPinYEnviaSms() {
        Pedido pedido = pedidoExistente(Estado.EN_PREPARACION);
        prepararPedidoDelRestaurante(pedido);
        when(serviciosExternosPort.buscarUsuario(ID_CLIENTE)).thenReturn(Optional.of(cliente()));
        when(pedidoRepositoryPort.actualizar(pedido)).thenReturn(pedido);

        Pedido listo = pedidoUseCase.marcarListo(20L, ID_EMPLEADO, CORREO_EMPLEADO);

        assertEquals(Estado.LISTO, listo.getEstado());
        assertTrue(listo.getPin().matches("\\d{6}"));
        verify(serviciosExternosPort).enviarSms(eq(CELULAR_CLIENTE), anyString());
    }

    @Test
    void marcarPedidoListoFallaSiNoEstaEnPreparacion() {
        prepararPedidoDelRestaurante(pedidoExistente(Estado.PENDIENTE));

        assertThrows(ConflictoException.class, () -> pedidoUseCase.marcarListo(20L, ID_EMPLEADO, CORREO_EMPLEADO));
        verify(serviciosExternosPort, never()).enviarSms(anyString(), anyString());
    }

    @Test
    void entregarPedidoConPinCorrectoQuedaEntregado() {
        Pedido pedido = pedidoExistente(Estado.LISTO);
        pedido.setPin("123456");
        prepararPedidoDelRestaurante(pedido);
        when(serviciosExternosPort.buscarUsuario(ID_CLIENTE)).thenReturn(Optional.of(cliente()));
        when(pedidoRepositoryPort.actualizar(pedido)).thenReturn(pedido);

        assertEquals(Estado.ENTREGADO,
                pedidoUseCase.entregar(20L, "123456", ID_EMPLEADO, CORREO_EMPLEADO).getEstado());
    }

    @Test
    void entregarPedidoFallaConPinIncorrecto() {
        Pedido pedido = pedidoExistente(Estado.LISTO);
        pedido.setPin("123456");
        prepararPedidoDelRestaurante(pedido);

        assertThrows(ValidacionException.class,
                () -> pedidoUseCase.entregar(20L, "000000", ID_EMPLEADO, CORREO_EMPLEADO));
        verify(pedidoRepositoryPort, never()).actualizar(any());
    }

    @Test
    void entregarPedidoFallaSinPin() {
        assertThrows(ValidacionException.class, () -> pedidoUseCase.entregar(20L, " ", ID_EMPLEADO, CORREO_EMPLEADO));
    }

    @Test
    void entregarPedidoFallaSiYaFueEntregado() {
        prepararPedidoDelRestaurante(pedidoExistente(Estado.ENTREGADO));

        assertThrows(ConflictoException.class,
                () -> pedidoUseCase.entregar(20L, "123456", ID_EMPLEADO, CORREO_EMPLEADO));
    }

    @Test
    void cancelarPedidoPendienteQuedaCancelado() {
        Pedido pedido = pedidoExistente(Estado.PENDIENTE);
        when(pedidoRepositoryPort.buscarPorId(20L)).thenReturn(Optional.of(pedido));
        when(pedidoRepositoryPort.actualizar(pedido)).thenReturn(pedido);

        assertEquals(Estado.CANCELADO, pedidoUseCase.cancelar(20L, ID_CLIENTE, CORREO_CLIENTE).getEstado());
        verify(serviciosExternosPort).registrarCambioEstado(new Pedido.CambioEstado(20L, ID_CLIENTE, CORREO_CLIENTE,
                ID_RESTAURANTE, Estado.PENDIENTE, Estado.CANCELADO, null, null));
    }

    @Test
    void cancelarPedidoEnPreparacionFallaConMensajeEspecifico() {
        when(pedidoRepositoryPort.buscarPorId(20L)).thenReturn(Optional.of(pedidoExistente(Estado.EN_PREPARACION)));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> pedidoUseCase.cancelar(20L, ID_CLIENTE, CORREO_CLIENTE));
        assertEquals("Lo sentimos, tu pedido ya está en preparación y no puede cancelarse", ex.getMessage());
    }

    @Test
    void cancelarPedidoListoFalla() {
        when(pedidoRepositoryPort.buscarPorId(20L)).thenReturn(Optional.of(pedidoExistente(Estado.LISTO)));

        assertThrows(ConflictoException.class, () -> pedidoUseCase.cancelar(20L, ID_CLIENTE, CORREO_CLIENTE));
    }

    @Test
    void cancelarPedidoFallaSiEsDeOtroCliente() {
        when(pedidoRepositoryPort.buscarPorId(20L)).thenReturn(Optional.of(pedidoExistente(Estado.PENDIENTE)));

        assertThrows(ProhibidoException.class, () -> pedidoUseCase.cancelar(20L, 99L, "otro@plazoleta.com"));
        verify(pedidoRepositoryPort, never()).actualizar(any());
    }

    private void prepararPedidoDelRestaurante(Pedido pedido) {
        when(restauranteRepositoryPort.buscarRestauranteDeEmpleado(ID_EMPLEADO)).thenReturn(Optional.of(ID_RESTAURANTE));
        when(pedidoRepositoryPort.buscarPorId(20L)).thenReturn(Optional.of(pedido));
    }

    private Pedido nuevoPedido(Pedido.Detalle... platos) {
        Pedido pedido = new Pedido();
        pedido.setIdRestaurante(ID_RESTAURANTE);
        pedido.setPlatos(List.of(platos));
        return pedido;
    }

    private Pedido pedidoExistente(Estado estado) {
        Pedido pedido = new Pedido();
        pedido.setId(20L);
        pedido.setIdCliente(ID_CLIENTE);
        pedido.setIdRestaurante(ID_RESTAURANTE);
        pedido.setEstado(estado);
        return pedido;
    }

    private Restaurante restaurante() {
        return new Restaurante(ID_RESTAURANTE, "Sabores", "900123456", "Calle 10", "+573001234567", "logo", 2L);
    }

    private Plato plato(Long idRestaurante, boolean activo) {
        Plato plato = new Plato();
        plato.setId(10L);
        plato.setIdRestaurante(idRestaurante);
        plato.setActivo(activo);
        return plato;
    }

    private Usuario cliente() {
        return new Usuario(ID_CLIENTE, "Jean", "Plazoleta", "1000000003", CELULAR_CLIENTE, CORREO_CLIENTE, null,
                Usuario.Rol.CLIENTE);
    }
}
