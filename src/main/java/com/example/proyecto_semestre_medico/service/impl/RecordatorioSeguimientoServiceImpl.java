package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.service.CorreoPendienteService;
import com.example.proyecto_semestre_medico.service.CorreoSeguimientoPlantilla;
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
    private final CorreoSeguimientoPlantilla plantillaCorreo;

    public RecordatorioSeguimientoServiceImpl(
            CitaRepository citaRepository,
            CorreoPendienteService correoPendienteService,
            CorreoSeguimientoPlantilla plantillaCorreo
    ) {

        this.citaRepository =
                citaRepository;

        this.correoPendienteService =
                correoPendienteService;
        this.plantillaCorreo = plantillaCorreo;
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
         * usamos la ventana documentada de 24 a 48 horas.
         */
        LocalDateTime desde =
                ahora.plusHours(24);

        LocalDateTime hasta =
                ahora.plusHours(48);

        List<Long> seguimientos =
                citaRepository
                        .buscarSeguimientosParaRecordatorio(
                                desde,
                                hasta
                        );

        int registrados = 0;

        for (Long candidataId : seguimientos) {

            // La consulta inicial devuelve IDs: la lectura bajo bloqueo debe cargar el estado actual.
            Cita cita = citaRepository.buscarParaSeguimiento(candidataId).orElse(null);
            if (cita == null || Boolean.TRUE.equals(cita.getRecordatorioSeguimientoEnviado())
                    || !"SEGUIMIENTO".equals(cita.getOrigen())
                    || cita.getFechaHoraInicio().isBefore(desde) || !cita.getFechaHoraInicio().isBefore(hasta)
                    || cita.getPaciente().getCorreo() == null || cita.getPaciente().getCorreo().isBlank()) continue;

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

    private void registrarCorreo(Cita cita) {
        correoPendienteService.registrarRecordatorio(cita,
                "Recordatorio: Su Cita de Seguimiento Mañana", plantillaCorreo.contenido(cita, true));
    }
}
