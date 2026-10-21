package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.service.AntivirusService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class AntivirusLocalServiceImpl
        implements AntivirusService {

    private static final String MENSAJE_DOCUMENTO_INVALIDO =
            "El documento debe ser un archivo PDF válido, no encriptado y con tamaño máximo de 2 MB.";

    @Override
    public void validar(
            MultipartFile archivo
    ) {

        if (archivo == null
                || archivo.isEmpty()) {

            throw new IllegalArgumentException(
                    MENSAJE_DOCUMENTO_INVALIDO
            );
        }

        try {

            byte[] contenido =
                    archivo.getBytes();

            // ============================================
            // VALIDAR CABECERA REAL DE PDF
            // ============================================

            if (contenido.length < 5
                    || contenido[0] != '%'
                    || contenido[1] != 'P'
                    || contenido[2] != 'D'
                    || contenido[3] != 'F'
                    || contenido[4] != '-') {

                throw new IllegalArgumentException(
                        MENSAJE_DOCUMENTO_INVALIDO
                );
            }

            // ============================================
            // VALIDAR QUE NO ESTÉ ENCRIPTADO
            // ============================================

            String textoPdf =
                    new String(
                            contenido,
                            StandardCharsets.ISO_8859_1
                    );

            if (textoPdf.contains("/Encrypt")) {

                throw new IllegalArgumentException(
                        MENSAJE_DOCUMENTO_INVALIDO
                );
            }

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    MENSAJE_DOCUMENTO_INVALIDO,
                    e
            );
        }
    }
}