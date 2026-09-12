package com.example.proyecto_semestre_medico.service;

public interface AutenticacionService {

    void registrarIntentoFallido(String nombreUsuario);

    void registrarLoginExitoso(String nombreUsuario);
}
