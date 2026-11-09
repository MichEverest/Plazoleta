package com.mich.usuarios.infrastructure.adapter.output.persistence;

import com.mich.usuarios.application.port.output.UsuarioRepositoryPort;
import com.mich.usuarios.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.usuarios.domain.model.Usuario;
import com.mich.usuarios.domain.model.Usuario.Rol;
import com.mich.usuarios.infrastructure.adapter.output.persistence.entity.RolEntity;
import com.mich.usuarios.infrastructure.adapter.output.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private static final String MENSAJE_ROL_NO_ENCONTRADO = "No existe el rol %s";

    public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

        boolean existsByCorreo(String correo);

        boolean existsByNumeroDocumento(String numeroDocumento);

        Optional<UsuarioEntity> findByCorreo(String correo);
    }

    public interface RolJpaRepository extends JpaRepository<RolEntity, Long> {

        Optional<RolEntity> findByNombre(Rol nombre);
    }

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final RolJpaRepository rolJpaRepository;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository usuarioJpaRepository, RolJpaRepository rolJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.rolJpaRepository = rolJpaRepository;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return toDomain(usuarioJpaRepository.save(toEntity(usuario)));
    }

    @Override
    public boolean existeCorreo(String correo) {
        return usuarioJpaRepository.existsByCorreo(correo);
    }

    @Override
    public boolean existeNumeroDocumento(String numeroDocumento) {
        return usuarioJpaRepository.existsByNumeroDocumento(numeroDocumento);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioJpaRepository.findByCorreo(correo).map(this::toDomain);
    }

    private UsuarioEntity toEntity(Usuario usuario) {
        RolEntity rol = rolJpaRepository.findByNombre(usuario.getRol())
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_ROL_NO_ENCONTRADO, usuario.getRol())));
        return new UsuarioEntity(usuario.getId(), usuario.getNombre(), usuario.getApellido(),
                usuario.getNumeroDocumento(), usuario.getCelular(), usuario.getFechaNacimiento(),
                usuario.getCorreo(), usuario.getClave(), rol);
    }

    private Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(entity.getId(), entity.getNombre(), entity.getApellido(), entity.getNumeroDocumento(),
                entity.getCelular(), entity.getFechaNacimiento(), entity.getCorreo(), entity.getClave(),
                entity.getRol().getNombre());
    }
}
