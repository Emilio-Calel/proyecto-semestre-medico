package com.example.proyecto_semestre_medico.service;

public interface DatosSensiblesService {

    String cifrar(String valor);

    String descifrar(String valorCifrado);

    String generarHashBusqueda(String valor);
}