package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.Cie10DTO;

import java.util.List;

public interface Cie10Service {

    List<Cie10DTO> buscar(String texto);
}