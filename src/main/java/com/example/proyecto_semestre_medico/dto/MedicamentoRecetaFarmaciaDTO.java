package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;

public class MedicamentoRecetaFarmaciaDTO {

    private Long recetaDetalleId;

    private Long medicamentoId;

    private String medicamento;

    private String dosis;

    private String indicaciones;

    private String unidad;

    private Integer cantidadRecetada;

    private Integer cantidadDespachada;

    private Integer cantidadPendiente;

    private Integer stockDisponible;

    private Integer stockMinimo;

    private BigDecimal precioUnitario;

    public MedicamentoRecetaFarmaciaDTO() {
    }

    public Long getRecetaDetalleId() {
        return recetaDetalleId;
    }

    public void setRecetaDetalleId(
            Long recetaDetalleId
    ) {
        this.recetaDetalleId =
                recetaDetalleId;
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

    public String getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(
            String medicamento
    ) {
        this.medicamento =
                medicamento;
    }

    public String getDosis() {
        return dosis;
    }

    public void setDosis(
            String dosis
    ) {
        this.dosis = dosis;
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

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(
            String unidad
    ) {
        this.unidad = unidad;
    }

    public Integer getCantidadRecetada() {
        return cantidadRecetada;
    }

    public void setCantidadRecetada(
            Integer cantidadRecetada
    ) {
        this.cantidadRecetada =
                cantidadRecetada;
    }

    public Integer getCantidadDespachada() {
        return cantidadDespachada;
    }

    public void setCantidadDespachada(
            Integer cantidadDespachada
    ) {
        this.cantidadDespachada =
                cantidadDespachada;
    }

    public Integer getCantidadPendiente() {
        return cantidadPendiente;
    }

    public void setCantidadPendiente(
            Integer cantidadPendiente
    ) {
        this.cantidadPendiente =
                cantidadPendiente;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(
            Integer stockDisponible
    ) {
        this.stockDisponible =
                stockDisponible;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(
            Integer stockMinimo
    ) {
        this.stockMinimo =
                stockMinimo;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(
            BigDecimal precioUnitario
    ) {
        this.precioUnitario =
                precioUnitario;
    }
}