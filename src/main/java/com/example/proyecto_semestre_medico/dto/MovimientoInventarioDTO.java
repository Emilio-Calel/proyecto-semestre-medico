package com.example.proyecto_semestre_medico.dto;

import com.example.proyecto_semestre_medico.model.TipoMovimientoInventario;

import java.math.BigDecimal;

public class MovimientoInventarioDTO {

    private Long inventarioId;

    private TipoMovimientoInventario tipoMovimiento;

    private Integer cantidad;

    private BigDecimal costoUnitario;

    private String referencia;

    private String motivo;

    public MovimientoInventarioDTO() {
    }

    public Long getInventarioId() {
        return inventarioId;
    }

    public void setInventarioId(
            Long inventarioId
    ) {
        this.inventarioId =
                inventarioId;
    }

    public TipoMovimientoInventario
    getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(
            TipoMovimientoInventario tipoMovimiento
    ) {
        this.tipoMovimiento =
                tipoMovimiento;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(
            Integer cantidad
    ) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(
            BigDecimal costoUnitario
    ) {
        this.costoUnitario =
                costoUnitario;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(
            String referencia
    ) {
        this.referencia =
                referencia;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(
            String motivo
    ) {
        this.motivo =
                motivo;
    }
}