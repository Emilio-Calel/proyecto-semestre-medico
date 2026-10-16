package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Rol;

import java.util.List;

public interface RolService {

    /*
     * Métodos existentes.
     */
    List<Rol> listarActivos();

    List<Rol> listarRolesInternosActivos();

    Rol buscarPorId(Long id);

    /*
     * Métodos CU-14.
     */
    List<Rol> listarTodos();

    Rol guardar(
            Rol rol,
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