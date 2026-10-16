package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Laboratorio;

import java.util.List;

public interface LaboratorioCatalogoService {

    List<Laboratorio> listarTodos();

    List<Laboratorio> listarActivos();

    Laboratorio buscarPorId(Long id);

    Laboratorio guardar(
            Laboratorio laboratorio,
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