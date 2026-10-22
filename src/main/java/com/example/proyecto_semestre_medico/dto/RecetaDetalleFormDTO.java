package com.example.proyecto_semestre_medico.dto;

public class RecetaDetalleFormDTO {

    private Long medicamentoId;

    private String dosis;

    private Integer cantidad;

    private String frecuencia;
    private String duracion;

    private String indicaciones;

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }
    public String getDuracion() { return duracion; }
    public void setDuracion(String duracion) { this.duracion = duracion; }

    public RecetaDetalleFormDTO() {
    }

    public Long getMedicamentoId() {
        return medicamentoId;
    }

    public void setMedicamentoId(
            Long medicamentoId
    ) {
        this.medicamentoId =
                medicamentoId;
    }

    public String getDosis() {
        return dosis;
    }

    public void setDosis(
            String dosis
    ) {
        this.dosis =
                dosis;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(
            Integer cantidad
    ) {
        this.cantidad =
                cantidad;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(
            String indicaciones
    ) {
        this.indicaciones =
                indicaciones;
    }
}