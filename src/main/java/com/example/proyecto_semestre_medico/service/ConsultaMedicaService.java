package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.ConsultaMedicaFormDTO;
import com.example.proyecto_semestre_medico.dto.MedicoCitaPanelDTO;

import java.util.List;

public interface ConsultaMedicaService {

    List<MedicoCitaPanelDTO> listarEnEspera(String nombreUsuario);

    List<MedicoCitaPanelDTO> listarEnConsulta(String nombreUsuario);

    List<MedicoCitaPanelDTO> listarPendientesCierre(String nombreUsuario);

    void iniciarConsulta(Long citaId, String nombreUsuario);

    ConsultaMedicaFormDTO obtenerConsulta(Long citaId, String nombreUsuario);

    void guardarConsulta(
            ConsultaMedicaFormDTO dto,
            String nombreUsuario
    );

    String marcarNoAsistio(
            Long citaId,
            String nombreUsuario
    );

    String finalizarAtencion(
            Long citaId,
            String nombreUsuario
    );
}
