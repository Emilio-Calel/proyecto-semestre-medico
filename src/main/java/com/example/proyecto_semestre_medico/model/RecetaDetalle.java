package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

@Entity
@Table(name = "receta_detalle")
public class RecetaDetalle {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "receta_id",
            nullable = false
    )
    private Receta receta;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "medicamento_id",
            nullable = false
    )
    private Medicamento medicamento;

    @Column(
            nullable = false,
            length = 200
    )
    private String dosis;

    @Column(
            nullable = false
    )
    private Integer cantidad;

    @Column(
            name = "cantidad_despachada",
            nullable = false
    )
    private Integer cantidadDespachada = 0;

    @Column(
            length = 1000
    )
    private String indicaciones;

    @Column(
            nullable = false
    )
    private Boolean activo = true;

    public RecetaDetalle() {
    }

    @PrePersist
    public void prePersist() {

        if (activo == null) {
            activo = true;
        }

        if (cantidadDespachada == null) {
            cantidadDespachada = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Receta getReceta() {
        return receta;
    }

    public void setReceta(
            Receta receta
    ) {
        this.receta = receta;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(
            Medicamento medicamento
    ) {
        this.medicamento =
                medicamento;
    }

    public String getDosis() {
        return dosis;
    }

    public void setDosis(
            String dosis
    ) {
        this.dosis = dosis;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(
            Integer cantidad
    ) {
        this.cantidad = cantidad;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(
            String indicaciones
    ) {
        this.indicaciones =
                indicaciones;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
        this.activo = activo;
    }

    public Integer getCantidadDespachada() {
        return cantidadDespachada;
    }

    public void setCantidadDespachada(
            Integer cantidadDespachada
    ) {
        this.cantidadDespachada =
                cantidadDespachada;
    }
}