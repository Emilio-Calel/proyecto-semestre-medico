package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.ExamenOpcionDTO;
import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.OrdenLaboratorio;

import java.util.List;

public interface OrdenLaboratorioService {

    List<ExamenOpcionDTO> listarExamenesActivos();

    OrdenLaboratorio crearOrden(
            OrdenLaboratorioFormDTO dto,
            String nombreUsuario
    );
}