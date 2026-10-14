package com.example.proyecto_semestre_medico.dto;

import java.time.LocalDateTime;

public class MedicoCitaPanelDTO {

    private Long citaId;
    private String numeroCita;

    private String paciente;
    private String especialidad;

    private LocalDateTime fechaHora;

    private String estado;

    private Boolean emergencia;

    public MedicoCitaPanelDTO() {
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public String getNumeroCita() {
        return numeroCita;
    }

    public void setNumeroCita(String numeroCita) {
        this.numeroCita = numeroCita;
    }

    public String getPaciente() {
        return paciente;
    }

    public void setPaciente(String paciente) {
        this.paciente = paciente;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getEmergencia() {
        return emergencia;
    }

    public void setEmergencia(Boolean emergencia) {
        this.emergencia = emergencia;
    }
}