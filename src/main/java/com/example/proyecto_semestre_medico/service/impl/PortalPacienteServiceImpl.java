package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Paciente;
import com.example.proyecto_semestre_medico.repository.PacienteRepository;
import com.example.proyecto_semestre_medico.repository.PersonalRepository;
import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import com.example.proyecto_semestre_medico.service.PortalPacienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalPacienteServiceImpl
        implements PortalPacienteService {

    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final DatosSensiblesService datosSensiblesService;

    public PortalPacienteServiceImpl(
            PacienteRepository pacienteRepository,
            PersonalRepository personalRepository,
            DatosSensiblesService datosSensiblesService
    ) {
        this.pacienteRepository = pacienteRepository;
        this.personalRepository = personalRepository;
        this.datosSensiblesService = datosSensiblesService;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoVerificacionDpi verificarDpi(
            String dpi
    ) {

        String dpiHash =
                obtenerHashDpiValido(dpi);

        /*
         * Primero revisamos personal interno.
         */
        if (personalRepository
                .existsByDpiHash(dpiHash)) {

            return ResultadoVerificacionDpi
                    .USUARIO_INTERNO;
        }

        /*
         * Después pacientes.
         */
        if (pacienteRepository
                .existsByDpiHash(dpiHash)) {

            return ResultadoVerificacionDpi
                    .PACIENTE_REGISTRADO;
        }

        return ResultadoVerificacionDpi
                .NO_REGISTRADO;
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerNombrePacientePorDpi(
            String dpi
    ) {

        String dpiHash =
                obtenerHashDpiValido(dpi);

        Paciente paciente =
                pacienteRepository
                        .findByDpiHash(dpiHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No se encontró un registro asociado a este DPI."
                                )
                        );

        return paciente.getNombreCompleto();
    }

    private String obtenerHashDpiValido(
            String dpi
    ) {

        /*
         * RN-GLOBAL-001:
         * campo obligatorio.
         */
        if (dpi == null
                || dpi.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "El campo DPI es obligatorio. Por favor, ingrese su número de DPI."
            );
        }

        String dpiNormalizado =
                dpi.trim();

        /*
         * RN-GLOBAL-001:
         * únicamente números.
         */
        if (!dpiNormalizado.matches("^[0-9]+$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales."
            );
        }

        /*
         * RN-GLOBAL-001:
         * exactamente 13 dígitos.
         */
        if (dpiNormalizado.length() != 13) {

            throw new IllegalArgumentException(
                    "El DPI debe contener exactamente 13 dígitos. Usted ingresó "
                            + dpiNormalizado.length()
                            + " dígitos."
            );
        }

        return datosSensiblesService
                .generarHashBusqueda(
                        dpiNormalizado
                );
    }
}