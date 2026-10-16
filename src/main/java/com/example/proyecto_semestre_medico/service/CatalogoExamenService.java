package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.CatalogoExamen;
import com.example.proyecto_semestre_medico.model.Laboratorio;

import java.util.List;

public interface CatalogoExamenService {

    List<CatalogoExamen> listarTodos();

    CatalogoExamen buscarPorId(Long id);

    List<Laboratorio> listarLaboratoriosActivos();

    CatalogoExamen guardar(
            CatalogoExamen examen,
            Long laboratorioId,
            String nombreUsuario
    );

    void desactivar(
            Long id,
            String nombreUsuario
    );

    void reactivar(
            Long id,
            String nombreUsuario
    );
}