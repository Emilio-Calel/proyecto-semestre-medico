package com.example.proyecto_semestre_medico.dto;

import java.util.ArrayList;
import java.util.List;

public class DespachoMedicamentoDTO {

    private Long recetaId;

    private Long sucursalId;

    private List<DespachoDetalleDTO> detalles =
            new ArrayList<>();

    public DespachoMedicamentoDTO() {
    }

    public Long getRecetaId() {
        return recetaId;
    }

    public void setRecetaId(
            Long recetaId
    ) {
        this.recetaId = recetaId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(
            Long sucursalId
    ) {
        this.sucursalId = sucursalId;
    }

    public List<DespachoDetalleDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(
            List<DespachoDetalleDTO> detalles
    ) {
        this.detalles = detalles;
    }
}