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
import com.example.proyecto_semestre_medico.service.CorreoPendienteService;
import com.example.proyecto_semestre_medico.service.CorreoSeguimientoPlantilla;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@PreAuthorize("hasRole('MEDICO') and #nombreUsuario == authentication.name")
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
    private final int duracionMinutos;
    private final CorreoSeguimientoPlantilla plantillaCorreo;

    private static final String FECHA_INVALIDA = "Seleccione una fecha futura dentro de los horarios disponibles del médico.";
    private static final String HORARIO_OCUPADO = "El horario seleccionado ya no está disponible. Por favor, elija otro horario.";

    private final CorreoPendienteService
            correoPendienteService;

    public SeguimientoCitaServiceImpl(
            CitaRepository citaRepository,
            ConsultaMedicaRepository consultaMedicaRepository,
            MedicoRepository medicoRepository,
            EstadoCitaRepository estadoCitaRepository,
            EventoAgendaRepository eventoAgendaRepository,
            CitaDisponibilidadService citaDisponibilidadService,
            CorreoPendienteService correoPendienteService,

            @Value("${app.citas.reserva-minutos:5}")
            int reservaMinutos,
            @Value("${app.citas.duracion-minutos:30}") int duracionMinutos,
            CorreoSeguimientoPlantilla plantillaCorreo
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

        this.correoPendienteService =
                correoPendienteService;
        this.duracionMinutos = duracionMinutos;
        this.plantillaCorreo = plantillaCorreo;
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

        if (fecha == null || fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(FECHA_INVALIDA);
        }

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

        Medico medico = medicoRepository.findByPersonalUsuarioNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el médico asociado al usuario autenticado."));
        medicoRepository.buscarParaAgendar(medico.getId())
                .orElseThrow(() -> new IllegalArgumentException("El médico original no existe."));

        Cita citaOrigen =
                obtenerYValidarCitaOrigen(
                        dto.getCitaOrigenId(),
                        nombreUsuario
                );

        LocalDateTime inicio =
                dto.getFechaHoraInicio();

        LocalDateTime finSolicitado = inicio.plusMinutes(duracionMinutos);
        if (eventoAgendaRepository.existeBloqueoEnRango(medico.getId(), inicio, finSolicitado)
                || citaRepository.existeCitaEnRango(medico.getId(), inicio, finSolicitado)) {
            throw new IllegalArgumentException(HORARIO_OCUPADO);
        }

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
                                        FECHA_INVALIDA
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
                    HORARIO_OCUPADO
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
                    HORARIO_OCUPADO
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
        seguimiento.setRecordatorioSeguimientoEnviado(false);
        seguimiento.setFechaRecordatorioSeguimiento(null);

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

        Cita seguimientoGuardado =
                citaRepository.save(
                        seguimiento
                );

        registrarCorreoConfirmacion(
                seguimientoGuardado
        );

        return seguimientoGuardado;
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

        if (consulta.getMedico() == null || !medicoAutenticado.getId().equals(consulta.getMedico().getId())
                || consulta.getCita() == null || !citaOrigenId.equals(consulta.getCita().getId())) {
            throw new IllegalArgumentException("No tiene autorización para programar seguimiento de esta consulta.");
        }
        if (citaOrigen.getPaciente() == null || citaOrigen.getPaciente().getId() == null
                || citaOrigen.getEspecialidad() == null || citaOrigen.getSucursal() == null
                || citaOrigen.getEspecialidad().getId() == null || citaOrigen.getSucursal().getId() == null
                || !Boolean.TRUE.equals(citaOrigen.getSucursal().getActivo())
                || !Boolean.TRUE.equals(citaOrigen.getEspecialidad().getActivo())
                || !Boolean.TRUE.equals(medicoAutenticado.getActivo())
                || medicoAutenticado.getPersonal() == null
                || medicoAutenticado.getPersonal().getSucursal() == null
                || !citaOrigen.getSucursal().getId().equals(medicoAutenticado.getPersonal().getSucursal().getId())
                || medicoAutenticado.getEspecialidad() == null
                || !citaOrigen.getEspecialidad().getId().equals(medicoAutenticado.getEspecialidad().getId())) {
            throw new IllegalArgumentException("La consulta original no tiene un contexto válido de paciente, médico, especialidad y sucursal.");
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
                    FECHA_INVALIDA
            );
        }

        if (!dto.getFechaHoraInicio()
                .isAfter(
                        LocalDateTime.now()
                )) {

            throw new IllegalArgumentException(
                    FECHA_INVALIDA
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
                    "Debe seleccionar el tipo de seguimiento."
            );
        }

        if (dto.getPrioridad()
                == null
                || dto.getPrioridad()
                .isBlank()) {

            dto.setPrioridad("NORMAL");
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
                    "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres."
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

    private void registrarCorreoConfirmacion(Cita seguimiento) {
        String destinatario = seguimiento.getPaciente().getCorreo();
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El paciente no tiene correo electrónico registrado para recibir la notificación de seguimiento.");
        }
        correoPendienteService.registrar(destinatario, plantillaCorreo.asuntoConfirmacion(),
                plantillaCorreo.contenido(seguimiento, false));
    }
}