package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.CorreoPendiente;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
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
    private final CitaRepository citaRepository;

    public CorreoPendienteServiceImpl(
            CorreoPendienteRepository correoPendienteRepository,
            CorreoService correoService,
            CitaRepository citaRepository
    ) {
        this.correoPendienteRepository =
                correoPendienteRepository;

        this.correoService =
                correoService;
        this.citaRepository = citaRepository;
    }

    @Override
    @Transactional
    public void registrar(
            String destinatario,
            String asunto,
            String contenido
    ) {

        registrarCorreo(destinatario, asunto, contenido, null);
    }

    @Override
    @Transactional
    public void registrarRecordatorio(Cita cita, String asunto, String contenido) {
        registrarCorreo(cita.getPaciente().getCorreo(), asunto, contenido, cita);
    }

    private void registrarCorreo(String destinatario, String asunto, String contenido, Cita cita) {

        CorreoPendiente correo =
                new CorreoPendiente();

        correo.setDestinatario(destinatario);
        correo.setAsunto(asunto);
        correo.setContenido(contenido);
        correo.setEstado("PENDIENTE");
        correo.setIntentos(0);
        correo.setMaxIntentos(5);
        correo.setCitaRecordatorio(cita);

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

                if (correo.getCitaRecordatorio() != null) {
                    Cita cita = citaRepository.buscarParaSeguimiento(correo.getCitaRecordatorio().getId()).orElse(null);
                    if (cita == null || java.util.Set.of("CANCELADA", "EXPIRADA", "NO_ASISTIO")
                            .contains(cita.getEstadoCita().getCodigo())
                            || cita.getFechaHoraInicio().isBefore(ahora.plusHours(24))
                            || cita.getFechaHoraInicio().isAfter(ahora.plusHours(48))) {
                        correo.setEstado("FALLIDO"); correo.setProximoIntento(null);
                        correo.setUltimoError("Recordatorio omitido: cita cancelada o fuera de la ventana de 1 a 2 días.");
                        correoPendienteRepository.save(correo);
                        continue;
                    }
                }

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
