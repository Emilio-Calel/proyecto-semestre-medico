package com.example.proyecto_semestre_medico.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class ResultadoLaboratorioFormDTO {

    private Long detalleId;
    private Long ordenId;

    private String valorResultado;

    private String unidad;

    @DateTimeFormat(
            pattern = "yyyy-MM-dd'T'HH:mm"
    )
    private LocalDateTime fechaResultado;

    private Boolean fueraRango = false;

    private String notas;

    public Long getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(Long detalleId) {
        this.detalleId = detalleId;
    }

    public String getValorResultado() {
        return valorResultado;
    }

    public void setValorResultado(String valorResultado) {
        this.valorResultado = valorResultado;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public LocalDateTime getFechaResultado() {
        return fechaResultado;
    }

    public void setFechaResultado(
            LocalDateTime fechaResultado
    ) {
        this.fechaResultado = fechaResultado;
    }

    public Boolean getFueraRango() {
        return fueraRango;
    }

    public void setFueraRango(Boolean fueraRango) {
        this.fueraRango = fueraRango;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
    }
}