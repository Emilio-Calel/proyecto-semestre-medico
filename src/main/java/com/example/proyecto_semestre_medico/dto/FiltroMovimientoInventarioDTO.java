package com.example.proyecto_semestre_medico.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Filtros de lectura; no contiene valores de stock ni campos del movimiento. */
public class FiltroMovimientoInventarioDTO {
    private Long inventarioId;
    private String tipo, estado, medicamento, sucursal, referencia, usuario;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta;
    public Long getInventarioId() { return inventarioId; }
    public void setInventarioId(Long v) { inventarioId = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public String getMedicamento() { return medicamento; }
    public void setMedicamento(String v) { medicamento = v; }
    public String getSucursal() { return sucursal; }
    public void setSucursal(String v) { sucursal = v; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String v) { referencia = v; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String v) { usuario = v; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate v) { desde = v; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate v) { hasta = v; }
}
