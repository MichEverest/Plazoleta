package com.mich.plazoleta.infrastructure.adapter.output.persistence;

import com.mich.plazoleta.application.port.output.PlatoRepositoryPort;
import com.mich.plazoleta.domain.model.Categoria;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Plato;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.RestaurantePersistenceAdapter.RestauranteJpaRepository;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.CategoriaEntity;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.PlatoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PlatoPersistenceAdapter implements PlatoRepositoryPort {

    private static final Sort ORDEN_POR_NOMBRE = Sort.by("nombre");
    private static final Sort ORDEN_POR_ID = Sort.by("id");

    public interface PlatoJpaRepository extends JpaRepository<PlatoEntity, Long> {

        @Query("""
                select p from PlatoEntity p
                where p.restaurante.id = :idRestaurante
                  and (:idCategoria is null or p.categoria.id = :idCategoria)
                  and (:soloActivos = false or p.activo = true)
                """)
        Page<PlatoEntity> buscarPorRestaurante(@Param("idRestaurante") Long idRestaurante,
                                               @Param("idCategoria") Long idCategoria,
                                               @Param("soloActivos") boolean soloActivos,
                                               Pageable pageable);
    }

    public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

        boolean existsByNombre(String nombre);
    }

    private final PlatoJpaRepository platoJpaRepository;
    private final CategoriaJpaRepository categoriaJpaRepository;
    private final RestauranteJpaRepository restauranteJpaRepository;

    public PlatoPersistenceAdapter(PlatoJpaRepository platoJpaRepository, CategoriaJpaRepository categoriaJpaRepository,
                                   RestauranteJpaRepository restauranteJpaRepository) {
        this.platoJpaRepository = platoJpaRepository;
        this.categoriaJpaRepository = categoriaJpaRepository;
        this.restauranteJpaRepository = restauranteJpaRepository;
    }

    @Override
    public Plato guardar(Plato plato) {
        return toDomain(platoJpaRepository.save(new PlatoEntity(plato.getId(), plato.getNombre(),
                categoriaJpaRepository.getReferenceById(plato.getIdCategoria()), plato.getDescripcion(),
                plato.getPrecio(), plato.getUrlImagen(), plato.getActivo(),
                restauranteJpaRepository.getReferenceById(plato.getIdRestaurante()))));
    }

    @Override
    public Optional<Plato> buscarPorId(Long id) {
        return platoJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Pagina<Plato> listarPorRestaurante(Long idRestaurante, Long idCategoria, boolean soloActivos,
                                              Pagina.Solicitud solicitud) {
        return PaginacionJpa.pagina(platoJpaRepository.buscarPorRestaurante(idRestaurante, idCategoria, soloActivos,
                PaginacionJpa.solicitud(solicitud, ORDEN_POR_NOMBRE)), this::toDomain);
    }

    @Override
    public List<Categoria> listarCategorias() {
        return categoriaJpaRepository.findAll(ORDEN_POR_ID).stream()
                .map(categoria -> new Categoria(categoria.getId(), categoria.getNombre(), categoria.getDescripcion()))
                .toList();
    }

    @Override
    public boolean existeCategoria(Long id) {
        return categoriaJpaRepository.existsById(id);
    }

    private Plato toDomain(PlatoEntity entity) {
        return new Plato(entity.getId(), entity.getNombre(), entity.getCategoria().getId(), entity.getDescripcion(),
                entity.getPrecio(), entity.getUrlImagen(), entity.getActivo(), entity.getRestaurante().getId());
    }
}
