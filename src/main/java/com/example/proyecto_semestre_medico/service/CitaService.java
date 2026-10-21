package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.AgendarCitaDTO;
import com.example.proyecto_semestre_medico.model.Cita;

public interface CitaService {

    Cita agendar(
            String nombreUsuarioPaciente,
            AgendarCitaDTO dto
    );

    Cita obtenerParaReagendar(
            Long citaId,
            String nombreUsuarioPaciente
    );
}