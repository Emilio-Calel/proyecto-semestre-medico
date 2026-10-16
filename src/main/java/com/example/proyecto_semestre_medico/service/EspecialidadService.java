package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Especialidad;

import java.util.List;

public interface EspecialidadService {

    /*
     * Este método ya existía y se conserva
     * porque otros módulos lo utilizan.
     */
    List<Especialidad> listarActivas();

    /*
     * También conservamos el método existente.
     */
    Especialidad buscarPorId(Long id);

    /*
     * Nuevos métodos para CU-14.
     */
    List<Especialidad> listarTodas();

    Especialidad guardar(
            Especialidad especialidad,
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