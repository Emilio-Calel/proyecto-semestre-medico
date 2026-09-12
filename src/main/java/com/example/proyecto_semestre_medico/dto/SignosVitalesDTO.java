package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;

public class SignosVitalesDTO {

    private Long citaId;

    private Integer presionSistolica;
    private Integer presionDiastolica;

    private Integer pulso;

    private BigDecimal temperatura;
    private BigDecimal peso;
    private BigDecimal talla;

    private String observaciones;

    public SignosVitalesDTO() {
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public Integer getPresionSistolica() {
        return presionSistolica;
    }

    public void setPresionSistolica(
            Integer presionSistolica
    ) {
        this.presionSistolica =
                presionSistolica;
    }

    public Integer getPresionDiastolica() {
        return presionDiastolica;
    }

    public void setPresionDiastolica(
            Integer presionDiastolica
    ) {
        this.presionDiastolica =
                presionDiastolica;
    }

    public Integer getPulso() {
        return pulso;
    }

    public void setPulso(Integer pulso) {
        this.pulso = pulso;
    }

    public BigDecimal getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(
            BigDecimal temperatura
    ) {
        this.temperatura = temperatura;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public BigDecimal getTalla() {
        return talla;
    }

    public void setTalla(BigDecimal talla) {
        this.talla = talla;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones
    ) {
        this.observaciones =
                observaciones;
    }
}