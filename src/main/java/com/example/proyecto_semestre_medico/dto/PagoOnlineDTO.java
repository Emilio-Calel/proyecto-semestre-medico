package com.example.proyecto_semestre_medico.dto;

public class PagoOnlineDTO {

    private Long citaId;

    private String numeroTarjeta;
    private String nombreTitular;
    private String expiracion;
    private String cvv;

    private String claveIdempotencia;

    public PagoOnlineDTO() {
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(
            String numeroTarjeta
    ) {
        this.numeroTarjeta =
                numeroTarjeta;
    }

    public String getNombreTitular() {
        return nombreTitular;
    }

    public void setNombreTitular(
            String nombreTitular
    ) {
        this.nombreTitular =
                nombreTitular;
    }

    public String getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(
            String expiracion
    ) {
        this.expiracion =
                expiracion;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(
            String claveIdempotencia
    ) {
        this.claveIdempotencia =
                claveIdempotencia;
    }
}