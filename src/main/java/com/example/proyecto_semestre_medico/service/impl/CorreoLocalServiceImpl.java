package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.service.CorreoService;
import org.springframework.stereotype.Service;

@Service
public class CorreoLocalServiceImpl
        implements CorreoService {

    @Override
    public void enviar(
            String destinatario,
            String asunto,
            String contenido
    ) {

        /*
         * Implementación local para desarrollo.
         * No envía correo real todavía.
         */

        System.out.println(
                "========================================"
        );

        System.out.println(
                ">>> CORREO SIMULADO"
        );

        System.out.println(
                ">>> PARA: " + destinatario
        );

        System.out.println(
                ">>> ASUNTO: " + asunto
        );

        System.out.println(
                contenido
        );

        System.out.println(
                "========================================"
        );
    }
}