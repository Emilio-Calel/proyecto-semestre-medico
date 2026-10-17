package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.AgendaItemDTO;
import com.example.proyecto_semestre_medico.dto.EventoAgendaFormDTO;
import com.example.proyecto_semestre_medico.dto.TareaMedicoFormDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.EventoAgenda;
import com.example.proyecto_semestre_medico.model.Medico;
import com.example.proyecto_semestre_medico.model.TareaMedico;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.EventoAgendaRepository;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import com.example.proyecto_semestre_medico.repository.TareaMedicoRepository;
import com.example.proyecto_semestre_medico.service.AgendaMedicoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class AgendaMedicoServiceImpl
        implements AgendaMedicoService {

    private static final Set<String>
            TIPOS_EVENTO =
            Set.of(
                    "REUNION",
                    "DESCANSO",
                    "CAPACITACION",
                    "PERSONAL",
                    "OTRO"
            );

    private static final Set<String>
            PRIORIDADES =
            Set.of(
                    "BAJA",
                    "NORMAL",
                    "ALTA"
            );

    private final MedicoRepository medicoRepository;

    private final CitaRepository citaRepository;

    private final EventoAgendaRepository
            eventoAgendaRepository;

    private final TareaMedicoRepository
            tareaMedicoRepository;

    public AgendaMedicoServiceImpl(
            MedicoRepository medicoRepository,
            CitaRepository citaRepository,
            EventoAgendaRepository eventoAgendaRepository,
            TareaMedicoRepository tareaMedicoRepository
    ) {

        this.medicoRepository =
                medicoRepository;

        this.citaRepository =
                citaRepository;

        this.eventoAgendaRepository =
                eventoAgendaRepository;

        this.tareaMedicoRepository =
                tareaMedicoRepository;
    }

    /*
     * =========================================================
     * CALENDARIO
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<AgendaItemDTO> listarAgenda(
            String nombreUsuario,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {

        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el rango de fechas."
            );
        }

        if (!desde.isBefore(hasta)) {
            throw new IllegalArgumentException(
                    "El rango de fechas no es válido."
            );
        }

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        List<AgendaItemDTO> items =
                new ArrayList<>();

        List<Cita> citas =
                citaRepository
                        .listarAgendaMedico(
                                medico.getId(),
                                desde,
                                hasta
                        );

        for (Cita cita : citas) {

            AgendaItemDTO dto =
                    new AgendaItemDTO();

            dto.setId(
                    cita.getId()
            );

            dto.setTipo(
                    "CITA"
            );

            dto.setTitulo(
                    cita.getPaciente()
                            .getNombreCompleto()
            );

            dto.setDescripcion(
                    cita.getEspecialidad()
                            .getNombre()
                            + " - "
                            + cita.getNumeroCita()
            );

            dto.setInicio(
                    cita.getFechaHoraInicio()
            );

            dto.setFin(
                    cita.getFechaHoraFin()
            );

            dto.setEstado(
                    cita.getEstadoCita()
                            .getCodigo()
            );

            dto.setTodoElDia(
                    false
            );

            items.add(
                    dto
            );
        }

        List<EventoAgenda> eventos =
                eventoAgendaRepository
                        .listarEnRango(
                                medico.getId(),
                                desde,
                                hasta
                        );

        for (EventoAgenda evento : eventos) {

            AgendaItemDTO dto =
                    new AgendaItemDTO();

            dto.setId(
                    evento.getId()
            );

            dto.setTipo(
                    "EVENTO"
            );

            dto.setTitulo(
                    evento.getTitulo()
            );

            dto.setDescripcion(
                    evento.getDescripcion()
            );

            dto.setInicio(
                    evento.getFechaInicio()
            );

            dto.setFin(
                    evento.getFechaFin()
            );

            dto.setTipoEvento(
                    evento.getTipoEvento()
            );

            dto.setTodoElDia(
                    Boolean.TRUE.equals(
                            evento.getTodoElDia()
                    )
            );

            items.add(
                    dto
            );
        }

        items.sort(
                Comparator.comparing(
                        AgendaItemDTO::getInicio
                )
        );

        return items;
    }

    /*
     * =========================================================
     * EVENTOS
     * =========================================================
     */

    @Override
    @Transactional
    public EventoAgenda guardarEvento(
            EventoAgendaFormDTO dto,
            String nombreUsuario
    ) {

        validarEvento(
                dto
        );

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        EventoAgenda evento;

        if (dto.getId() == null) {

            evento =
                    new EventoAgenda();

            evento.setMedico(
                    medico
            );

        } else {

            evento =
                    eventoAgendaRepository
                            .findByIdAndMedicoIdAndActivoTrue(
                                    dto.getId(),
                                    medico.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El evento no existe o no pertenece al médico autenticado."
                                    )
                            );
        }

        evento.setTitulo(
                dto.getTitulo()
                        .trim()
        );

        evento.setDescripcion(
                limpiar(
                        dto.getDescripcion()
                )
        );

        evento.setFechaInicio(
                dto.getFechaInicio()
        );

        evento.setFechaFin(
                dto.getFechaFin()
        );

        evento.setTipoEvento(
                dto.getTipoEvento()
                        .trim()
                        .toUpperCase()
        );

        evento.setTodoElDia(
                Boolean.TRUE.equals(
                        dto.getTodoElDia()
                )
        );

        evento.setActivo(
                true
        );

        return eventoAgendaRepository
                .save(
                        evento
                );
    }

    @Override
    @Transactional(readOnly = true)
    public EventoAgendaFormDTO obtenerEvento(
            Long eventoId,
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        EventoAgenda evento =
                eventoAgendaRepository
                        .findByIdAndMedicoIdAndActivoTrue(
                                eventoId,
                                medico.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El evento no existe o no pertenece al médico autenticado."
                                )
                        );

        EventoAgendaFormDTO dto =
                new EventoAgendaFormDTO();

        dto.setId(
                evento.getId()
        );

        dto.setTitulo(
                evento.getTitulo()
        );

        dto.setDescripcion(
                evento.getDescripcion()
        );

        dto.setFechaInicio(
                evento.getFechaInicio()
        );

        dto.setFechaFin(
                evento.getFechaFin()
        );

        dto.setTipoEvento(
                evento.getTipoEvento()
        );

        dto.setTodoElDia(
                evento.getTodoElDia()
        );

        return dto;
    }

    @Override
    @Transactional
    public void eliminarEvento(
            Long eventoId,
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        EventoAgenda evento =
                eventoAgendaRepository
                        .findByIdAndMedicoIdAndActivoTrue(
                                eventoId,
                                medico.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El evento no existe o no pertenece al médico autenticado."
                                )
                        );

        /*
         * Borrado lógico.
         */
        evento.setActivo(
                false
        );

        eventoAgendaRepository.save(
                evento
        );
    }

    /*
     * =========================================================
     * TAREAS
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<TareaMedico> listarTareas(
            String nombreUsuario,
            String filtro
    ) {

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        String valor =
                filtro == null
                        ? "TODAS"
                        : filtro.trim()
                        .toUpperCase();

        return switch (valor) {

            case "PENDIENTES" ->
                    tareaMedicoRepository
                            .findByMedicoIdAndActivoTrueAndCompletadaOrderByFechaLimiteAsc(
                                    medico.getId(),
                                    false
                            );

            case "COMPLETADAS" ->
                    tareaMedicoRepository
                            .findByMedicoIdAndActivoTrueAndCompletadaOrderByFechaLimiteAsc(
                                    medico.getId(),
                                    true
                            );

            default ->
                    tareaMedicoRepository
                            .findByMedicoIdAndActivoTrueOrderByCompletadaAscFechaLimiteAsc(
                                    medico.getId()
                            );
        };
    }

    @Override
    @Transactional
    public TareaMedico guardarTarea(
            TareaMedicoFormDTO dto,
            String nombreUsuario
    ) {

        validarTarea(
                dto
        );

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        TareaMedico tarea;

        if (dto.getId() == null) {

            tarea =
                    new TareaMedico();

            tarea.setMedico(
                    medico
            );

            tarea.setCompletada(
                    false
            );

        } else {

            tarea =
                    tareaMedicoRepository
                            .findByIdAndMedicoIdAndActivoTrue(
                                    dto.getId(),
                                    medico.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "La tarea no existe o no pertenece al médico autenticado."
                                    )
                            );
        }

        tarea.setTitulo(
                dto.getTitulo()
                        .trim()
        );

        tarea.setDescripcion(
                limpiar(
                        dto.getDescripcion()
                )
        );

        tarea.setPrioridad(
                dto.getPrioridad()
                        .trim()
                        .toUpperCase()
        );

        tarea.setFechaLimite(
                dto.getFechaLimite()
        );

        tarea.setActivo(
                true
        );

        return tareaMedicoRepository
                .save(
                        tarea
                );
    }

    @Override
    @Transactional
    public void cambiarEstadoTarea(
            Long tareaId,
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        TareaMedico tarea =
                tareaMedicoRepository
                        .findByIdAndMedicoIdAndActivoTrue(
                                tareaId,
                                medico.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La tarea no existe o no pertenece al médico autenticado."
                                )
                        );

        tarea.setCompletada(
                !Boolean.TRUE.equals(
                        tarea.getCompletada()
                )
        );

        tareaMedicoRepository.save(
                tarea
        );
    }

    @Override
    @Transactional
    public void eliminarTarea(
            Long tareaId,
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedico(
                        nombreUsuario
                );

        TareaMedico tarea =
                tareaMedicoRepository
                        .findByIdAndMedicoIdAndActivoTrue(
                                tareaId,
                                medico.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La tarea no existe o no pertenece al médico autenticado."
                                )
                        );

        /*
         * También usamos borrado lógico.
         */
        tarea.setActivo(
                false
        );

        tareaMedicoRepository.save(
                tarea
        );
    }

    /*
     * =========================================================
     * VALIDACIONES
     * =========================================================
     */

    private void validarEvento(
            EventoAgendaFormDTO dto
    ) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Datos del evento requeridos."
            );
        }

        if (dto.getTitulo() == null
                || dto.getTitulo()
                .trim()
                .length() < 3
                || dto.getTitulo()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "El título del evento debe tener entre 3 y 200 caracteres."
            );
        }

        if (dto.getDescripcion() != null
                && dto.getDescripcion()
                .trim()
                .length() > 2000) {

            throw new IllegalArgumentException(
                    "La descripción no puede superar los 2000 caracteres."
            );
        }

        if (dto.getFechaInicio() == null
                || dto.getFechaFin() == null) {

            throw new IllegalArgumentException(
                    "Debe indicar fecha de inicio y fecha de finalización."
            );
        }

        if (!dto.getFechaInicio()
                .isBefore(
                        dto.getFechaFin()
                )) {

            throw new IllegalArgumentException(
                    "La fecha de finalización debe ser posterior a la fecha de inicio."
            );
        }

        if (dto.getTipoEvento() == null
                || dto.getTipoEvento()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de evento."
            );
        }

        String tipo =
                dto.getTipoEvento()
                        .trim()
                        .toUpperCase();

        if (!TIPOS_EVENTO.contains(
                tipo
        )) {

            throw new IllegalArgumentException(
                    "El tipo de evento seleccionado no es válido."
            );
        }
    }

    private void validarTarea(
            TareaMedicoFormDTO dto
    ) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Datos de la tarea requeridos."
            );
        }

        if (dto.getTitulo() == null
                || dto.getTitulo()
                .trim()
                .length() < 3
                || dto.getTitulo()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "El título de la tarea debe tener entre 3 y 200 caracteres."
            );
        }

        if (dto.getDescripcion() != null
                && dto.getDescripcion()
                .trim()
                .length() > 2000) {

            throw new IllegalArgumentException(
                    "La descripción no puede superar los 2000 caracteres."
            );
        }

        if (dto.getPrioridad() == null
                || dto.getPrioridad()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar la prioridad."
            );
        }

        String prioridad =
                dto.getPrioridad()
                        .trim()
                        .toUpperCase();

        if (!PRIORIDADES.contains(
                prioridad
        )) {

            throw new IllegalArgumentException(
                    "La prioridad seleccionada no es válida."
            );
        }

        if (dto.getFechaLimite() == null) {

            throw new IllegalArgumentException(
                    "La fecha límite es obligatoria."
            );
        }
    }

    private Medico obtenerMedico(
            String nombreUsuario
    ) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new IllegalArgumentException(
                    "No se pudo identificar al médico autenticado."
            );
        }

        return medicoRepository
                .findByPersonalUsuarioNombreUsuario(
                        nombreUsuario
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El usuario autenticado no está registrado como médico."
                        )
                );
    }

    private String limpiar(
            String valor
    ) {

        if (valor == null) {
            return null;
        }

        String limpio =
                valor.trim();

        return limpio.isEmpty()
                ? null
                : limpio;
    }
}