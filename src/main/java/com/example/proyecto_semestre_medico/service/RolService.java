package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Rol;

import java.util.List;

public interface RolService {

    List<Rol> listarActivos();
    List<Rol> listarRolesInternosActivos();

    Rol buscarPorId(Long id);
}