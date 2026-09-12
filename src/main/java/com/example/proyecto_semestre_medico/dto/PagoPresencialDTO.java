package com.example.proyecto_semestre_medico.dto;

public class PagoPresencialDTO {

    private Long citaId;

    private String metodoPago;

    /*
     * Solo se usan cuando el pago
     * presencial es con tarjeta.
     */
    private String numeroTarjeta;
    private String nombreTitular;
    private String expiracion;
    private String cvv;

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

    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(
            String numeroTarjeta
    ) {
        this.numeroTarjeta = numeroTarjeta;
    }

    public String getNombreTitular() {
        return nombreTitular;
    }

    public void setNombreTitular(
            String nombreTitular
    ) {
        this.nombreTitular = nombreTitular;
    }

    public String getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(
            String expiracion
    ) {
        this.expiracion = expiracion;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(
            String cvv
    ) {
        this.cvv = cvv;
    }
}