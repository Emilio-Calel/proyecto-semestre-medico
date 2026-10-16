package com.example.proyecto_semestre_medico.dto;

import java.util.ArrayList;
import java.util.List;

public class RecetaFormDTO {

    private Long citaId;

    private String observaciones;

    private List<RecetaDetalleFormDTO> detalles =
            new ArrayList<>();

    public RecetaFormDTO() {
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(
            Long citaId
    ) {
        this.citaId =
                citaId;
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

    public List<RecetaDetalleFormDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(
            List<RecetaDetalleFormDTO> detalles
    ) {
        this.detalles =
                detalles;
    }
}