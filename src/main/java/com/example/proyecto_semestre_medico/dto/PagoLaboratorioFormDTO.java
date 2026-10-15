package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;

public class PagoLaboratorioFormDTO {

    private Long ordenId;

    private String metodoPago;

    /*
     * EFECTIVO
     */
    private BigDecimal montoRecibido;

    /*
     * VISA / MASTERCARD / DEBITO
     *
     * El CU-10 solicita únicamente
     * los últimos cuatro dígitos.
     */
    private String ultimos4;

    public Long getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
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