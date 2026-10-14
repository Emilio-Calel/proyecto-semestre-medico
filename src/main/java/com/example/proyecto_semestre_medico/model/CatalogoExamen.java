package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalogo_examen")
public class CatalogoExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "codigo",
            nullable = false,
            unique = true,
            length = 30
    )
    private String codigo;

    @Column(
            name = "nombre",
            nullable = false,
            length = 150
    )
    private String nombre;

    @Column(
            name = "descripcion",
            length = 500
    )
    private String descripcion;

    @Column(
            name = "unidad_referencia",
            length = 50
    )
    private String unidadReferencia;

    @Column(
            name = "rango_referencia",
            length = 100
    )
    private String rangoReferencia;

    @Column(
            name = "precio",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precio;

    @Column(
            name = "activo",
            nullable = false
    )
    private Boolean activo;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {

        if (activo == null) {
            activo = true;
        }

        if (fechaCreacion == null) {
            fechaCreacion =
                    LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(
            String descripcion
    ) {
        this.descripcion = descripcion;
    }

    public String getUnidadReferencia() {
        return unidadReferencia;
    }

    public void setUnidadReferencia(
            String unidadReferencia
    ) {
        this.unidadReferencia =
                unidadReferencia;
    }

    public String getRangoReferencia() {
        return rangoReferencia;
    }

    public void setRangoReferencia(
            String rangoReferencia
    ) {
        this.rangoReferencia =
                rangoReferencia;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(
            BigDecimal precio
    ) {
        this.precio = precio;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(
            LocalDateTime fechaCreacion
    ) {
        this.fechaCreacion =
                fechaCreacion;
    }
}