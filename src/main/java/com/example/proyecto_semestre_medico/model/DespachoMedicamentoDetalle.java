package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "despacho_medicamento_detalle")
public class DespachoMedicamentoDetalle {

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
            name = "despacho_id",
            nullable = false
    )
    private DespachoMedicamento despacho;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "receta_detalle_id",
            nullable = false
    )
    private RecetaDetalle recetaDetalle;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "medicamento_despachado_id",
            nullable = false
    )
    private Medicamento medicamentoDespachado;

    @Column(
            nullable = false
    )
    private Integer cantidad;

    @Column(
            name = "precio_unitario",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precioUnitario;

    @Column(
            nullable = false
    )
    private Boolean sustituido = false;

    @Column(
            name = "motivo_sustitucion",
            length = 500
    )
    private String motivoSustitucion;

    public DespachoMedicamentoDetalle() {
    }

    @PrePersist
    public void prePersist() {

        if (sustituido == null) {
            sustituido = false;
        }
    }

    public Long getId() {
        return id;
    }

    public DespachoMedicamento getDespacho() {
        return despacho;
    }

    public void setDespacho(
            DespachoMedicamento despacho
    ) {
        this.despacho = despacho;
    }

    public RecetaDetalle getRecetaDetalle() {
        return recetaDetalle;
    }

    public void setRecetaDetalle(
            RecetaDetalle recetaDetalle
    ) {
        this.recetaDetalle =
                recetaDetalle;
    }

    public Medicamento getMedicamentoDespachado() {
        return medicamentoDespachado;
    }

    public void setMedicamentoDespachado(
            Medicamento medicamentoDespachado
    ) {
        this.medicamentoDespachado =
                medicamentoDespachado;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(
            Integer cantidad
    ) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(
            BigDecimal precioUnitario
    ) {
        this.precioUnitario =
                precioUnitario;
    }

    public Boolean getSustituido() {
        return sustituido;
    }

    public void setSustituido(
            Boolean sustituido
    ) {
        this.sustituido = sustituido;
    }

    public String getMotivoSustitucion() {
        return motivoSustitucion;
    }

    public void setMotivoSustitucion(
            String motivoSustitucion
    ) {
        this.motivoSustitucion =
                motivoSustitucion;
    }
}