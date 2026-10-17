package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.service.CorreoPendienteService;
import com.example.proyecto_semestre_medico.service.RecordatorioSeguimientoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RecordatorioSeguimientoServiceImpl
        implements RecordatorioSeguimientoService {

    private final CitaRepository citaRepository;

    private final CorreoPendienteService
            correoPendienteService;

    public RecordatorioSeguimientoServiceImpl(
            CitaRepository citaRepository,
            CorreoPendienteService correoPendienteService
    ) {

        this.citaRepository =
                citaRepository;

        this.correoPendienteService =
                correoPendienteService;
    }

    @Override
    @Transactional
    public int procesarRecordatorios() {

        LocalDateTime ahora =
                LocalDateTime.now();

        /*
         * Recordamos aproximadamente un día antes.
         *
         * Como el scheduler corre periódicamente,
         * usamos una ventana de 24 a 25 horas.
         */
        LocalDateTime desde =
                ahora.plusHours(24);

        LocalDateTime hasta =
                ahora.plusHours(25);

        List<Cita> seguimientos =
                citaRepository
                        .buscarSeguimientosParaRecordatorio(
                                desde,
                                hasta
                        );

        int registrados = 0;

        for (Cita cita : seguimientos) {

            /*
             * Segunda validación defensiva.
             */
            String estado =
                    cita.getEstadoCita()
                            .getCodigo();

            if ("CANCELADA".equals(estado)
                    || "EXPIRADA".equals(estado)
                    || "NO_ASISTIO".equals(estado)) {

                continue;
            }

            registrarCorreo(
                    cita
            );

            cita.setRecordatorioSeguimientoEnviado(
                    true
            );

            cita.setFechaRecordatorioSeguimiento(
                    LocalDateTime.now()
            );

            citaRepository.save(
                    cita
            );

            registrados++;
        }

        return registrados;
    }

    private void registrarCorreo(
            Cita cita
    ) {

        String destinatario =
                cita.getPaciente()
                        .getCorreo();

        if (destinatario == null
                || destinatario.isBlank()) {

            return;
        }

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm"
                );

        String tipo =
                switch (
                        cita.getTipoSeguimiento()
                        ) {

                    case "MONITOREO_TRATAMIENTO" ->
                            "Monitoreo de Tratamiento";

                    case "REVISION_RESULTADOS_LABORATORIO" ->
                            "Revisión de Resultados de Laboratorio";

                    default ->
                            cita.getTipoSeguimiento();
                };

        String asunto =
                "Recordatorio de seguimiento - "
                        + cita.getNumeroCita();

        String contenido =
                """
                Sistema Informático Hospitalario

                Recordatorio de cita de seguimiento.

                Paciente: %s
                Número de cita: %s
                Tipo de seguimiento: %s
                Médico: %s
                Especialidad: %s
                Sucursal: %s
                Fecha y hora: %s

                Motivo:
                %s

                Le recordamos presentarse con anticipación
                a su cita programada.

                Este es un mensaje generado automáticamente
                por el sistema hospitalario.
                """
                        .formatted(
                                cita.getPaciente()
                                        .getNombreCompleto(),

                                cita.getNumeroCita(),

                                tipo,

                                cita.getMedico()
                                        .getPersonal()
                                        .getNombreCompleto(),

                                cita.getEspecialidad()
                                        .getNombre(),

                                cita.getSucursal()
                                        .getNombre(),

                                cita.getFechaHoraInicio()
                                        .format(formato),

                                cita.getMotivoConsulta()
                        );

        correoPendienteService.registrar(
                destinatario,
                asunto,
                contenido
        );
    }
}
