package com.example.proyecto_semestre_medico.dto;

import java.time.LocalDateTime;

public class HorarioDisponibleDTO {

    private LocalDateTime inicio;
    private LocalDateTime fin;
    private String etiqueta;

    public HorarioDisponibleDTO(
            LocalDateTime inicio,
            LocalDateTime fin,
            String etiqueta
    ) {
        this.inicio = inicio;
        this.fin = fin;
        this.etiqueta = etiqueta;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}