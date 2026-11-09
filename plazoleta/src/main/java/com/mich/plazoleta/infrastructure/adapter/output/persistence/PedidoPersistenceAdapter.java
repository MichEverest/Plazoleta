package com.mich.plazoleta.infrastructure.adapter.output.persistence;

import com.mich.plazoleta.application.port.output.PedidoRepositoryPort;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Pedido;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.PlatoPersistenceAdapter.PlatoJpaRepository;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.RestaurantePersistenceAdapter.RestauranteJpaRepository;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.PedidoEntity;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.PedidoPlatoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class PedidoPersistenceAdapter implements PedidoRepositoryPort {

    private static final Sort MAS_RECIENTES_PRIMERO = Sort.by(Sort.Direction.DESC, "fecha");
    private static final Sort MAS_ANTIGUOS_PRIMERO = Sort.by(Sort.Direction.ASC, "fecha");
    private static final String MENSAJE_PEDIDO_NO_EXISTE = "No existe el pedido con id %d";

    public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select p from PedidoEntity p where p.id = :id")
        Optional<PedidoEntity> buscarParaActualizar(@Param("id") Long id);

        boolean existsByIdClienteAndEstadoIn(Long idCliente, Collection<Pedido.Estado> estados);

        Page<PedidoEntity> findByIdCliente(Long idCliente, Pageable pageable);

        @Query("""
                select p from PedidoEntity p
                where p.restaurante.id = :idRestaurante
                  and (:estado is null or p.estado = :estado)
                """)
        Page<PedidoEntity> buscarPorRestaurante(@Param("idRestaurante") Long idRestaurante,
                                                @Param("estado") Pedido.Estado estado,
                                                Pageable pageable);
    }

    private final PedidoJpaRepository pedidoJpaRepository;
    private final RestauranteJpaRepository restauranteJpaRepository;
    private final PlatoJpaRepository platoJpaRepository;

    public PedidoPersistenceAdapter(PedidoJpaRepository pedidoJpaRepository,
                                    RestauranteJpaRepository restauranteJpaRepository,
                                    PlatoJpaRepository platoJpaRepository) {
        this.pedidoJpaRepository = pedidoJpaRepository;
        this.restauranteJpaRepository = restauranteJpaRepository;
        this.platoJpaRepository = platoJpaRepository;
    }

    @Override
    public Pedido guardar(Pedido pedido) {
        PedidoEntity entity = new PedidoEntity(pedido.getIdCliente(), pedido.getFecha(), pedido.getEstado(),
                restauranteJpaRepository.getReferenceById(pedido.getIdRestaurante()));
        for (Pedido.Detalle detalle : pedido.getPlatos()) {
            entity.getPlatos().add(new PedidoPlatoEntity(entity,
                    platoJpaRepository.getReferenceById(detalle.idPlato()), detalle.cantidad()));
        }
        return toDomain(pedidoJpaRepository.save(entity));
    }

    @Override
    public Pedido actualizar(Pedido pedido) {
        PedidoEntity entity = pedidoJpaRepository.findById(pedido.getId())
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_PEDIDO_NO_EXISTE, pedido.getId())));
        entity.setEstado(pedido.getEstado());
        entity.setIdEmpleado(pedido.getIdEmpleado());
        entity.setPin(pedido.getPin());
        return toDomain(pedidoJpaRepository.save(entity));
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoJpaRepository.buscarParaActualizar(id).map(this::toDomain);
    }

    @Override
    public boolean existePedidoDeCliente(Long idCliente, List<Pedido.Estado> estados) {
        return pedidoJpaRepository.existsByIdClienteAndEstadoIn(idCliente, estados);
    }

    @Override
    public Pagina<Pedido> listarPorCliente(Long idCliente, Pagina.Solicitud solicitud) {
        return PaginacionJpa.pagina(pedidoJpaRepository.findByIdCliente(idCliente,
                PaginacionJpa.solicitud(solicitud, MAS_RECIENTES_PRIMERO)), this::toDomain);
    }

    @Override
    public Pagina<Pedido> listarPorRestaurante(Long idRestaurante, Pedido.Estado estado, Pagina.Solicitud solicitud) {
        return PaginacionJpa.pagina(pedidoJpaRepository.buscarPorRestaurante(idRestaurante, estado,
                PaginacionJpa.solicitud(solicitud, MAS_ANTIGUOS_PRIMERO)), this::toDomain);
    }

    private Pedido toDomain(PedidoEntity entity) {
        List<Pedido.Detalle> platos = entity.getPlatos().stream()
                .map(detalle -> new Pedido.Detalle(detalle.getPlato().getId(), detalle.getPlato().getNombre(),
                        detalle.getCantidad()))
                .toList();
        return new Pedido(entity.getId(), entity.getIdCliente(), entity.getFecha(), entity.getEstado(),
                entity.getIdEmpleado(), entity.getRestaurante().getId(), entity.getPin(), platos);
    }
}
