package com.mich.usuarios.application.usecase;

import com.mich.usuarios.application.port.output.SeguridadPort;
import com.mich.usuarios.application.port.output.UsuarioRepositoryPort;
import com.mich.usuarios.application.service.UsuarioService;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.ConflictoException;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.NoAutorizadoException;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.usuarios.domain.model.Usuario;
import com.mich.usuarios.domain.model.Usuario.Rol;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class UsuarioUseCase implements UsuarioService {

    private static final Pattern PATRON_CORREO = Pattern.compile("^[\\w.%+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PATRON_DOCUMENTO = Pattern.compile("^\\d+$");
    private static final Pattern PATRON_CELULAR = Pattern.compile("^\\+?\\d{1,12}$|^\\d{13}$");
    private static final int EDAD_MINIMA = 18;

    private static final String MENSAJE_CAMPO_OBLIGATORIO = "El campo %s es obligatorio";
    private static final String MENSAJE_CORREO_INVALIDO = "El correo no tiene una estructura válida";
    private static final String MENSAJE_DOCUMENTO_INVALIDO = "El número de documento debe ser numérico";
    private static final String MENSAJE_CELULAR_INVALIDO = "El celular debe ser numérico, de máximo 13 caracteres y puede iniciar con +";
    private static final String MENSAJE_MENOR_DE_EDAD = "El usuario debe ser mayor de edad";
    private static final String MENSAJE_CORREO_EXISTENTE = "Ya existe un usuario con ese correo";
    private static final String MENSAJE_DOCUMENTO_EXISTENTE = "Ya existe un usuario con ese número de documento";
    private static final String MENSAJE_USUARIO_NO_ENCONTRADO = "No existe el usuario con id %d";
    private static final String MENSAJE_CREDENCIALES_OBLIGATORIAS = "El correo y la clave son obligatorios";
    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Correo o clave incorrectos";

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final SeguridadPort seguridadPort;

    public UsuarioUseCase(UsuarioRepositoryPort usuarioRepositoryPort, SeguridadPort seguridadPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.seguridadPort = seguridadPort;
    }

    @Override
    public Usuario crearPropietario(Usuario usuario) {
        validarDatos(usuario);
        validarMayorDeEdad(usuario.getFechaNacimiento());
        return registrar(usuario, Rol.PROPIETARIO);
    }

    @Override
    public Usuario crearEmpleado(Usuario usuario) {
        validarDatos(usuario);
        return registrar(usuario, Rol.EMPLEADO);
    }

    @Override
    public Usuario crearCliente(Usuario usuario) {
        validarDatos(usuario);
        return registrar(usuario, Rol.CLIENTE);
    }

    @Override
    public Usuario consultarPorId(Long id) {
        return usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_USUARIO_NO_ENCONTRADO, id)));
    }

    @Override
    public Usuario.Sesion iniciarSesion(String correo, String clave) {
        if (esVacio(correo) || esVacio(clave)) {
            throw new ValidacionException(MENSAJE_CREDENCIALES_OBLIGATORIAS);
        }
        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correo)
                .filter(encontrado -> seguridadPort.claveCoincide(clave, encontrado.getClave()))
                .orElseThrow(() -> new NoAutorizadoException(MENSAJE_CREDENCIALES_INVALIDAS));
        return new Usuario.Sesion(seguridadPort.generarToken(usuario), usuario);
    }

    private void validarDatos(Usuario usuario) {
        requerido(usuario.getNombre(), "nombre");
        requerido(usuario.getApellido(), "apellido");
        requerido(usuario.getNumeroDocumento(), "numeroDocumento");
        requerido(usuario.getCelular(), "celular");
        requerido(usuario.getCorreo(), "correo");
        requerido(usuario.getClave(), "clave");
        validarFormato(usuario.getCorreo(), PATRON_CORREO, MENSAJE_CORREO_INVALIDO);
        validarFormato(usuario.getNumeroDocumento(), PATRON_DOCUMENTO, MENSAJE_DOCUMENTO_INVALIDO);
        validarFormato(usuario.getCelular(), PATRON_CELULAR, MENSAJE_CELULAR_INVALIDO);
    }

    private void requerido(String valor, String campo) {
        if (esVacio(valor)) {
            throw new ValidacionException(String.format(MENSAJE_CAMPO_OBLIGATORIO, campo));
        }
    }

    private void validarFormato(String valor, Pattern patron, String mensaje) {
        if (!patron.matcher(valor).matches()) {
            throw new ValidacionException(mensaje);
        }
    }

    private void validarMayorDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ValidacionException(String.format(MENSAJE_CAMPO_OBLIGATORIO, "fechaNacimiento"));
        }
        if (fechaNacimiento.plusYears(EDAD_MINIMA).isAfter(LocalDate.now())) {
            throw new ValidacionException(MENSAJE_MENOR_DE_EDAD);
        }
    }

    private Usuario registrar(Usuario usuario, Rol rol) {
        if (usuarioRepositoryPort.existeCorreo(usuario.getCorreo())) {
            throw new ConflictoException(MENSAJE_CORREO_EXISTENTE);
        }
        if (usuarioRepositoryPort.existeNumeroDocumento(usuario.getNumeroDocumento())) {
            throw new ConflictoException(MENSAJE_DOCUMENTO_EXISTENTE);
        }
        usuario.setRol(rol);
        usuario.setClave(seguridadPort.encriptarClave(usuario.getClave()));
        return usuarioRepositoryPort.guardar(usuario);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
