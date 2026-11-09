package com.mich.trazabilidad.infrastructure.adapter.output.persistence.entity;

import com.mich.trazabilidad.domain.model.Trazabilidad.Estado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "trazabilidad")
public class TrazabilidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_pedido", nullable = false)
    private Long idPedido;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(name = "correo_cliente", nullable = false)
    private String correoCliente;

    @Column(name = "id_restaurante", nullable = false)
    private Long idRestaurante;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private Estado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private Estado estadoNuevo;

    @Column(name = "id_empleado")
    private Long idEmpleado;

    @Column(name = "correo_empleado")
    private String correoEmpleado;

    protected TrazabilidadEntity() {
    }

    public TrazabilidadEntity(Long id, Long idPedido, Long idCliente, String correoCliente, Long idRestaurante,
                              LocalDateTime fecha, Estado estadoAnterior, Estado estadoNuevo, Long idEmpleado,
                              String correoEmpleado) {
        this.id = id;
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.correoCliente = correoCliente;
        this.idRestaurante = idRestaurante;
        this.fecha = fecha;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.idEmpleado = idEmpleado;
        this.correoEmpleado = correoEmpleado;
    }

    public Long getId() { return id; }
    public Long getIdPedido() { return idPedido; }
    public Long getIdCliente() { return idCliente; }
    public String getCorreoCliente() { return correoCliente; }
    public Long getIdRestaurante() { return idRestaurante; }
    public LocalDateTime getFecha() { return fecha; }
    public Estado getEstadoAnterior() { return estadoAnterior; }
    public Estado getEstadoNuevo() { return estadoNuevo; }
    public Long getIdEmpleado() { return idEmpleado; }
    public String getCorreoEmpleado() { return correoEmpleado; }
}
