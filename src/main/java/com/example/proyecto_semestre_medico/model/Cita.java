package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cita")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "numero_cita",
            nullable = false,
            unique = true,
            length = 30
    )
    private String numeroCita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "especialidad_id"
    )
    private Especialidad especialidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "medico_id"
    )
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_cita_id", nullable = false)
    private EstadoCita estadoCita;

    @Column(
            name = "fecha_hora_inicio",
            nullable = false
    )
    private LocalDateTime fechaHoraInicio;

    @Column(
            name = "fecha_hora_fin",
            nullable = false
    )
    private LocalDateTime fechaHoraFin;

    @Column(
            name = "motivo_consulta",
            nullable = false,
            length = 2000
    )
    private String motivoConsulta;

    @Column(
            nullable = false,
            length = 20
    )
    private String origen = "PORTAL";

    @Column(name = "reserva_expira_en")
    private LocalDateTime reservaExpiraEn;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(
            name = "prioridad_emergencia",
            nullable = false
    )
    private Boolean prioridadEmergencia = false;

    @Column(
            name = "fecha_hora_llegada"
    )
    private LocalDateTime fechaHoraLlegada;

    /*
     * =========================================================
     * CU-12 - DATOS DE SEGUIMIENTO
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cita_seguimiento_de_id"
    )
    private Cita citaSeguimientoDe;

    @Column(
            name = "tipo_seguimiento",
            length = 50
    )
    private String tipoSeguimiento;

    @Column(
            name = "prioridad_seguimiento",
            length = 20
    )
    private String prioridadSeguimiento;

    @Column(
            name = "recordatorio_seguimiento_enviado",
            nullable = false
    )
    private Boolean recordatorioSeguimientoEnviado = false;

    @Column(
            name = "fecha_recordatorio_seguimiento"
    )
    private LocalDateTime fechaRecordatorioSeguimiento;


    @Version
    @Column(nullable = false)
    private Long version;

    public Cita() {
    }

    public String getTipoSeguimientoDescripcion() {
        if (tipoSeguimiento == null) return "";
        return switch (tipoSeguimiento) {
            case "MONITOREO_TRATAMIENTO" -> "Monitoreo de Tratamiento";
            case "REVISION_RESULTADOS_LABORATORIO" -> "Revisión de Resultados de Laboratorio";
            default -> tipoSeguimiento;
        };
    }

    @PrePersist
    public void prePersist() {

        if (fechaCreacion == null) {
            fechaCreacion =
                    LocalDateTime.now();
        }

        if (origen == null
                || origen.isBlank()) {
            origen = "PORTAL";
        }

        if (prioridadEmergencia == null) {
            prioridadEmergencia = false;
        }

        if (recordatorioSeguimientoEnviado == null) {
            recordatorioSeguimientoEnviado = false;
        }
    }

    public Long getId() {
        return id;
    }

    public String getNumeroCita() {
        return numeroCita;
    }

    public void setNumeroCita(String numeroCita) {
        this.numeroCita = numeroCita;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(
            Especialidad especialidad
    ) {
        this.especialidad = especialidad;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public EstadoCita getEstadoCita() {
        return estadoCita;
    }

    public void setEstadoCita(
            EstadoCita estadoCita
    ) {
        this.estadoCita = estadoCita;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(
            LocalDateTime fechaHoraInicio
    ) {
        this.fechaHoraInicio =
                fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(
            LocalDateTime fechaHoraFin
    ) {
        this.fechaHoraFin =
                fechaHoraFin;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(
            String motivoConsulta
    ) {
        this.motivoConsulta =
                motivoConsulta;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public LocalDateTime getReservaExpiraEn() {
        return reservaExpiraEn;
    }

    public void setReservaExpiraEn(
            LocalDateTime reservaExpiraEn
    ) {
        this.reservaExpiraEn =
                reservaExpiraEn;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Long getVersion() {
        return version;
    }

    public Boolean getPrioridadEmergencia() {
        return prioridadEmergencia;
    }

    public void setPrioridadEmergencia(
            Boolean prioridadEmergencia
    ) {
        this.prioridadEmergencia =
                prioridadEmergencia;
    }

    public Cita getCitaSeguimientoDe() {
        return citaSeguimientoDe;
    }

    public void setCitaSeguimientoDe(
            Cita citaSeguimientoDe
    ) {
        this.citaSeguimientoDe =
                citaSeguimientoDe;
    }

    public String getTipoSeguimiento() {
        return tipoSeguimiento;
    }

    public void setTipoSeguimiento(
            String tipoSeguimiento
    ) {
        this.tipoSeguimiento =
                tipoSeguimiento;
    }

    public String getPrioridadSeguimiento() {
        return prioridadSeguimiento;
    }

    public void setPrioridadSeguimiento(
            String prioridadSeguimiento
    ) {
        this.prioridadSeguimiento =
                prioridadSeguimiento;
    }

    public Boolean getRecordatorioSeguimientoEnviado() {
        return recordatorioSeguimientoEnviado;
    }

    public void setRecordatorioSeguimientoEnviado(
            Boolean recordatorioSeguimientoEnviado
    ) {
        this.recordatorioSeguimientoEnviado =
                recordatorioSeguimientoEnviado;
    }

    public LocalDateTime getFechaRecordatorioSeguimiento() {
        return fechaRecordatorioSeguimiento;
    }

    public void setFechaRecordatorioSeguimiento(
            LocalDateTime fechaRecordatorioSeguimiento
    ) {
        this.fechaRecordatorioSeguimiento =
                fechaRecordatorioSeguimiento;
    }

    public LocalDateTime getFechaHoraLlegada() {
        return fechaHoraLlegada;
    }

    public void setFechaHoraLlegada(
            LocalDateTime fechaHoraLlegada
    ) {
        this.fechaHoraLlegada =
                fechaHoraLlegada;
    }
}
