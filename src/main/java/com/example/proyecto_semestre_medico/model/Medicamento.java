package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicamento")
public class Medicamento {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 200
    )
    private String nombre;

    @Column(
            nullable = false,
            length = 500
    )
    private String descripcion;

    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precio;

    @Column(
            nullable = false,
            length = 50
    )
    private String unidad;

    @Column(
            name = "es_controlado",
            nullable = false
    )
    private Boolean esControlado = false;

    @Column(
            name = "stock_minimo"
    )
    private Integer stockMinimo;

    @Column(
            nullable = false
    )
    private Boolean activo = true;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    public Medicamento() {
    }

    @PrePersist
    public void prePersist() {

        if (activo == null) {
            activo = true;
        }

        if (esControlado == null) {
            esControlado = false;
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(
            String nombre
    ) {
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

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(
            BigDecimal precio
    ) {
        this.precio = precio;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(
            String unidad
    ) {
        this.unidad = unidad;
    }

    public Boolean getEsControlado() {
        return esControlado;
    }

    public void setEsControlado(
            Boolean esControlado
    ) {
        this.esControlado =
                esControlado;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(
            Integer stockMinimo
    ) {
        this.stockMinimo =
                stockMinimo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
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