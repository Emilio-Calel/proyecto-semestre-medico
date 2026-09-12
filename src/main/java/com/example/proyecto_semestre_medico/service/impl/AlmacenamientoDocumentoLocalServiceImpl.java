package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.service.AlmacenamientoDocumentoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class AlmacenamientoDocumentoLocalServiceImpl
        implements AlmacenamientoDocumentoService {

    private final Path directorioBase;

    public AlmacenamientoDocumentoLocalServiceImpl(
            @Value("${app.archivos.directorio:uploads/citas}")
            String directorio
    ) {

        this.directorioBase =
                Path.of(directorio)
                        .toAbsolutePath()
                        .normalize();

        try {

            Files.createDirectories(
                    directorioBase
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "No fue posible crear el directorio de archivos",
                    e
            );
        }
    }

    @Override
    public ResultadoAlmacenamiento guardar(
            MultipartFile archivo
    ) {

        String nombreAlmacenado =
                UUID.randomUUID()
                        + ".pdf";

        Path destino =
                directorioBase.resolve(
                        nombreAlmacenado
                );

        try {

            Files.copy(
                    archivo.getInputStream(),
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "No fue posible almacenar el documento",
                    e
            );
        }

        return new ResultadoAlmacenamiento(
                nombreAlmacenado,
                destino.toString()
        );
    }
}