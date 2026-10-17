package com.example.proyecto_semestre_medico.service;

public interface PortalPacienteService {

    ResultadoVerificacionDpi verificarDpi(String dpi);

    String obtenerNombrePacientePorDpi(String dpi);

    enum ResultadoVerificacionDpi {
        PACIENTE_REGISTRADO,
        NO_REGISTRADO,
        USUARIO_INTERNO
    }
}