package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenLaboratorioDetalleDTO {

    private Long id;

    private String numeroOrden;

    private String paciente;
    private String medico;

    private String estado;

    private BigDecimal montoTotal;

    private Boolean externa;

    private String notas;

    private LocalDateTime fechaCreacion;

    private List<ExamenOrdenDetalleDTO> examenes =
            new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getPaciente() {
        return paciente;
    }

    public void setPaciente(String paciente) {
        this.paciente = paciente;
    }

    public String getMedico() {
        return medico;
    }

    public void setMedico(String medico) {
        this.medico = medico;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public Boolean getExterna() {
        return externa;
    }

    public void setExterna(Boolean externa) {
        this.externa = externa;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<ExamenOrdenDetalleDTO> getExamenes() {
        return examenes;
    }

    public void setExamenes(
            List<ExamenOrdenDetalleDTO> examenes
    ) {
        this.examenes = examenes;
    }
}