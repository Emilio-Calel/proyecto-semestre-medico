package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;

public class ResumenMensualInventarioDTO {

    private int anio;
    private int mes;

    private long totalMovimientos;

    private int totalEntradas;
    private int totalSalidas;

    private int compras;
    private int devoluciones;
    private int ventas;
    private int reclamos;
    private int ajustesPositivos;
    private int ajustesNegativos;
    private int despachos;

    private BigDecimal valorTotalCompras =
            BigDecimal.ZERO;

    public ResumenMensualInventarioDTO() {
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public int getMes() {
        return mes;
    }

    public void setMes(int mes) {
        this.mes = mes;
    }

    public long getTotalMovimientos() {
        return totalMovimientos;
    }

    public void setTotalMovimientos(
            long totalMovimientos
    ) {
        this.totalMovimientos =
                totalMovimientos;
    }

    public int getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(
            int totalEntradas
    ) {
        this.totalEntradas =
                totalEntradas;
    }

    public int getTotalSalidas() {
        return totalSalidas;
    }

    public void setTotalSalidas(
            int totalSalidas
    ) {
        this.totalSalidas =
                totalSalidas;
    }

    public int getCompras() {
        return compras;
    }

    public void setCompras(int compras) {
        this.compras = compras;
    }

    public int getDevoluciones() {
        return devoluciones;
    }

    public void setDevoluciones(
            int devoluciones
    ) {
        this.devoluciones =
                devoluciones;
    }

    public int getVentas() {
        return ventas;
    }

    public void setVentas(int ventas) {
        this.ventas = ventas;
    }

    public int getReclamos() {
        return reclamos;
    }

    public void setReclamos(int reclamos) {
        this.reclamos = reclamos;
    }

    public int getAjustesPositivos() {
        return ajustesPositivos;
    }

    public void setAjustesPositivos(
            int ajustesPositivos
    ) {
        this.ajustesPositivos =
                ajustesPositivos;
    }

    public int getAjustesNegativos() {
        return ajustesNegativos;
    }

    public void setAjustesNegativos(
            int ajustesNegativos
    ) {
        this.ajustesNegativos =
                ajustesNegativos;
    }

    public int getDespachos() {
        return despachos;
    }

    public void setDespachos(
            int despachos
    ) {
        this.despachos =
                despachos;
    }

    public BigDecimal getValorTotalCompras() {
        return valorTotalCompras;
    }

    public void setValorTotalCompras(
            BigDecimal valorTotalCompras
    ) {
        this.valorTotalCompras =
                valorTotalCompras;
    }
}