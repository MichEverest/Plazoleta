package com.mich.plazoleta.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "platos")
public class PlatoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaEntity categoria;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Integer precio;

    @Column(name = "url_imagen", nullable = false)
    private String urlImagen;

    @ColumnDefault("true")
    @Column(nullable = false)
    private Boolean activo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_restaurante", nullable = false)
    private RestauranteEntity restaurante;

    protected PlatoEntity() {
    }

    public PlatoEntity(Long id, String nombre, CategoriaEntity categoria, String descripcion, Integer precio,
                       String urlImagen, Boolean activo, RestauranteEntity restaurante) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.precio = precio;
        this.urlImagen = urlImagen;
        this.activo = activo;
        this.restaurante = restaurante;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public CategoriaEntity getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public Integer getPrecio() { return precio; }
    public String getUrlImagen() { return urlImagen; }
    public Boolean getActivo() { return activo; }
    public RestauranteEntity getRestaurante() { return restaurante; }
}
