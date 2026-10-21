package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.AgendarCitaDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import java.util.List;

public interface CitaService {

    Cita agendar(
            String nombreUsuarioPaciente,
            AgendarCitaDTO dto
    );

    Cita obtenerParaReagendar(
            Long citaId,
            String nombreUsuarioPaciente
    );

    List<Cita> listarMisCitas(
            String nombreUsuarioPaciente
    );
}