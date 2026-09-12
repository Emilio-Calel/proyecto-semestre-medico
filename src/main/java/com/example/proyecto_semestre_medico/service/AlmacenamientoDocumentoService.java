package com.example.proyecto_semestre_medico.service;

import org.springframework.web.multipart.MultipartFile;

public interface AlmacenamientoDocumentoService {

    ResultadoAlmacenamiento guardar(
            MultipartFile archivo
    );

    record ResultadoAlmacenamiento(
            String nombreAlmacenado,
            String rutaArchivo
    ) {
    }
}