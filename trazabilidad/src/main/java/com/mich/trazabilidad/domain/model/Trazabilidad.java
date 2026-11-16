package com.mich.trazabilidad.domain.model;

import java.time.LocalDateTime;

public class Trazabilidad {

    public enum Estado {
        PENDIENTE(false),
        EN_PREPARACION(false),
        LISTO(false),
        ENTREGADO(true),
        CANCELADO(true);

        private final boolean esFinal;

        Estado(boolean esFinal) {
            this.esFinal = esFinal;
        }

        public boolean esFinal() {
            return esFinal;
        }
    }

    /**
     * conSms: en el paso a LISTO, true si el PIN se envió por SMS y false si se usó el atajo sin SMS; null en los demás.
     */
    public record Historial(Estado estadoAnterior, Estado estadoNuevo, LocalDateTime fecha, String correoEmpleado,
                            Long duracionSegundos, Boolean conSms) {
    }

    public record Resumen(Long idPedido, Long idRestaurante, Estado estadoActual, LocalDateTime fechaCreacion,
                          LocalDateTime fechaUltimoCambio) {
    }

    private Long id;
    private Long idPedido;
    private Long idCliente;
    private String correoCliente;
    private Long idRestaurante;
    private LocalDateTime fecha;
    private Estado estadoAnterior;
    private Estado estadoNuevo;
    private Long idEmpleado;
    private String correoEmpleado;
    private Boolean conSms;

    public Trazabilidad() {
    }

    public Trazabilidad(Long id, Long idPedido, Long idCliente, String correoCliente, Long idRestaurante,
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
    public void setId(Long id) { this.id = id; }

    public Long getIdPedido() { return idPedido; }
    public void setIdPedido(Long idPedido) { this.idPedido = idPedido; }

    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }

    public String getCorreoCliente() { return correoCliente; }
    public void setCorreoCliente(String correoCliente) { this.correoCliente = correoCliente; }

    public Long getIdRestaurante() { return idRestaurante; }
    public void setIdRestaurante(Long idRestaurante) { this.idRestaurante = idRestaurante; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Estado getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(Estado estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public Estado getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(Estado estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getCorreoEmpleado() { return correoEmpleado; }
    public void setCorreoEmpleado(String correoEmpleado) { this.correoEmpleado = correoEmpleado; }

    public Boolean getConSms() { return conSms; }
    public void setConSms(Boolean conSms) { this.conSms = conSms; }
}
