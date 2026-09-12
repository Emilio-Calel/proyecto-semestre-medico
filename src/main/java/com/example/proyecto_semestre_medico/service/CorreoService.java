package com.example.proyecto_semestre_medico.service;

public interface CorreoService {

    void enviar(
            String destinatario,
            String asunto,
            String contenido
    );
}