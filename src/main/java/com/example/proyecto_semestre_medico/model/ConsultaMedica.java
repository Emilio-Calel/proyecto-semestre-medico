package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consulta_medica")
public class ConsultaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cita_id",
            nullable = false,
            unique = true
    )
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "medico_id",
            nullable = false
    )
    private Medico medico;

    @Column(
            name = "motivo_visita",
            nullable = false,
            length = 2000
    )
    private String motivoVisita;

    @Column(
            name = "hallazgos_clinicos",
            length = 5000
    )
    private String hallazgosClinicos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cie10_id"
    )
    private Cie10 cie10;

    @Column(
            name = "diagnostico",
            length = 5000
    )
    private String diagnostico;

    @Column(
            name = "plan_tratamiento",
            length = 5000
    )
    private String planTratamiento;

    @Column(
            name = "notas_adicionales",
            length = 5000
    )
    private String notasAdicionales;

    @Column(
            name = "estado",
            nullable = false,
            length = 20
    )
    private String estado;

    @Column(
            name = "fecha_inicio",
            nullable = false
    )
    private LocalDateTime fechaInicio;

    @Column(
            name = "fecha_finalizacion"
    )
    private LocalDateTime fechaFinalizacion;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @PrePersist
    public void prePersist() {

        if (estado == null) {
            estado = "EN_CURSO";
        }

        if (fechaInicio == null) {
            fechaInicio =
                    LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public String getMotivoVisita() {
        return motivoVisita;
    }

    public void setMotivoVisita(
            String motivoVisita
    ) {
        this.motivoVisita = motivoVisita;
    }

    public String getHallazgosClinicos() {
        return hallazgosClinicos;
    }

    public void setHallazgosClinicos(
            String hallazgosClinicos
    ) {
        this.hallazgosClinicos =
                hallazgosClinicos;
    }

    public Cie10 getCie10() {
        return cie10;
    }

    public void setCie10(Cie10 cie10) {
        this.cie10 = cie10;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(
            String diagnostico
    ) {
        this.diagnostico = diagnostico;
    }

    public String getPlanTratamiento() {
        return planTratamiento;
    }

    public void setPlanTratamiento(
            String planTratamiento
    ) {
        this.planTratamiento =
                planTratamiento;
    }

    public String getNotasAdicionales() {
        return notasAdicionales;
    }

    public void setNotasAdicionales(
            String notasAdicionales
    ) {
        this.notasAdicionales =
                notasAdicionales;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(
            LocalDateTime fechaInicio
    ) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(
            LocalDateTime fechaFinalizacion
    ) {
        this.fechaFinalizacion =
                fechaFinalizacion;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}