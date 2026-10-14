package com.example.proyecto_semestre_medico.dto;

public class ConsultaMedicaFormDTO {

    private Long consultaId;

    private Long citaId;

    private String numeroCita;

    private String paciente;

    private String motivoVisita;

    private String hallazgosClinicos;

    private Long cie10Id;

    private String codigoCie10;

    private String descripcionCie10;

    private String diagnostico;

    private String planTratamiento;

    private String notasAdicionales;

    private String estado;

    public ConsultaMedicaFormDTO() {
    }

    public Long getConsultaId() {
        return consultaId;
    }

    public void setConsultaId(Long consultaId) {
        this.consultaId = consultaId;
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

    public String getMotivoVisita() {
        return motivoVisita;
    }

    public void setMotivoVisita(String motivoVisita) {
        this.motivoVisita = motivoVisita;
    }

    public String getHallazgosClinicos() {
        return hallazgosClinicos;
    }

    public void setHallazgosClinicos(String hallazgosClinicos) {
        this.hallazgosClinicos = hallazgosClinicos;
    }

    public Long getCie10Id() {
        return cie10Id;
    }

    public void setCie10Id(Long cie10Id) {
        this.cie10Id = cie10Id;
    }

    public String getCodigoCie10() {
        return codigoCie10;
    }

    public void setCodigoCie10(String codigoCie10) {
        this.codigoCie10 = codigoCie10;
    }

    public String getDescripcionCie10() {
        return descripcionCie10;
    }

    public void setDescripcionCie10(String descripcionCie10) {
        this.descripcionCie10 = descripcionCie10;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getPlanTratamiento() {
        return planTratamiento;
    }

    public void setPlanTratamiento(String planTratamiento) {
        this.planTratamiento = planTratamiento;
    }

    public String getNotasAdicionales() {
        return notasAdicionales;
    }

    public void setNotasAdicionales(String notasAdicionales) {
        this.notasAdicionales = notasAdicionales;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}