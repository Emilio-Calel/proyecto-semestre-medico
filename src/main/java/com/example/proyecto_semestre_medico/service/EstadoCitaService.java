package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.EstadoCita;

import java.util.List;

public interface EstadoCitaService {

    /*
     * Métodos existentes.
     */
    List<EstadoCita> listarActivos();

    EstadoCita buscarPorCodigo(String codigo);

    /*
     * Métodos CU-14.
     */
    List<EstadoCita> listarTodos();

    EstadoCita buscarPorId(Long id);

    EstadoCita guardar(
            EstadoCita estadoCita,
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