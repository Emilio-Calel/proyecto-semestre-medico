package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Sucursal;

import java.util.List;

public interface SucursalService {

    List<Sucursal> listarActivas();

    Sucursal buscarPorId(Long id);
}