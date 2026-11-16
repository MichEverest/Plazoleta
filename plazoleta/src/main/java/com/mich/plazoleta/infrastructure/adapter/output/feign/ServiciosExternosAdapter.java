package com.mich.plazoleta.infrastructure.adapter.output.feign;

import com.mich.plazoleta.application.port.output.ServiciosExternosPort;
import com.mich.plazoleta.domain.model.Pedido;
import com.mich.plazoleta.domain.model.Usuario;
import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;

@Component
public class ServiciosExternosAdapter implements ServiciosExternosPort {

    @FeignClient(name = "usuarios", url = "${services.usuarios.url}")
    public interface UsuarioClient {

        record UsuarioResponse(Long id, String nombre, String correo, String celular, String rol) {
        }

        record EmpleadoRequest(String nombre, String apellido, String numeroDocumento, String celular, String correo,
                               String clave) {
        }

        record IdResponse(Long id) {
        }

        @GetMapping("/usuarios/{id}")
        UsuarioResponse consultarPorId(@PathVariable("id") Long id);

        @PostMapping("/usuarios/empleado")
        IdResponse crearEmpleado(@RequestBody EmpleadoRequest request);
    }

    @FeignClient(name = "mensajeria", url = "${services.mensajeria.url}")
    public interface MensajeriaClient {

        record PinRequest(String celular, String mensaje, String pin) {
        }

        record PinResponse(boolean verificacionExterna) {
        }

        record VerificacionRequest(String celular, String codigo) {
        }

        record VerificacionResponse(boolean valido) {
        }

        @PostMapping("/mensajes/pin")
        PinResponse enviarPin(@RequestBody PinRequest request);

        @PostMapping("/mensajes/pin/verificacion")
        VerificacionResponse verificarPin(@RequestBody VerificacionRequest request);
    }

    @FeignClient(name = "trazabilidad", url = "${services.trazabilidad.url}")
    public interface TrazabilidadClient {

        record CambioEstadoRequest(Long idPedido, Long idCliente, String correoCliente, Long idRestaurante,
                                   Pedido.Estado estadoAnterior, Pedido.Estado estadoNuevo, Long idEmpleado,
                                   String correoEmpleado, Boolean conSms) {
        }

        @PostMapping("/trazabilidad")
        void registrar(@RequestBody CambioEstadoRequest request);
    }

    private final UsuarioClient usuarioClient;
    private final MensajeriaClient mensajeriaClient;
    private final TrazabilidadClient trazabilidadClient;

    public ServiciosExternosAdapter(UsuarioClient usuarioClient, MensajeriaClient mensajeriaClient,
                                    TrazabilidadClient trazabilidadClient) {
        this.usuarioClient = usuarioClient;
        this.mensajeriaClient = mensajeriaClient;
        this.trazabilidadClient = trazabilidadClient;
    }

    @Override
    public Optional<Usuario> buscarUsuario(Long id) {
        try {
            UsuarioClient.UsuarioResponse usuario = usuarioClient.consultarPorId(id);
            return Optional.of(new Usuario(usuario.id(), usuario.nombre(), null, null, usuario.celular(),
                    usuario.correo(), null, Usuario.Rol.valueOf(usuario.rol())));
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        }
    }

    @Override
    public Long crearEmpleado(Usuario empleado) {
        return usuarioClient.crearEmpleado(new UsuarioClient.EmpleadoRequest(empleado.nombre(), empleado.apellido(),
                empleado.numeroDocumento(), empleado.celular(), empleado.correo(), empleado.clave())).id();
    }

    @Override
    public boolean enviarPin(String celular, String mensaje, String pin) {
        return mensajeriaClient.enviarPin(new MensajeriaClient.PinRequest(celular, mensaje, pin)).verificacionExterna();
    }

    @Override
    public boolean verificarPinExterno(String celular, String codigo) {
        return mensajeriaClient.verificarPin(new MensajeriaClient.VerificacionRequest(celular, codigo)).valido();
    }

    @Override
    public void registrarCambioEstado(Pedido.CambioEstado cambio) {
        trazabilidadClient.registrar(new TrazabilidadClient.CambioEstadoRequest(cambio.idPedido(), cambio.idCliente(),
                cambio.correoCliente(), cambio.idRestaurante(), cambio.estadoAnterior(), cambio.estadoNuevo(),
                cambio.idEmpleado(), cambio.correoEmpleado(), cambio.conSms()));
    }
}
