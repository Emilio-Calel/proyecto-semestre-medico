package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.HorarioDisponibleDTO;
import com.example.proyecto_semestre_medico.dto.SeguimientoCitaDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.dto.SeguimientoContextoDTO;

import java.time.LocalDate;
import java.util.List;

public interface SeguimientoCitaService {

    List<HorarioDisponibleDTO> obtenerHorariosDisponibles(
            Long citaOrigenId,
            LocalDate fecha,
            String nombreUsuario
    );

    Cita crearSeguimiento(
            SeguimientoCitaDTO dto,
            String nombreUsuario
    );

    SeguimientoContextoDTO obtenerContexto(
            Long citaOrigenId,
            String nombreUsuario
    );
}