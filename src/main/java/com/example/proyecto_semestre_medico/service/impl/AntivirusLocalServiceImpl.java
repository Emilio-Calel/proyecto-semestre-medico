package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.service.AntivirusService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AntivirusLocalServiceImpl
        implements AntivirusService {

    @Override
    public void validar(
            MultipartFile archivo
    ) {

        /*
         * Implementación local temporal.
         *
         * En producción se reemplazará por
         * un motor antivirus real.
         */

        if (archivo == null
                || archivo.isEmpty()) {

            throw new IllegalArgumentException(
                    "El archivo está vacío"
            );
        }
    }
}