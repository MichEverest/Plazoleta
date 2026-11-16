package com.mich.usuarios.infrastructure.config;

import com.mich.usuarios.application.port.output.SeguridadPort;
import com.mich.usuarios.application.port.output.UsuarioRepositoryPort;
import com.mich.usuarios.domain.model.Usuario;
import com.mich.usuarios.domain.model.Usuario.Rol;
import com.mich.usuarios.infrastructure.adapter.output.persistence.UsuarioPersistenceAdapter.RolJpaRepository;
import com.mich.usuarios.infrastructure.adapter.output.persistence.entity.RolEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Siembra los roles y los usuarios de prueba. El orden de la lista fija los ids en una base nueva
 * (1 a 4) y Plazoleta usa el id 2 (Cartethyia) como dueña del restaurante y el 4 (Mari) como su empleada:
 * no cambiar el orden.
 * Busca por número de documento para corregir nombre, correo y celular de bases creadas con versiones anteriores,
 * y quita el indicativo +57 de los celulares guardados antes de que el sistema pidiera solo los 10 dígitos.
 * El personal (administrador, propietaria y empleada) usa el celular de la empresa. Si seed.celular-para-clientes tiene
 * un número, se lo asigna a todos los CLIENTES en cada arranque, para que los SMS
 * de prueba (como el PIN) lleguen a un único celular verificado en Twilio.
 */
@Component
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final String CLAVE_INICIAL = "Clave123*";
    private static final LocalDate FECHA_NACIMIENTO_INICIAL = LocalDate.of(1995, 1, 1);
    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);
    private static final String INDICATIVO_COLOMBIA = "+57";
    private static final String CELULAR_EMPRESA = "3022249736";
    private static final String CELULAR_CLIENTE = "3023066544";
    private static final Pattern PATRON_CELULAR = Pattern.compile("^3\\d{9}$");

    private record Semilla(String nombre, String apellido, String correo, String documento, String celular, Rol rol) {
    }

    private static final List<Semilla> USUARIOS = List.of(
            new Semilla("Mich", "Everest", "mich@gmail.com", "1000000001", CELULAR_EMPRESA, Rol.ADMINISTRADOR),
            new Semilla("Cartethyia", "Plazoleta", "cartethyia@gmail.com", "1000000002", CELULAR_EMPRESA, Rol.PROPIETARIO),
            new Semilla("Jean", "Plazoleta", "jean@gmail.com", "1000000003", CELULAR_CLIENTE, Rol.CLIENTE),
            new Semilla("Mari", "Plazoleta", "mari@gmail.com", "1000000004", CELULAR_EMPRESA, Rol.EMPLEADO));

    private final RolJpaRepository rolJpaRepository;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final SeguridadPort seguridadPort;
    private final String celularParaClientes;

    public DataInitializer(RolJpaRepository rolJpaRepository, UsuarioRepositoryPort usuarioRepositoryPort,
                           SeguridadPort seguridadPort,
                           @Value("${seed.celular-para-clientes:}") String celularParaClientes) {
        this.rolJpaRepository = rolJpaRepository;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.seguridadPort = seguridadPort;
        this.celularParaClientes = celularParaClientes.trim();
    }

    @Override
    public void run(String... args) {
        for (Rol rol : Rol.values()) {
            if (rolJpaRepository.findByNombre(rol).isEmpty()) {
                rolJpaRepository.save(new RolEntity(rol));
            }
        }
        USUARIOS.forEach(this::sembrar);
        usuarioRepositoryPort.listar().stream()
                .filter(usuario -> usuario.getCelular().startsWith(INDICATIVO_COLOMBIA))
                .forEach(usuario -> {
                    usuario.setCelular(usuario.getCelular().substring(INDICATIVO_COLOMBIA.length()));
                    usuarioRepositoryPort.guardar(usuario);
                });
        asignarCelularAClientes();
    }

    private void asignarCelularAClientes() {
        if (celularParaClientes.isEmpty()) {
            return;
        }
        if (!PATRON_CELULAR.matcher(celularParaClientes).matches()) {
            LOGGER.warn("seed.celular-para-clientes={} no es un celular de 10 dígitos que empiece por 3; se ignora",
                    celularParaClientes);
            return;
        }
        List<Usuario> pendientes = usuarioRepositoryPort.listar().stream()
                .filter(usuario -> usuario.getRol() == Rol.CLIENTE)
                .filter(usuario -> !celularParaClientes.equals(usuario.getCelular()))
                .toList();
        pendientes.forEach(usuario -> {
            usuario.setCelular(celularParaClientes);
            usuarioRepositoryPort.guardar(usuario);
        });
        LOGGER.info("Todos los clientes tienen el celular {} ({} actualizados)", celularParaClientes, pendientes.size());
    }

    private void sembrar(Semilla semilla) {
        Optional<Usuario> existente = usuarioRepositoryPort.buscarPorNumeroDocumento(semilla.documento());
        if (existente.isPresent() && estaAlDia(existente.get(), semilla)) {
            return;
        }
        if (existente.isEmpty() && usuarioRepositoryPort.existeCorreo(semilla.correo())) {
            return;
        }
        Long id = existente.map(Usuario::getId).orElse(null);
        String clave = existente.map(Usuario::getClave).orElseGet(() -> seguridadPort.encriptarClave(CLAVE_INICIAL));
        usuarioRepositoryPort.guardar(new Usuario(id, semilla.nombre(), semilla.apellido(), semilla.documento(),
                semilla.celular(), FECHA_NACIMIENTO_INICIAL, semilla.correo(), clave, semilla.rol()));
    }

    private boolean estaAlDia(Usuario usuario, Semilla semilla) {
        return semilla.correo().equals(usuario.getCorreo()) && semilla.nombre().equals(usuario.getNombre())
                && semilla.celular().equals(usuario.getCelular());
    }
}
