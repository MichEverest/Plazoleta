package com.mich.usuarios.application.usecase;

import com.mich.usuarios.application.port.output.SeguridadPort;
import com.mich.usuarios.application.port.output.UsuarioRepositoryPort;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.ConflictoException;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.NoAutorizadoException;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.usuarios.domain.model.Usuario;
import com.mich.usuarios.domain.model.Usuario.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioUseCaseTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private SeguridadPort seguridadPort;

    @InjectMocks
    private UsuarioUseCase usuarioUseCase;

    @Test
    void crearPropietarioAsignaRolPropietarioYEncriptaClave() {
        Usuario usuario = usuarioValido();
        when(seguridadPort.encriptarClave("Clave123*")).thenReturn("claveEncriptada");
        when(usuarioRepositoryPort.guardar(usuario)).thenReturn(usuario);

        Usuario creado = usuarioUseCase.crearPropietario(usuario);

        assertEquals(Rol.PROPIETARIO, creado.getRol());
        assertEquals("claveEncriptada", creado.getClave());
    }

    @Test
    void crearPropietarioPermiteQuienCumpleDieciochoHoy() {
        Usuario usuario = usuarioValido();
        usuario.setFechaNacimiento(LocalDate.now().minusYears(18));
        when(usuarioRepositoryPort.guardar(usuario)).thenReturn(usuario);

        usuarioUseCase.crearPropietario(usuario);

        verify(usuarioRepositoryPort).guardar(usuario);
    }

    @Test
    void crearPropietarioFallaSiNombreEstaVacio() {
        Usuario usuario = usuarioValido();
        usuario.setNombre(" ");

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
        verify(usuarioRepositoryPort, never()).guardar(any());
    }

    @Test
    void crearPropietarioFallaSiClaveEsNula() {
        Usuario usuario = usuarioValido();
        usuario.setClave(null);

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
    }

    @Test
    void crearPropietarioFallaSiFechaNacimientoEsNula() {
        Usuario usuario = usuarioValido();
        usuario.setFechaNacimiento(null);

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
    }

    @Test
    void crearPropietarioFallaSiEsMenorDeEdad() {
        Usuario usuario = usuarioValido();
        usuario.setFechaNacimiento(LocalDate.now().minusYears(18).plusDays(1));

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
        verify(usuarioRepositoryPort, never()).guardar(any());
    }

    @Test
    void crearPropietarioFallaSiCorreoEsInvalido() {
        Usuario usuario = usuarioValido();
        usuario.setCorreo("correo-invalido");

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
    }

    @Test
    void crearPropietarioFallaSiDocumentoNoEsNumerico() {
        Usuario usuario = usuarioValido();
        usuario.setNumeroDocumento("10A0");

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
    }

    @Test
    void crearPropietarioFallaSiCelularSuperaTreceCaracteres() {
        Usuario usuario = usuarioValido();
        usuario.setCelular("+5730056983251");

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
    }

    @Test
    void crearPropietarioFallaSiCelularTieneCaracteresNoNumericos() {
        Usuario usuario = usuarioValido();
        usuario.setCelular("300-569-832");

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearPropietario(usuario));
    }

    @Test
    void crearPropietarioFallaSiCorreoYaExiste() {
        Usuario usuario = usuarioValido();
        when(usuarioRepositoryPort.existeCorreo(usuario.getCorreo())).thenReturn(true);

        assertThrows(ConflictoException.class, () -> usuarioUseCase.crearPropietario(usuario));
        verify(usuarioRepositoryPort, never()).guardar(any());
    }

    @Test
    void crearPropietarioFallaSiDocumentoYaExiste() {
        Usuario usuario = usuarioValido();
        when(usuarioRepositoryPort.existeNumeroDocumento(usuario.getNumeroDocumento())).thenReturn(true);

        assertThrows(ConflictoException.class, () -> usuarioUseCase.crearPropietario(usuario));
    }

    @Test
    void crearEmpleadoAsignaRolEmpleadoSinFechaNacimiento() {
        Usuario usuario = usuarioValido();
        usuario.setFechaNacimiento(null);
        when(usuarioRepositoryPort.guardar(usuario)).thenReturn(usuario);

        assertEquals(Rol.EMPLEADO, usuarioUseCase.crearEmpleado(usuario).getRol());
    }

    @Test
    void crearEmpleadoFallaSiCelularEsInvalido() {
        Usuario usuario = usuarioValido();
        usuario.setCelular("abc");

        assertThrows(ValidacionException.class, () -> usuarioUseCase.crearEmpleado(usuario));
    }

    @Test
    void crearClienteAsignaRolCliente() {
        Usuario usuario = usuarioValido();
        when(usuarioRepositoryPort.guardar(usuario)).thenReturn(usuario);

        assertEquals(Rol.CLIENTE, usuarioUseCase.crearCliente(usuario).getRol());
    }

    @Test
    void crearClienteFallaSiCorreoYaExiste() {
        Usuario usuario = usuarioValido();
        when(usuarioRepositoryPort.existeCorreo(usuario.getCorreo())).thenReturn(true);

        assertThrows(ConflictoException.class, () -> usuarioUseCase.crearCliente(usuario));
    }

    @Test
    void consultarPorIdRetornaUsuarioExistente() {
        Usuario usuario = usuarioValido();
        when(usuarioRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(usuario));

        assertEquals(usuario, usuarioUseCase.consultarPorId(1L));
    }

    @Test
    void consultarPorIdFallaSiNoExiste() {
        when(usuarioRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> usuarioUseCase.consultarPorId(99L));
    }

    @Test
    void iniciarSesionRetornaTokenYUsuario() {
        Usuario usuario = usuarioValido();
        when(usuarioRepositoryPort.buscarPorCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(seguridadPort.claveCoincide("Clave123*", "Clave123*")).thenReturn(true);
        when(seguridadPort.generarToken(usuario)).thenReturn("token");

        Usuario.Sesion sesion = usuarioUseCase.iniciarSesion("ana@correo.com", "Clave123*");

        assertEquals("token", sesion.token());
        assertEquals(usuario, sesion.usuario());
    }

    @Test
    void iniciarSesionFallaSiFaltaCorreo() {
        assertThrows(ValidacionException.class, () -> usuarioUseCase.iniciarSesion("", "Clave123*"));
    }

    @Test
    void iniciarSesionFallaSiUsuarioNoExiste() {
        when(usuarioRepositoryPort.buscarPorCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThrows(NoAutorizadoException.class, () -> usuarioUseCase.iniciarSesion("nadie@correo.com", "Clave123*"));
    }

    @Test
    void iniciarSesionFallaSiClaveEsIncorrecta() {
        when(usuarioRepositoryPort.buscarPorCorreo("ana@correo.com")).thenReturn(Optional.of(usuarioValido()));
        when(seguridadPort.claveCoincide("Otra123*", "Clave123*")).thenReturn(false);

        assertThrows(NoAutorizadoException.class, () -> usuarioUseCase.iniciarSesion("ana@correo.com", "Otra123*"));
        verify(seguridadPort, never()).generarToken(any());
    }

    private Usuario usuarioValido() {
        return new Usuario(1L, "Ana", "Gomez", "1234567890", "+573005698325", LocalDate.of(1990, 5, 20),
                "ana@correo.com", "Clave123*", null);
    }
}
