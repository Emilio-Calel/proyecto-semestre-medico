package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;
import java.util.List;
import com.example.proyecto_semestre_medico.model.MovimientoInventarioMedicamento;

public class ResumenMensualInventarioDTO {

    private int anio;
    private int mes;

    private long totalMovimientos;

    private long totalEntradas;
    private long totalSalidas;

    private long compras;
    private long devoluciones;
    private long ventas;
    private long reclamos;
    private long ajustesPositivos;
    private long ajustesNegativos;
    private long despachos;

    private List<MovimientoInventarioMedicamento> movimientos = List.of();

    public List<MovimientoInventarioMedicamento> getMovimientos() { return movimientos; }

    public void setMovimientos(List<MovimientoInventarioMedicamento> movimientos) {
        this.movimientos = List.copyOf(movimientos);
    }

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

    public long getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(
            long totalEntradas
    ) {
        this.totalEntradas =
                totalEntradas;
    }

    public long getTotalSalidas() {
        return totalSalidas;
    }

    public void setTotalSalidas(
            long totalSalidas
    ) {
        this.totalSalidas =
                totalSalidas;
    }

    public long getCompras() {
        return compras;
    }

    public void setCompras(long compras) {
        this.compras = compras;
    }

    public long getDevoluciones() {
        return devoluciones;
    }

    public void setDevoluciones(
            long devoluciones
    ) {
        this.devoluciones =
                devoluciones;
    }

    public long getVentas() {
        return ventas;
    }

    public void setVentas(long ventas) {
        this.ventas = ventas;
    }

    public long getReclamos() {
        return reclamos;
    }

    public void setReclamos(long reclamos) {
        this.reclamos = reclamos;
    }

    public long getAjustesPositivos() {
        return ajustesPositivos;
    }

    public void setAjustesPositivos(
            long ajustesPositivos
    ) {
        this.ajustesPositivos =
                ajustesPositivos;
    }

    public long getAjustesNegativos() {
        return ajustesNegativos;
    }

    public void setAjustesNegativos(
            long ajustesNegativos
    ) {
        this.ajustesNegativos =
                ajustesNegativos;
    }

    public long getDespachos() {
        return despachos;
    }

    public void setDespachos(
            long despachos
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
