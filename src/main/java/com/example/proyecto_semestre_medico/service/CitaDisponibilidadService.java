package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.HorarioDisponibleDTO;

import java.time.LocalDate;
import java.util.List;

public interface CitaDisponibilidadService {

    List<HorarioDisponibleDTO> obtenerHorarios(
            Long medicoId,
            LocalDate fecha
    );
}