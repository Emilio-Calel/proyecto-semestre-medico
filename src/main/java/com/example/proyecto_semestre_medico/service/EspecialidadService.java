package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Especialidad;

import java.util.List;

public interface EspecialidadService {

    List<Especialidad> listarActivas();

    Especialidad buscarPorId(Long id);
}