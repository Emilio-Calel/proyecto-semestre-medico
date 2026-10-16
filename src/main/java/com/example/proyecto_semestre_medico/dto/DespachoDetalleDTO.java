package com.example.proyecto_semestre_medico.dto;

public class DespachoDetalleDTO {

    private Long recetaDetalleId;

    private Integer cantidad;

    private Long medicamentoDespachadoId;

    private String motivoSustitucion;

    public DespachoDetalleDTO() {
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

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(
            Integer cantidad
    ) {
        this.cantidad = cantidad;
    }

    public Long getMedicamentoDespachadoId() {
        return medicamentoDespachadoId;
    }

    public void setMedicamentoDespachadoId(
            Long medicamentoDespachadoId
    ) {
        this.medicamentoDespachadoId =
                medicamentoDespachadoId;
    }

    public String getMotivoSustitucion() {
        return motivoSustitucion;
    }

    public void setMotivoSustitucion(
            String motivoSustitucion
    ) {
        this.motivoSustitucion =
                motivoSustitucion;
    }
}