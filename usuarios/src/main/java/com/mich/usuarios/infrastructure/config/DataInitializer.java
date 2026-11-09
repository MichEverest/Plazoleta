package com.mich.usuarios.infrastructure.config;

import com.mich.usuarios.application.port.output.SeguridadPort;
import com.mich.usuarios.application.port.output.UsuarioRepositoryPort;
import com.mich.usuarios.domain.model.Usuario;
import com.mich.usuarios.domain.model.Usuario.Rol;
import com.mich.usuarios.infrastructure.adapter.output.persistence.UsuarioPersistenceAdapter.RolJpaRepository;
import com.mich.usuarios.infrastructure.adapter.output.persistence.entity.RolEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final String CLAVE_INICIAL = "Clave123*";
    private static final LocalDate FECHA_NACIMIENTO_INICIAL = LocalDate.of(1995, 1, 1);

    private final RolJpaRepository rolJpaRepository;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final SeguridadPort seguridadPort;

    public DataInitializer(RolJpaRepository rolJpaRepository, UsuarioRepositoryPort usuarioRepositoryPort,
                           SeguridadPort seguridadPort) {
        this.rolJpaRepository = rolJpaRepository;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.seguridadPort = seguridadPort;
    }

    @Override
    public void run(String... args) {
        for (Rol rol : Rol.values()) {
            if (rolJpaRepository.findByNombre(rol).isEmpty()) {
                rolJpaRepository.save(new RolEntity(rol));
            }
        }
        crearUsuario("Mich", "Everest", "mich@plazoleta.com", "1000000001", "+573000000001", Rol.ADMINISTRADOR);
        crearUsuario("Cartheyia", "Plazoleta", "cartheyia@plazoleta.com", "1000000002", "+573000000002", Rol.PROPIETARIO);
        crearUsuario("Jean", "Plazoleta", "jean@plazoleta.com", "1000000003", "+573000000003", Rol.CLIENTE);
    }

    private void crearUsuario(String nombre, String apellido, String correo, String documento, String celular, Rol rol) {
        if (usuarioRepositoryPort.existeCorreo(correo)) {
            return;
        }
        usuarioRepositoryPort.guardar(new Usuario(null, nombre, apellido, documento, celular, FECHA_NACIMIENTO_INICIAL,
                correo, seguridadPort.encriptarClave(CLAVE_INICIAL), rol));
    }
}
