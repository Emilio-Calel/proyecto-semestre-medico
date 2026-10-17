package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.AgendaItemDTO;
import com.example.proyecto_semestre_medico.dto.EventoAgendaFormDTO;
import com.example.proyecto_semestre_medico.dto.TareaMedicoFormDTO;
import com.example.proyecto_semestre_medico.model.EventoAgenda;
import com.example.proyecto_semestre_medico.model.TareaMedico;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendaMedicoService {

    List<AgendaItemDTO> listarAgenda(
            String nombreUsuario,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    EventoAgenda guardarEvento(
            EventoAgendaFormDTO dto,
            String nombreUsuario
    );

    EventoAgendaFormDTO obtenerEvento(
            Long eventoId,
            String nombreUsuario
    );

    void eliminarEvento(
            Long eventoId,
            String nombreUsuario
    );

    List<TareaMedico> listarTareas(
            String nombreUsuario,
            String filtro
    );

    TareaMedico guardarTarea(
            TareaMedicoFormDTO dto,
            String nombreUsuario
    );

    void cambiarEstadoTarea(
            Long tareaId,
            String nombreUsuario
    );

    void eliminarTarea(
            Long tareaId,
            String nombreUsuario
    );
}