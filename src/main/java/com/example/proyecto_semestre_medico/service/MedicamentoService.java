package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Medicamento;

import java.util.List;

public interface MedicamentoService {

    List<Medicamento> listarTodos();

    List<Medicamento> listarActivos();

    Medicamento buscarPorId(Long id);

    Medicamento guardar(
            Medicamento medicamento,
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