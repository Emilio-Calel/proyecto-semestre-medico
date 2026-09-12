package com.example.proyecto_semestre_medico.service;

import org.springframework.web.multipart.MultipartFile;

public interface AntivirusService {

    void validar(MultipartFile archivo);
}