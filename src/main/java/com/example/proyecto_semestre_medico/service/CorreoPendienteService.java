package com.example.proyecto_semestre_medico.service;

public interface CorreoPendienteService {

    void registrar(
            String destinatario,
            String asunto,
            String contenido
    );

    int procesarPendientes();
}