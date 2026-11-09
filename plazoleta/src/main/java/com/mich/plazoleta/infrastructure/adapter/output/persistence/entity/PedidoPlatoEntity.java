package com.mich.plazoleta.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "pedidos_platos")
public class PedidoPlatoEntity {

    @Embeddable
    public static class Id implements Serializable {

        private Long idPedido;
        private Long idPlato;

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Id id && Objects.equals(idPedido, id.idPedido) && Objects.equals(idPlato, id.idPlato);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idPedido, idPlato);
        }
    }

    @EmbeddedId
    private Id id = new Id();

    @MapsId("idPedido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido")
    private PedidoEntity pedido;

    @MapsId("idPlato")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plato")
    private PlatoEntity plato;

    @Column(nullable = false)
    private Integer cantidad;

    protected PedidoPlatoEntity() {
    }

    public PedidoPlatoEntity(PedidoEntity pedido, PlatoEntity plato, Integer cantidad) {
        this.pedido = pedido;
        this.plato = plato;
        this.cantidad = cantidad;
    }

    public PlatoEntity getPlato() { return plato; }
    public Integer getCantidad() { return cantidad; }
}
