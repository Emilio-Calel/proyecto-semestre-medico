package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.EstadoCita;

import java.util.List;

public interface EstadoCitaService {

    List<EstadoCita> listarActivos();

    EstadoCita buscarPorCodigo(String codigo);
}