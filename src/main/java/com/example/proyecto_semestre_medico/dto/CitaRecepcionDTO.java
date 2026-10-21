package com.example.proyecto_semestre_medico.dto;

import java.time.LocalDateTime;

public class CitaRecepcionDTO {

    private Long citaId;
    private Long pacienteId;
    private String numeroCita;

    private String paciente;
    private String telefono;
    private String correo;

    private String sucursal;
    private String especialidad;
    private String medico;

    private LocalDateTime fechaHora;

    private String estado;

    private String estadoPago;
    private String metodoPago;

    private LocalDateTime fechaHoraLlegada;

    private String motivoConsulta;

    private Boolean prioridadEmergencia;

    public CitaRecepcionDTO(
            Long citaId,
            Long pacienteId,
            String numeroCita,
            String paciente,
            String telefono,
            String correo,
            String sucursal,
            String especialidad,
            String medico,
            LocalDateTime fechaHora,
            LocalDateTime fechaHoraLlegada,
            String motivoConsulta,
            Boolean prioridadEmergencia,
            String estado,
            String estadoPago,
            String metodoPago
    ) {
        this.citaId = citaId;
        this.numeroCita = numeroCita;
        this.paciente = paciente;
        this.telefono = telefono;
        this.correo = correo;
        this.sucursal = sucursal;
        this.especialidad = especialidad;
        this.medico = medico;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.estadoPago = estadoPago;
        this.metodoPago = metodoPago;
        this.fechaHoraLlegada =
                fechaHoraLlegada;

        this.motivoConsulta =
                motivoConsulta;

        this.prioridadEmergencia =
                prioridadEmergencia;
        this.pacienteId =
                pacienteId;
    }

    public Long getCitaId() {
        return citaId;
    }

    public String getNumeroCita() {
        return numeroCita;
    }

    public String getPaciente() {
        return paciente;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public String getSucursal() {
        return sucursal;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getMedico() {
        return medico;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getEstado() {
        return estado;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public LocalDateTime getFechaHoraLlegada() {
        return fechaHoraLlegada;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public Boolean getPrioridadEmergencia() {
        return prioridadEmergencia;
    }
    public Long getPacienteId() {
        return pacienteId;
    }
}