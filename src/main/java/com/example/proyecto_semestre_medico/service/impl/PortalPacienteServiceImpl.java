package com.example.proyecto_semestre_medico.service.impl;

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

        if (dpi == null
                || !dpi.trim()
                .matches("^[0-9]{13}$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener exactamente 13 dígitos"
            );
        }

        String dpiNormalizado =
                dpi.trim();

        String dpiHash =
                datosSensiblesService
                        .generarHashBusqueda(
                                dpiNormalizado
                        );

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
}