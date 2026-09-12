package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.SignosVitalesDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.SignosVitales;

import java.util.List;

public interface SignosVitalesService {

    List<Cita> listarPacientesPresentes();

    void iniciarToma(
            Long citaId
    );

    Cita buscarCitaParaSignos(
            Long citaId
    );

    SignosVitales registrar(
            String usuarioEnfermeria,
            SignosVitalesDTO dto
    );
}
