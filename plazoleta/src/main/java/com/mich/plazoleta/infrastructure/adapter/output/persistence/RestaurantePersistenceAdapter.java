package com.mich.plazoleta.infrastructure.adapter.output.persistence;

import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.RestauranteEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RestaurantePersistenceAdapter implements RestauranteRepositoryPort {

    private static final Sort ORDEN_POR_NOMBRE = Sort.by("nombre");

    public interface RestauranteJpaRepository extends JpaRepository<RestauranteEntity, Long> {

        boolean existsByNit(String nit);

        boolean existsByIdPropietario(Long idPropietario);

        Optional<RestauranteEntity> findByIdPropietario(Long idPropietario);

        @Query("select r.id from RestauranteEntity r join r.empleados e where e = :idEmpleado")
        Optional<Long> buscarIdPorEmpleado(@Param("idEmpleado") Long idEmpleado);
    }

    private final RestauranteJpaRepository restauranteJpaRepository;

    public RestaurantePersistenceAdapter(RestauranteJpaRepository restauranteJpaRepository) {
        this.restauranteJpaRepository = restauranteJpaRepository;
    }

    @Override
    public Restaurante guardar(Restaurante restaurante) {
        return toDomain(restauranteJpaRepository.save(new RestauranteEntity(restaurante.id(), restaurante.nombre(),
                restaurante.nit(), restaurante.direccion(), restaurante.telefono(), restaurante.urlLogo(),
                restaurante.idPropietario())));
    }

    @Override
    public boolean existeNit(String nit) {
        return restauranteJpaRepository.existsByNit(nit);
    }

    @Override
    public boolean existeDePropietario(Long idPropietario) {
        return restauranteJpaRepository.existsByIdPropietario(idPropietario);
    }

    @Override
    public Optional<Restaurante> buscarPorId(Long id) {
        return restauranteJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Restaurante> buscarPorPropietario(Long idPropietario) {
        return restauranteJpaRepository.findByIdPropietario(idPropietario).map(this::toDomain);
    }

    @Override
    public Pagina<Restaurante> listar(Pagina.Solicitud solicitud) {
        return PaginacionJpa.pagina(
                restauranteJpaRepository.findAll(PaginacionJpa.solicitud(solicitud, ORDEN_POR_NOMBRE)), this::toDomain);
    }

    @Override
    public void vincularEmpleado(Long idRestaurante, Long idEmpleado) {
        RestauranteEntity restaurante = restauranteJpaRepository.getReferenceById(idRestaurante);
        restaurante.getEmpleados().add(idEmpleado);
        restauranteJpaRepository.save(restaurante);
    }

    @Override
    public Optional<Long> buscarRestauranteDeEmpleado(Long idEmpleado) {
        return restauranteJpaRepository.buscarIdPorEmpleado(idEmpleado);
    }

    private Restaurante toDomain(RestauranteEntity entity) {
        return new Restaurante(entity.getId(), entity.getNombre(), entity.getNit(), entity.getDireccion(),
                entity.getTelefono(), entity.getUrlLogo(), entity.getIdPropietario());
    }
}
