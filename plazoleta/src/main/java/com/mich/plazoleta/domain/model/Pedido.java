package com.mich.plazoleta.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public class Pedido {

    public enum Estado {
        PENDIENTE,
        EN_PREPARACION,
        LISTO,
        ENTREGADO,
        CANCELADO
    }

    public record Detalle(Long idPlato, String nombrePlato, Integer cantidad) {
    }

    public record CambioEstado(Long idPedido, Long idCliente, String correoCliente, Long idRestaurante,
                               Estado estadoAnterior, Estado estadoNuevo, Long idEmpleado, String correoEmpleado) {
    }

    private Long id;
    private Long idCliente;
    private LocalDateTime fecha;
    private Estado estado;
    private Long idEmpleado;
    private Long idRestaurante;
    private String pin;
    private List<Detalle> platos;

    public Pedido() {
    }

    public Pedido(Long id, Long idCliente, LocalDateTime fecha, Estado estado, Long idEmpleado, Long idRestaurante,
                  String pin, List<Detalle> platos) {
        this.id = id;
        this.idCliente = idCliente;
        this.fecha = fecha;
        this.estado = estado;
        this.idEmpleado = idEmpleado;
        this.idRestaurante = idRestaurante;
        this.pin = pin;
        this.platos = platos;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public Long getIdRestaurante() { return idRestaurante; }
    public void setIdRestaurante(Long idRestaurante) { this.idRestaurante = idRestaurante; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public List<Detalle> getPlatos() { return platos; }
    public void setPlatos(List<Detalle> platos) { this.platos = platos; }
}
