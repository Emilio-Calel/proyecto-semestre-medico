package com.example.proyecto_semestre_medico.dto;

public class ResultadoLlegadaRecepcionDTO {

    private final String nombrePaciente;
    private final String numeroCita;
    private final boolean emergencia;

    public ResultadoLlegadaRecepcionDTO(
            String nombrePaciente,
            String numeroCita,
            boolean emergencia
    ) {
        this.nombrePaciente = nombrePaciente;
        this.numeroCita = numeroCita;
        this.emergencia = emergencia;
    }

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public String getNumeroCita() {
        return numeroCita;
    }

    public boolean isEmergencia() {
        return emergencia;
    }
}