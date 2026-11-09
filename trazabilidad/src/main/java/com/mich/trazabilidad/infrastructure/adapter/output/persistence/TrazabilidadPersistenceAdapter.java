package com.mich.trazabilidad.infrastructure.adapter.output.persistence;

import com.mich.trazabilidad.application.port.output.TrazabilidadRepositoryPort;
import com.mich.trazabilidad.domain.model.Trazabilidad;
import com.mich.trazabilidad.infrastructure.adapter.output.persistence.entity.TrazabilidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrazabilidadPersistenceAdapter implements TrazabilidadRepositoryPort {

    public interface TrazabilidadJpaRepository extends JpaRepository<TrazabilidadEntity, Long> {

        List<TrazabilidadEntity> findByIdPedidoOrderByFechaAscIdAsc(Long idPedido);

        List<TrazabilidadEntity> findByIdRestauranteOrderByFechaAscIdAsc(Long idRestaurante);
    }

    private final TrazabilidadJpaRepository trazabilidadJpaRepository;

    public TrazabilidadPersistenceAdapter(TrazabilidadJpaRepository trazabilidadJpaRepository) {
        this.trazabilidadJpaRepository = trazabilidadJpaRepository;
    }

    @Override
    public void guardar(Trazabilidad trazabilidad) {
        trazabilidadJpaRepository.save(new TrazabilidadEntity(trazabilidad.getId(), trazabilidad.getIdPedido(),
                trazabilidad.getIdCliente(), trazabilidad.getCorreoCliente(), trazabilidad.getIdRestaurante(),
                trazabilidad.getFecha(), trazabilidad.getEstadoAnterior(), trazabilidad.getEstadoNuevo(),
                trazabilidad.getIdEmpleado(), trazabilidad.getCorreoEmpleado()));
    }

    @Override
    public List<Trazabilidad> listarPorPedido(Long idPedido) {
        return trazabilidadJpaRepository.findByIdPedidoOrderByFechaAscIdAsc(idPedido).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Trazabilidad> listarPorRestaurante(Long idRestaurante) {
        return trazabilidadJpaRepository.findByIdRestauranteOrderByFechaAscIdAsc(idRestaurante).stream()
                .map(this::toDomain)
                .toList();
    }

    private Trazabilidad toDomain(TrazabilidadEntity entity) {
        return new Trazabilidad(entity.getId(), entity.getIdPedido(), entity.getIdCliente(), entity.getCorreoCliente(),
                entity.getIdRestaurante(), entity.getFecha(), entity.getEstadoAnterior(), entity.getEstadoNuevo(),
                entity.getIdEmpleado(), entity.getCorreoEmpleado());
    }
}
