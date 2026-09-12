package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.CorreoPendiente;
import com.example.proyecto_semestre_medico.repository.CorreoPendienteRepository;
import com.example.proyecto_semestre_medico.service.CorreoPendienteService;
import com.example.proyecto_semestre_medico.service.CorreoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CorreoPendienteServiceImpl
        implements CorreoPendienteService {

    private final CorreoPendienteRepository correoPendienteRepository;
    private final CorreoService correoService;

    public CorreoPendienteServiceImpl(
            CorreoPendienteRepository correoPendienteRepository,
            CorreoService correoService
    ) {
        this.correoPendienteRepository =
                correoPendienteRepository;

        this.correoService =
                correoService;
    }

    @Override
    @Transactional
    public void registrar(
            String destinatario,
            String asunto,
            String contenido
    ) {

        CorreoPendiente correo =
                new CorreoPendiente();

        correo.setDestinatario(destinatario);
        correo.setAsunto(asunto);
        correo.setContenido(contenido);
        correo.setEstado("PENDIENTE");
        correo.setIntentos(0);
        correo.setMaxIntentos(5);

        /*
         * Puede procesarse inmediatamente.
         */
        correo.setProximoIntento(
                LocalDateTime.now()
        );

        correoPendienteRepository.save(
                correo
        );
    }

    @Override
    @Transactional
    public int procesarPendientes() {

        LocalDateTime ahora =
                LocalDateTime.now();

        List<CorreoPendiente> pendientes =
                correoPendienteRepository
                        .findByEstadoAndProximoIntentoLessThanEqualOrderByFechaCreacionAsc(
                                "PENDIENTE",
                                ahora
                        );

        int enviados = 0;

        for (CorreoPendiente correo : pendientes) {

            try {

                correoService.enviar(
                        correo.getDestinatario(),
                        correo.getAsunto(),
                        correo.getContenido()
                );

                correo.setEstado("ENVIADO");
                correo.setFechaEnvio(
                        LocalDateTime.now()
                );

                correo.setUltimoError(null);
                correo.setProximoIntento(null);

                enviados++;

            } catch (Exception e) {

                int intentos =
                        correo.getIntentos() + 1;

                correo.setIntentos(intentos);

                correo.setUltimoError(
                        e.getMessage()
                );

                if (intentos >=
                        correo.getMaxIntentos()) {

                    correo.setEstado(
                            "FALLIDO"
                    );

                    correo.setProximoIntento(
                            null
                    );

                } else {

                    /*
                     * Reintento en 1 minuto.
                     *
                     * Luego podemos usar una política
                     * exponencial.
                     */
                    correo.setProximoIntento(
                            LocalDateTime.now()
                                    .plusMinutes(1)
                    );
                }
            }

            correoPendienteRepository.save(
                    correo
            );
        }

        return enviados;
    }
}