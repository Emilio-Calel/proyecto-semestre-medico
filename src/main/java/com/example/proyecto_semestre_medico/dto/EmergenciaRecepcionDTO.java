package com.example.proyecto_semestre_medico.dto;

public class EmergenciaRecepcionDTO {

    private String nombreCompleto;
    private String dpi;

    public EmergenciaRecepcionDTO() {
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(
            String nombreCompleto
    ) {
        this.nombreCompleto =
                nombreCompleto;
    }

    public String getDpi() {
        return dpi;
    }

    public void setDpi(
            String dpi
    ) {
        this.dpi =
                dpi;
    }
}