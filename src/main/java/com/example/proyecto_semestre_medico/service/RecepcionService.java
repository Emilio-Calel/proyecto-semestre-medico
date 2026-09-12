package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.CitaRecepcionDTO;

import java.util.List;

public interface RecepcionService {

    List<CitaRecepcionDTO> buscar(
            String criterio
    );

    void registrarLlegada(
            Long citaId,
            String usuarioRecepcionista
    );
}