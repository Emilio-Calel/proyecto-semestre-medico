package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.RecetaFormDTO;
import com.example.proyecto_semestre_medico.model.Medicamento;
import com.example.proyecto_semestre_medico.model.Receta;

import java.util.List;

public interface RecetaService {

    Receta crearReceta(
            RecetaFormDTO dto,
            String nombreUsuario
    );

    Receta buscarPorId(
            Long id
    );

    List<Receta> listarTodas();

    List<Medicamento> listarMedicamentosActivos();

    Receta marcarComoNoAdquirida(
            Long recetaId,
            String nombreUsuario
    );
}