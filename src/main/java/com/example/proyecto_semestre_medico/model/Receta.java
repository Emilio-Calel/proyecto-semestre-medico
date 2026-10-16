package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receta")
public class Receta {

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
            name = "consulta_medica_id",
            nullable = false
    )
    private ConsultaMedica consultaMedica;

    @Column(
            name = "fecha_emision",
            nullable = false
    )
    private LocalDateTime fechaEmision;

    @Column(
            name = "fecha_vencimiento",
            nullable = false
    )
    private LocalDateTime fechaVencimiento;

    @Column(
            length = 1000
    )
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private EstadoReceta estado =
            EstadoReceta.PENDIENTE;

    @Column(
            nullable = false
    )
    private Boolean activo = true;

    @OneToMany(
            mappedBy = "receta",
            cascade = CascadeType.ALL,
            orphanRemoval = false
    )
    private List<RecetaDetalle> detalles =
            new ArrayList<>();

    public Receta() {
    }

    @PrePersist
    public void prePersist() {

        if (fechaEmision == null) {
            fechaEmision =
                    LocalDateTime.now();
        }

        if (fechaVencimiento == null) {
            fechaVencimiento =
                    fechaEmision.plusDays(7);
        }

        if (estado == null) {
            estado =
                    EstadoReceta.PENDIENTE;
        }

        if (activo == null) {
            activo = true;
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

    public ConsultaMedica getConsultaMedica() {
        return consultaMedica;
    }

    public void setConsultaMedica(
            ConsultaMedica consultaMedica
    ) {
        this.consultaMedica =
                consultaMedica;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(
            LocalDateTime fechaEmision
    ) {
        this.fechaEmision =
                fechaEmision;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(
            LocalDateTime fechaVencimiento
    ) {
        this.fechaVencimiento =
                fechaVencimiento;
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

    public EstadoReceta getEstado() {
        return estado;
    }

    public void setEstado(
            EstadoReceta estado
    ) {
        this.estado =
                estado;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
        this.activo =
                activo;
    }

    public List<RecetaDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(
            List<RecetaDetalle> detalles
    ) {
        this.detalles =
                detalles;
    }
}