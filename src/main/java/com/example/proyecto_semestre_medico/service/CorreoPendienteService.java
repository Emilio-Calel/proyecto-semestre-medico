package com.example.proyecto_semestre_medico.service;

public interface CorreoPendienteService {

    void registrarRecordatorio(com.example.proyecto_semestre_medico.model.Cita cita,
                              String asunto, String contenido);

    void registrar(
            String destinatario,
            String asunto,
            String contenido
    );

    int procesarPendientes();
}
