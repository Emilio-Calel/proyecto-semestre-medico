package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;

public class PagoPresencialDTO {

    private Long citaId;
    private String metodoPago;
    private BigDecimal montoRecibido;
    private String ultimos4;

    public PagoPresencialDTO() {
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(
            String metodoPago
    ) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(
            BigDecimal montoRecibido
    ) {
        this.montoRecibido = montoRecibido;
    }

    public String getUltimos4() {
        return ultimos4;
    }

    public void setUltimos4(
            String ultimos4
    ) {
        this.ultimos4 = ultimos4;
    }
}