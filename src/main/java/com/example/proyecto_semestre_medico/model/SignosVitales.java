package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signos_vitales")
public class SignosVitales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cita_id",
            nullable = false,
            unique = true
    )
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_enfermeria_id",
            nullable = false
    )
    private Usuario usuarioEnfermeria;

    @Column(
            name = "presion_sistolica",
            nullable = false
    )
    private Integer presionSistolica;

    @Column(
            name = "presion_diastolica",
            nullable = false
    )
    private Integer presionDiastolica;

    @Column(nullable = false)
    private Integer pulso;

    @Column(
            nullable = false,
            precision = 4,
            scale = 1
    )
    private BigDecimal temperatura;

    @Column(
            nullable = false,
            precision = 6,
            scale = 2
    )
    private BigDecimal peso;

    @Column(
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal talla;

    @Column(length = 1000)
    private String observaciones;

    @Column(
            name = "fecha_registro",
            nullable = false
    )
    private LocalDateTime fechaRegistro;

    public SignosVitales() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro =
                    LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    public Usuario getUsuarioEnfermeria() {
        return usuarioEnfermeria;
    }

    public void setUsuarioEnfermeria(
            Usuario usuarioEnfermeria
    ) {
        this.usuarioEnfermeria =
                usuarioEnfermeria;
    }

    public Integer getPresionSistolica() {
        return presionSistolica;
    }

    public void setPresionSistolica(
            Integer presionSistolica
    ) {
        this.presionSistolica =
                presionSistolica;
    }

    public Integer getPresionDiastolica() {
        return presionDiastolica;
    }

    public void setPresionDiastolica(
            Integer presionDiastolica
    ) {
        this.presionDiastolica =
                presionDiastolica;
    }

    public Integer getPulso() {
        return pulso;
    }

    public void setPulso(Integer pulso) {
        this.pulso = pulso;
    }

    public BigDecimal getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(
            BigDecimal temperatura
    ) {
        this.temperatura = temperatura;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public BigDecimal getTalla() {
        return talla;
    }

    public void setTalla(BigDecimal talla) {
        this.talla = talla;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones
    ) {
        this.observaciones =
                observaciones;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}