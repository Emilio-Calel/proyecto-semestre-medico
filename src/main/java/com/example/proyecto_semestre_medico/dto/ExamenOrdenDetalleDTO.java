package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamenOrdenDetalleDTO {

    private Long detalleId;

    private String codigo;
    private String nombre;

    private BigDecimal precio;

    private String unidadReferencia;
    private String rangoReferencia;

    private String valorResultado;
    private String unidad;
    private LocalDateTime fechaResultado;

    private Boolean fueraRango;
    private String notasResultado;

    private Boolean publicado;

    public Long getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(Long detalleId) {
        this.detalleId = detalleId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getUnidadReferencia() {
        return unidadReferencia;
    }

    public void setUnidadReferencia(String unidadReferencia) {
        this.unidadReferencia = unidadReferencia;
    }

    public String getRangoReferencia() {
        return rangoReferencia;
    }

    public void setRangoReferencia(String rangoReferencia) {
        this.rangoReferencia = rangoReferencia;
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

    public void setFechaResultado(LocalDateTime fechaResultado) {
        this.fechaResultado = fechaResultado;
    }

    public Boolean getFueraRango() {
        return fueraRango;
    }

    public void setFueraRango(Boolean fueraRango) {
        this.fueraRango = fueraRango;
    }

    public String getNotasResultado() {
        return notasResultado;
    }

    public void setNotasResultado(String notasResultado) {
        this.notasResultado = notasResultado;
    }

    public Boolean getPublicado() {
        return publicado;
    }

    public void setPublicado(Boolean publicado) {
        this.publicado = publicado;
    }
}