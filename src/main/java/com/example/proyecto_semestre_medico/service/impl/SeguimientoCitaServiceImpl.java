package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.HorarioDisponibleDTO;
import com.example.proyecto_semestre_medico.dto.SeguimientoCitaDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.ConsultaMedica;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.model.Medico;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.ConsultaMedicaRepository;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.repository.EventoAgendaRepository;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import com.example.proyecto_semestre_medico.service.CitaDisponibilidadService;
import com.example.proyecto_semestre_medico.service.SeguimientoCitaService;
import com.example.proyecto_semestre_medico.dto.SeguimientoContextoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SeguimientoCitaServiceImpl
        implements SeguimientoCitaService {

    private static final Set<String>
            TIPOS_PERMITIDOS =
            Set.of(
                    "MONITOREO_TRATAMIENTO",
                    "REVISION_RESULTADOS_LABORATORIO"
            );

    private static final Set<String>
            PRIORIDADES_PERMITIDAS =
            Set.of(
                    "BAJA",
                    "NORMAL",
                    "ALTA"
            );

    private final CitaRepository
            citaRepository;

    private final ConsultaMedicaRepository
            consultaMedicaRepository;

    private final MedicoRepository
            medicoRepository;

    private final EstadoCitaRepository
            estadoCitaRepository;

    private final EventoAgendaRepository
            eventoAgendaRepository;

    private final CitaDisponibilidadService
            citaDisponibilidadService;

    private final int reservaMinutos;

    public SeguimientoCitaServiceImpl(
            CitaRepository citaRepository,
            ConsultaMedicaRepository consultaMedicaRepository,
            MedicoRepository medicoRepository,
            EstadoCitaRepository estadoCitaRepository,
            EventoAgendaRepository eventoAgendaRepository,
            CitaDisponibilidadService citaDisponibilidadService,

            @Value("${app.citas.reserva-minutos:5}")
            int reservaMinutos
    ) {

        this.citaRepository =
                citaRepository;

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.medicoRepository =
                medicoRepository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.eventoAgendaRepository =
                eventoAgendaRepository;

        this.citaDisponibilidadService =
                citaDisponibilidadService;

        this.reservaMinutos =
                reservaMinutos;
    }

    /*
     * =========================================================
     * CONSULTAR DISPONIBILIDAD PARA CU-12
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public SeguimientoContextoDTO obtenerContexto(
            Long citaOrigenId,
            String nombreUsuario
    ) {

        Cita cita =
                obtenerYValidarCitaOrigen(
                        citaOrigenId,
                        nombreUsuario
                );

        SeguimientoContextoDTO dto =
                new SeguimientoContextoDTO();

        dto.setCitaId(
                cita.getId()
        );

        dto.setNumeroCita(
                cita.getNumeroCita()
        );

        dto.setPaciente(
                cita.getPaciente()
                        .getNombreCompleto()
        );

        dto.setMedico(
                cita.getMedico()
                        .getPersonal()
                        .getNombreCompleto()
        );

        dto.setEspecialidad(
                cita.getEspecialidad()
                        .getNombre()
        );

        dto.setSucursal(
                cita.getSucursal()
                        .getNombre()
        );

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioDisponibleDTO>
    obtenerHorariosDisponibles(
            Long citaOrigenId,
            LocalDate fecha,
            String nombreUsuario
    ) {

        Cita citaOrigen =
                obtenerYValidarCitaOrigen(
                        citaOrigenId,
                        nombreUsuario
                );

        /*
         * Reutilizamos exactamente el motor de
         * disponibilidad que ya utiliza CU-03.
         */
        return citaDisponibilidadService
                .obtenerHorarios(
                        citaOrigen
                                .getMedico()
                                .getId(),
                        fecha
                );
    }

    /*
     * =========================================================
     * CREAR CITA DE SEGUIMIENTO
     * =========================================================
     */

    @Override
    @Transactional
    public Cita crearSeguimiento(
            SeguimientoCitaDTO dto,
            String nombreUsuario
    ) {

        validarDTO(
                dto
        );

        Cita citaOrigen =
                obtenerYValidarCitaOrigen(
                        dto.getCitaOrigenId(),
                        nombreUsuario
                );

        LocalDateTime inicio =
                dto.getFechaHoraInicio();

        /*
         * Consultamos nuevamente los horarios
         * disponibles utilizando CU-03.
         *
         * Esto impide aceptar una hora inventada
         * manualmente desde el navegador.
         */
        List<HorarioDisponibleDTO>
                horariosDisponibles =
                citaDisponibilidadService
                        .obtenerHorarios(
                                citaOrigen
                                        .getMedico()
                                        .getId(),
                                inicio.toLocalDate()
                        );

        HorarioDisponibleDTO
                horarioSeleccionado =
                horariosDisponibles
                        .stream()
                        .filter(h ->
                                h.getInicio()
                                        .equals(inicio)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El horario seleccionado ya no se encuentra disponible."
                                )
                        );

        LocalDateTime fin =
                horarioSeleccionado
                        .getFin();

        /*
         * REVALIDACIÓN justo antes de persistir.
         *
         * Entre mostrar los horarios y confirmar
         * el formulario otra persona pudo reservar
         * el mismo espacio.
         */
        if (eventoAgendaRepository
                .existeBloqueoEnRango(
                        citaOrigen
                                .getMedico()
                                .getId(),
                        inicio,
                        fin
                )) {

            throw new IllegalArgumentException(
                    "El médico ya no está disponible en ese horario."
            );
        }

        if (citaRepository
                .existeCitaEnRango(
                        citaOrigen
                                .getMedico()
                                .getId(),
                        inicio,
                        fin
                )) {

            throw new IllegalArgumentException(
                    "El horario acaba de ser reservado. Seleccione otro."
            );
        }

        EstadoCita estado =
                estadoCitaRepository
                        .findByCodigo(
                                "PENDIENTE_PAGO"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el estado PENDIENTE_PAGO."
                                )
                        );

        Cita seguimiento =
                new Cita();

        seguimiento.setNumeroCita(
                generarNumeroCita()
        );

        /*
         * Estos datos NO vienen del formulario.
         *
         * Todos se heredan de la cita original.
         */
        seguimiento.setPaciente(
                citaOrigen.getPaciente()
        );

        seguimiento.setSucursal(
                citaOrigen.getSucursal()
        );

        seguimiento.setEspecialidad(
                citaOrigen.getEspecialidad()
        );

        seguimiento.setMedico(
                citaOrigen.getMedico()
        );

        seguimiento.setEstadoCita(
                estado
        );

        seguimiento.setFechaHoraInicio(
                inicio
        );

        seguimiento.setFechaHoraFin(
                fin
        );

        seguimiento.setMotivoConsulta(
                dto.getMotivo()
                        .trim()
        );

        seguimiento.setOrigen(
                "SEGUIMIENTO"
        );

        seguimiento.setCitaSeguimientoDe(
                citaOrigen
        );

        seguimiento.setTipoSeguimiento(
                dto.getTipoSeguimiento()
                        .trim()
                        .toUpperCase()
        );

        seguimiento.setPrioridadSeguimiento(
                dto.getPrioridad()
                        .trim()
                        .toUpperCase()
        );

        /*
         * La prioridad de seguimiento y la prioridad
         * de emergencia son conceptos diferentes.
         */
        seguimiento.setPrioridadEmergencia(
                false
        );

        /*
         * Mantenemos el mismo mecanismo de reserva
         * temporal usado en CU-03.
         */
        seguimiento.setReservaExpiraEn(
                LocalDateTime
                        .now()
                        .plusMinutes(
                                reservaMinutos
                        )
        );

        return citaRepository.save(
                seguimiento
        );
    }

    /*
     * =========================================================
     * VALIDACIÓN DE CITA ORIGEN
     * =========================================================
     */

    private Cita obtenerYValidarCitaOrigen(
            Long citaOrigenId,
            String nombreUsuario
    ) {

        if (citaOrigenId == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la cita de origen."
            );
        }

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new IllegalArgumentException(
                    "No se pudo identificar al médico autenticado."
            );
        }

        Cita citaOrigen =
                citaRepository
                        .findById(
                                citaOrigenId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La cita original no fue encontrada."
                                )
                        );

        Medico medicoAutenticado =
                medicoRepository
                        .findByPersonalUsuarioNombreUsuario(
                                nombreUsuario
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No se encontró el médico asociado al usuario autenticado."
                                )
                        );

        /*
         * Seguridad:
         * el médico no puede programar seguimientos
         * de una cita perteneciente a otro médico.
         */
        if (citaOrigen.getMedico() == null
                || citaOrigen
                .getMedico()
                .getId() == null
                || !citaOrigen
                .getMedico()
                .getId()
                .equals(
                        medicoAutenticado
                                .getId()
                )) {

            throw new IllegalArgumentException(
                    "No tiene autorización para programar seguimiento de esta cita."
            );
        }

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findByCitaId(
                                citaOrigenId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La cita no posee una consulta médica registrada."
                                )
                        );

        if (!"FINALIZADA".equalsIgnoreCase(
                consulta.getEstado()
        )) {

            throw new IllegalArgumentException(
                    "La consulta debe estar finalizada antes de programar un seguimiento."
            );
        }

        return citaOrigen;
    }

    /*
     * =========================================================
     * VALIDACIONES DEL FORMULARIO
     * =========================================================
     */

    private void validarDTO(
            SeguimientoCitaDTO dto
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Datos del seguimiento requeridos."
            );
        }

        if (dto.getCitaOrigenId()
                == null) {

            throw new IllegalArgumentException(
                    "La cita original es obligatoria."
            );
        }

        if (dto.getFechaHoraInicio()
                == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar fecha y hora para el seguimiento."
            );
        }

        if (!dto.getFechaHoraInicio()
                .isAfter(
                        LocalDateTime.now()
                )) {

            throw new IllegalArgumentException(
                    "La fecha y hora del seguimiento deben ser futuras."
            );
        }

        if (dto.getTipoSeguimiento()
                == null
                || dto.getTipoSeguimiento()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de seguimiento."
            );
        }

        String tipo =
                dto.getTipoSeguimiento()
                        .trim()
                        .toUpperCase();

        if (!TIPOS_PERMITIDOS
                .contains(tipo)) {

            throw new IllegalArgumentException(
                    "El tipo de seguimiento seleccionado no es válido."
            );
        }

        if (dto.getPrioridad()
                == null
                || dto.getPrioridad()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar la prioridad del seguimiento."
            );
        }

        String prioridad =
                dto.getPrioridad()
                        .trim()
                        .toUpperCase();

        if (!PRIORIDADES_PERMITIDAS
                .contains(prioridad)) {

            throw new IllegalArgumentException(
                    "La prioridad seleccionada no es válida."
            );
        }

        if (dto.getMotivo()
                == null
                || dto.getMotivo()
                .trim()
                .length() < 10
                || dto.getMotivo()
                .trim()
                .length() > 2000) {

            throw new IllegalArgumentException(
                    "El motivo del seguimiento debe tener entre 10 y 2000 caracteres."
            );
        }
    }

    /*
     * =========================================================
     * NÚMERO DE CITA
     * =========================================================
     */

    private String generarNumeroCita() {

        String numero;

        do {

            numero =
                    "CIT-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase();

        } while (
                citaRepository
                        .existsByNumeroCita(
                                numero
                        )
        );

        return numero;
    }
}