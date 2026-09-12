package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.HorarioDisponibleDTO;
import com.example.proyecto_semestre_medico.model.HorarioMedico;
import com.example.proyecto_semestre_medico.model.Medico;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.EventoAgendaRepository;
import com.example.proyecto_semestre_medico.repository.HorarioMedicoRepository;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import com.example.proyecto_semestre_medico.service.CitaDisponibilidadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CitaDisponibilidadServiceImpl
        implements CitaDisponibilidadService {

    private final HorarioMedicoRepository horarioMedicoRepository;
    private final EventoAgendaRepository eventoAgendaRepository;
    private final CitaRepository citaRepository;
    private final MedicoRepository medicoRepository;

    private final int duracionMinutos;

    public CitaDisponibilidadServiceImpl(
            HorarioMedicoRepository horarioMedicoRepository,
            EventoAgendaRepository eventoAgendaRepository,
            CitaRepository citaRepository,
            MedicoRepository medicoRepository,
            @Value("${app.citas.duracion-minutos:30}")
            int duracionMinutos
    ) {
        this.horarioMedicoRepository =
                horarioMedicoRepository;
        this.eventoAgendaRepository =
                eventoAgendaRepository;
        this.citaRepository =
                citaRepository;
        this.medicoRepository =
                medicoRepository;
        this.duracionMinutos =
                duracionMinutos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioDisponibleDTO> obtenerHorarios(
            Long medicoId,
            LocalDate fecha
    ) {

        if (medicoId == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un médico"
            );
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una fecha"
            );
        }

        LocalDate hoy = LocalDate.now();

        if (fecha.isBefore(hoy)) {
            throw new IllegalArgumentException(
                    "No se pueden agendar citas en fechas pasadas"
            );
        }

        Medico medico =
                medicoRepository
                        .findById(medicoId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Médico no encontrado"
                                )
                        );

        if (!Boolean.TRUE.equals(
                medico.getActivo()
        )) {
            throw new IllegalArgumentException(
                    "El médico no está disponible"
            );
        }

        String diaSemana =
                convertirDiaSemana(
                        fecha.getDayOfWeek()
                );

        List<HorarioMedico> bloques =
                horarioMedicoRepository
                        .findByMedicoIdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
                                medicoId,
                                diaSemana
                        );

        List<HorarioDisponibleDTO> disponibles =
                new ArrayList<>();

        LocalDateTime ahora =
                LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "HH:mm"
                );

        for (HorarioMedico bloque : bloques) {

            LocalDateTime inicioBloque =
                    LocalDateTime.of(
                            fecha,
                            bloque.getHoraInicio()
                    );

            LocalDateTime finBloque =
                    LocalDateTime.of(
                            fecha,
                            bloque.getHoraFin()
                    );

            LocalDateTime inicioSlot =
                    inicioBloque;

            while (!inicioSlot
                    .plusMinutes(duracionMinutos)
                    .isAfter(finBloque)) {

                LocalDateTime finSlot =
                        inicioSlot.plusMinutes(
                                duracionMinutos
                        );

                /*
                 * Si es hoy, no mostramos horas
                 * que ya pasaron.
                 */
                boolean futuro =
                        inicioSlot.isAfter(ahora);

                boolean bloqueadoPorEvento =
                        eventoAgendaRepository
                                .existeBloqueoEnRango(
                                        medicoId,
                                        inicioSlot,
                                        finSlot
                                );

                boolean ocupadoPorCita =
                        citaRepository
                                .existeCitaEnRango(
                                        medicoId,
                                        inicioSlot,
                                        finSlot
                                );

                if (futuro
                        && !bloqueadoPorEvento
                        && !ocupadoPorCita) {

                    disponibles.add(
                            new HorarioDisponibleDTO(
                                    inicioSlot,
                                    finSlot,
                                    inicioSlot
                                            .toLocalTime()
                                            .format(formatter)
                            )
                    );
                }

                inicioSlot =
                        inicioSlot.plusMinutes(
                                duracionMinutos
                        );
            }
        }

        return disponibles;
    }

    private String convertirDiaSemana(
            DayOfWeek dayOfWeek
    ) {

        return switch (dayOfWeek) {

            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }
}