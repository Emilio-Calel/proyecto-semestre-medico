package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.CitaRecepcionDTO;
import com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO;
import com.example.proyecto_semestre_medico.dto.EmergenciaRecepcionDTO;

import com.example.proyecto_semestre_medico.dto.ResultadoLlegadaRecepcionDTO;


import java.util.List;
import java.util.Optional;

public interface RecepcionService {

    List<CitaRecepcionDTO> buscar(
            String criterio,
            String tipoBusqueda
    );

    ResultadoLlegadaRecepcionDTO registrarLlegada(
            Long citaId,
            String usuarioRecepcionista
    );

    Optional<String> buscarNombrePacientePorDpi(
            String dpi
    );

    Optional<Long> buscarIdPacientePorDpi(
            String dpi
    );

    CitaRecepcionDTO obtenerParaReasignacion(
            Long citaId
    );

    List<MedicoOpcionDTO> listarMedicosDisponiblesParaReasignacion(
            Long citaId
    );

    void reasignarMedico(
            Long citaId,
            Long medicoId,
            String motivo,
            String usuarioRecepcionista
    );

    CitaRecepcionDTO registrarEmergencia(
            EmergenciaRecepcionDTO dto,
            String usuarioRecepcionista
    );
}