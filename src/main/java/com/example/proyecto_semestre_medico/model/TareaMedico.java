package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tarea_medico")
public class TareaMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "medico_id",
            nullable = false
    )
    private Medico medico;

    @Column(
            nullable = false,
            length = 200
    )
    private String titulo;

    @Column(
            length = 2000
    )
    private String descripcion;

    @Column(
            nullable = false,
            length = 20
    )
    private String prioridad;

    @Column(
            name = "fecha_limite"
    )
    private LocalDateTime fechaLimite;

    @Column(
            nullable = false
    )
    private Boolean completada = false;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(
            nullable = false
    )
    private Boolean activo = true;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    public TareaMedico() {
    }

    @PrePersist
    public void prePersist() {

        if (completada == null) {
            completada = false;
        }

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

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(
            Medico medico
    ) {
        this.medico = medico;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(
            String titulo
    ) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(
            String descripcion
    ) {
        this.descripcion =
                descripcion;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(
            String prioridad
    ) {
        this.prioridad = prioridad;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(
            LocalDateTime fechaLimite
    ) {
        this.fechaLimite =
                fechaLimite;
    }

    public Boolean getCompletada() {
        return "COMPLETADA".equals(estado);
    }

    public void setCompletada(
            Boolean completada
    ) {
        this.completada = completada;
        this.estado = Boolean.TRUE.equals(completada) ? "COMPLETADA" : "PENDIENTE";
    }

    public String getEstado() { return estado; }

    public void setEstado(String estado) {
        this.estado = estado;
        this.completada = "COMPLETADA".equals(estado);
    }

    public String getEstadoDescripcion() {
        return switch (estado) {
            case "EN_PROGRESO" -> "En progreso";
            case "COMPLETADA" -> "Completada";
            default -> "Pendiente";
        };
    }

    public String getPrioridadDescripcion() {
        return "NORMAL".equals(prioridad) ? "Media" : ("ALTA".equals(prioridad) ? "Alta" : "Baja");
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
}
