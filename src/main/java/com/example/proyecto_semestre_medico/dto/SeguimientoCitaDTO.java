package com.example.proyecto_semestre_medico.dto;

import java.time.LocalDateTime;

public class SeguimientoCitaDTO {

    private Long citaOrigenId;

    private LocalDateTime fechaHoraInicio;

    private String tipoSeguimiento;

    private String prioridad;

    private String motivo;

    public SeguimientoCitaDTO() {
    }

    public Long getCitaOrigenId() {
        return citaOrigenId;
    }

    public void setCitaOrigenId(
            Long citaOrigenId
    ) {
        this.citaOrigenId =
                citaOrigenId;
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

    public String getTipoSeguimiento() {
        return tipoSeguimiento;
    }

    public void setTipoSeguimiento(
            String tipoSeguimiento
    ) {
        this.tipoSeguimiento =
                tipoSeguimiento;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(
            String prioridad
    ) {
        this.prioridad =
                prioridad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(
            String motivo
    ) {
        this.motivo =
                motivo;
    }
}