package com.mich.usuarios.infrastructure.adapter.output.persistence.entity;

import com.mich.usuarios.domain.model.Usuario.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Rol nombre;

    @Column(nullable = false)
    private String descripcion;

    protected RolEntity() {
    }

    public RolEntity(Rol nombre) {
        this.nombre = nombre;
        this.descripcion = nombre.getDescripcion();
    }

    public Rol getNombre() { return nombre; }
}
