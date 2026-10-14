package com.example.proyecto_semestre_medico.dto;

import java.util.ArrayList;
import java.util.List;

public class OrdenLaboratorioFormDTO {

    private Long citaId;

    private List<Long> examenesIds =
            new ArrayList<>();

    private Boolean externa = false;

    private String notas;

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public List<Long> getExamenesIds() {
        return examenesIds;
    }

    public void setExamenesIds(
            List<Long> examenesIds
    ) {
        this.examenesIds = examenesIds;
    }

    public Boolean getExterna() {
        return externa;
    }

    public void setExterna(Boolean externa) {
        this.externa = externa;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}