package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrdenLaboratorioPagoDTO {

    private Long id;
    private String numeroOrden;
    private String paciente;
    private String dpi;
    private Integer cantidadExamenes;
    private LocalDateTime fechaCreacion;
    private BigDecimal montoTotal;

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

    public String getDpi() {
        return dpi;
    }

    public void setDpi(String dpi) {
        this.dpi = dpi;
    }

    public Integer getCantidadExamenes() {
        return cantidadExamenes;
    }

    public void setCantidadExamenes(
            Integer cantidadExamenes
    ) {
        this.cantidadExamenes = cantidadExamenes;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(
            LocalDateTime fechaCreacion
    ) {
        this.fechaCreacion = fechaCreacion;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(
            BigDecimal montoTotal
    ) {
        this.montoTotal = montoTotal;
    }
}