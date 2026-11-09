package com.mich.plazoleta.infrastructure.adapter.output.persistence.entity;

import com.mich.plazoleta.domain.model.Pedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Pedido.Estado estado;

    @Column(name = "id_empleado")
    private Long idEmpleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_restaurante", nullable = false)
    private RestauranteEntity restaurante;

    @Column(length = 6)
    private String pin;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoPlatoEntity> platos = new ArrayList<>();

    protected PedidoEntity() {
    }

    public PedidoEntity(Long idCliente, LocalDateTime fecha, Pedido.Estado estado, RestauranteEntity restaurante) {
        this.idCliente = idCliente;
        this.fecha = fecha;
        this.estado = estado;
        this.restaurante = restaurante;
    }

    public Long getId() { return id; }
    public Long getIdCliente() { return idCliente; }
    public LocalDateTime getFecha() { return fecha; }
    public RestauranteEntity getRestaurante() { return restaurante; }
    public List<PedidoPlatoEntity> getPlatos() { return platos; }

    public Pedido.Estado getEstado() { return estado; }
    public void setEstado(Pedido.Estado estado) { this.estado = estado; }

    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
