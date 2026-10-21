package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.CitaRecepcionDTO;
import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import com.example.proyecto_semestre_medico.service.RecepcionService;
import com.example.proyecto_semestre_medico.repository.PacienteRepository;
import com.example.proyecto_semestre_medico.dto.ResultadoLlegadaRecepcionDTO;
import com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO;
import com.example.proyecto_semestre_medico.model.Medico;
import com.example.proyecto_semestre_medico.model.HorarioMedico;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import com.example.proyecto_semestre_medico.repository.EventoAgendaRepository;
import com.example.proyecto_semestre_medico.repository.HorarioMedicoRepository;
import com.example.proyecto_semestre_medico.dto.EmergenciaRecepcionDTO;
import com.example.proyecto_semestre_medico.repository.PersonalRepository;
import com.example.proyecto_semestre_medico.model.Paciente;
import com.example.proyecto_semestre_medico.model.Personal;
import com.example.proyecto_semestre_medico.model.Sucursal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.proyecto_semestre_medico.model.Pago;
import com.example.proyecto_semestre_medico.repository.PagoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecepcionServiceImpl
        implements RecepcionService {

    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final DatosSensiblesService datosSensiblesService;
    private final PagoRepository pagoRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final EventoAgendaRepository eventoAgendaRepository;
    private final HorarioMedicoRepository horarioMedicoRepository;
    private final PersonalRepository personalRepository;

    public RecepcionServiceImpl(
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            DatosSensiblesService datosSensiblesService,
            PagoRepository pagoRepository,
            PacienteRepository pacienteRepository,
            MedicoRepository medicoRepository,
            EventoAgendaRepository eventoAgendaRepository,
            PersonalRepository personalRepository,
            HorarioMedicoRepository horarioMedicoRepository
    ) {
        this.citaRepository = citaRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.datosSensiblesService = datosSensiblesService;
        this.pagoRepository = pagoRepository;
        this.pacienteRepository =
                pacienteRepository;
        this.medicoRepository =
                medicoRepository;

        this.eventoAgendaRepository =
                eventoAgendaRepository;

        this.horarioMedicoRepository =
                horarioMedicoRepository;
        this.personalRepository =
                personalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaRecepcionDTO> buscar(
            String criterio,
            String tipoBusqueda
    ) {

        if (criterio == null
                || criterio.trim().isEmpty()) {

            return List.of();
        }

        String valor =
                criterio.trim();

        String tipo =
                tipoBusqueda == null
                        ? "CITA"
                        : tipoBusqueda.trim().toUpperCase();

        List<Cita> citas =
                new ArrayList<>();

        if ("DPI".equals(tipo)) {

            if (!valor.matches(
                    "^[0-9]{13}$"
            )) {

                return List.of();
            }

            String dpiHash =
                    datosSensiblesService
                            .generarHashBusqueda(
                                    valor
                            );

            citas =
                    citaRepository
                            .buscarPorDpiHash(
                                    dpiHash
                            );

        } else {

            citaRepository
                    .findByNumeroCitaIgnoreCase(
                            valor
                    )
                    .ifPresent(
                            citas::add
                    );
        }

        return citas.stream()
                .map(this::convertir)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> buscarNombrePacientePorDpi(
            String dpi
    ) {

        if (dpi == null
                || !dpi.trim()
                .matches("^[0-9]{13}$")) {

            return Optional.empty();
        }

        String dpiHash =
                datosSensiblesService
                        .generarHashBusqueda(
                                dpi.trim()
                        );

        return pacienteRepository
                .findByDpiHash(dpiHash)
                .map(
                        paciente ->
                                paciente.getNombreCompleto()
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarIdPacientePorDpi(
            String dpi
    ) {

        if (dpi == null
                || !dpi.trim()
                .matches("^[0-9]{13}$")) {

            return Optional.empty();
        }

        String dpiHash =
                datosSensiblesService
                        .generarHashBusqueda(
                                dpi.trim()
                        );

        return pacienteRepository
                .findByDpiHash(
                        dpiHash
                )
                .map(
                        paciente ->
                                paciente.getId()
                );
    }

    @Override
    @Transactional
    public ResultadoLlegadaRecepcionDTO registrarLlegada(
            Long citaId,
            String usuarioRecepcionista
    ) {

        if (citaId == null) {
            throw new IllegalArgumentException(
                    "Cita requerida"
            );
        }

        Cita cita =
                citaRepository
                        .findById(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada"
                                )
                        );

        if (!"CONFIRMADA".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "Operación no permitida"
            );
        }

        boolean pagoAprobado =
                pagoRepository
                        .findFirstByCitaIdAndEstadoOrderByFechaPagoDesc(
                                cita.getId(),
                                "APROBADO"
                        )
                        .isPresent();

        if (!pagoAprobado) {

            throw new IllegalArgumentException(
                    "La cita no tiene un pago aprobado registrado"
            );
        }

        /*
         * Evitamos registrar llegada
         * absurdamente antes de la cita.
         *
         * Dejamos 2 horas de tolerancia previa
         * por ahora.
         */
        LocalDateTime ahora =
                LocalDateTime.now();



        EstadoCita presente =
                estadoCitaRepository
                        .findByCodigo(
                                "PACIENTE_PRESENTE"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el estado PACIENTE_PRESENTE"
                                )
                        );

        cita.setFechaHoraLlegada(
                LocalDateTime.now()
        );

        cita.setEstadoCita(presente);

        citaRepository.save(cita);

        registrarAuditoria(
                cita,
                usuarioRecepcionista
        );

        return new ResultadoLlegadaRecepcionDTO(
                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getNumeroCita(),

                Boolean.TRUE.equals(
                        cita.getPrioridadEmergencia()
                )
        );

    }

    private CitaRecepcionDTO convertir(
            Cita cita
    ) {

        Pago pagoAprobado =
                pagoRepository
                        .findFirstByCitaIdAndEstadoOrderByFechaPagoDesc(
                                cita.getId(),
                                "APROBADO"
                        )
                        .orElse(null);

        String estadoPago;
        String metodoPago;

        if (pagoAprobado != null) {

            estadoPago = "PAGADO";
            metodoPago =
                    pagoAprobado.getMetodoPago();

        } else {

            estadoPago = "PENDIENTE";
            metodoPago = "-";
        }

        return new CitaRecepcionDTO(
                cita.getId(),

                cita.getPaciente()
                        .getId(),

                cita.getNumeroCita(),

                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getPaciente()
                        .getTelefono(),

                cita.getPaciente()
                        .getCorreo(),

                cita.getSucursal()
                        .getNombre(),

                cita.getEspecialidad() != null
                        ? cita.getEspecialidad()
                        .getNombre()
                        : "Pendiente de asignación",

                cita.getMedico() != null
                        ? cita.getMedico()
                        .getPersonal()
                        .getNombreCompleto()
                        : "Pendiente de asignación",

                cita.getFechaHoraInicio(),

                cita.getFechaHoraLlegada(),

                cita.getMotivoConsulta(),

                Boolean.TRUE.equals(
                        cita.getPrioridadEmergencia()
                ),

                cita.getEstadoCita()
                        .getCodigo(),

                estadoPago,
                metodoPago
        );
    }

    private void registrarAuditoria(
            Cita cita,
            String nombreUsuario
    ) {

        Usuario usuario =
                usuarioRepository
                        .findByNombreUsuarioIgnoreCase(
                                nombreUsuario
                        )
                        .orElse(null);

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuario);
        auditoria.setAccion(
                "REGISTRAR_LLEGADA"
        );
        auditoria.setTipoEntidad(
                "CITA"
        );
        auditoria.setEntidadId(
                String.valueOf(cita.getId())
        );
        auditoria.setDetalle(
                "Check-in de cita "
                        + cita.getNumeroCita()
                        + ". Nuevo estado: PACIENTE_PRESENTE"
        );

        auditoriaRepository.save(
                auditoria
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CitaRecepcionDTO obtenerParaReasignacion(
            Long citaId
    ) {

        if (citaId == null) {

            throw new IllegalArgumentException(
                    "Cita requerida."
            );
        }

        Cita cita =
                citaRepository
                        .buscarCitaCompletaPorId(
                                citaId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada."
                                )
                        );

        String estado =
                cita.getEstadoCita()
                        .getCodigo();

        if (!"CONFIRMADA".equals(estado)
                && !"PACIENTE_PRESENTE".equals(estado)) {

            throw new IllegalStateException(
                    "La cita no puede ser reasignada en su estado actual."
            );
        }

        return convertir(
                cita
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoOpcionDTO> listarMedicosDisponiblesParaReasignacion(
            Long citaId
    ) {

        if (citaId == null) {

            throw new IllegalArgumentException(
                    "Cita requerida."
            );
        }

        Cita cita =
                citaRepository
                        .buscarCitaCompletaPorId(
                                citaId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada."
                                )
                        );

        String estado =
                cita.getEstadoCita()
                        .getCodigo();

        if (!"CONFIRMADA".equals(estado)
                && !"PACIENTE_PRESENTE".equals(estado)) {

            throw new IllegalStateException(
                    "La cita no puede ser reasignada en su estado actual."
            );
        }

        Long sucursalId =
                cita.getSucursal()
                        .getId();

        Long especialidadId =
                cita.getEspecialidad()
                        .getId();

        Long medicoActualId =
                cita.getMedico()
                        .getId();

        List<MedicoOpcionDTO> candidatos =
                medicoRepository
                        .listarDisponiblesParaCita(
                                sucursalId,
                                especialidadId
                        );

        List<MedicoOpcionDTO> disponibles =
                new java.util.ArrayList<>();

        String diaSemana =
                convertirDiaSemanaReasignacion(
                        cita.getFechaHoraInicio()
                                .getDayOfWeek()
                );

        for (MedicoOpcionDTO candidato : candidatos) {

            if (candidato.getId()
                    .equals(medicoActualId)) {

                continue;
            }

            List<HorarioMedico> horarios =
                    horarioMedicoRepository
                            .findByMedicoIdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
                                    candidato.getId(),
                                    diaSemana
                            );

            boolean cubreHorario =
                    horarios.stream()
                            .anyMatch(horario ->

                                    !cita.getFechaHoraInicio()
                                            .toLocalTime()
                                            .isBefore(
                                                    horario.getHoraInicio()
                                            )

                                            &&

                                            !cita.getFechaHoraFin()
                                                    .toLocalTime()
                                                    .isAfter(
                                                            horario.getHoraFin()
                                                    )
                            );

            if (!cubreHorario) {

                continue;
            }

            boolean bloqueado =
                    eventoAgendaRepository
                            .existeBloqueoEnRango(
                                    candidato.getId(),
                                    cita.getFechaHoraInicio(),
                                    cita.getFechaHoraFin()
                            );

            if (bloqueado) {

                continue;
            }

            boolean ocupado =
                    citaRepository
                            .existeCitaEnRango(
                                    candidato.getId(),
                                    cita.getFechaHoraInicio(),
                                    cita.getFechaHoraFin()
                            );

            if (ocupado) {

                continue;
            }

            disponibles.add(
                    candidato
            );
        }

        return disponibles;
    }

    @Override
    @Transactional
    public void reasignarMedico(
            Long citaId,
            Long medicoId,
            String motivo,
            String usuarioRecepcionista
    ) {

        if (citaId == null) {

            throw new IllegalArgumentException(
                    "Cita requerida."
            );
        }

        if (medicoId == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un médico."
            );
        }

        Cita cita =
                citaRepository
                        .buscarCitaCompletaPorId(
                                citaId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada."
                                )
                        );

        String estado =
                cita.getEstadoCita()
                        .getCodigo();

        if (!"CONFIRMADA".equals(estado)
                && !"PACIENTE_PRESENTE".equals(estado)) {

            throw new IllegalStateException(
                    "La cita no puede ser reasignada en su estado actual."
            );
        }

        Medico medicoActual =
                cita.getMedico();

        if (medicoActual.getId()
                .equals(medicoId)) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un médico diferente al actual."
            );
        }

        Medico nuevoMedico =
                medicoRepository
                        .findById(
                                medicoId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Médico no encontrado."
                                )
                        );

        if (!Boolean.TRUE.equals(
                nuevoMedico.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El médico seleccionado no está disponible."
            );
        }

        if (!Boolean.TRUE.equals(
                nuevoMedico.getPersonal()
                        .getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El médico seleccionado no está disponible."
            );
        }

        if (!nuevoMedico
                .getPersonal()
                .getSucursal()
                .getId()
                .equals(
                        cita.getSucursal()
                                .getId()
                )) {

            throw new IllegalArgumentException(
                    "El médico seleccionado no pertenece a la sucursal de la cita."
            );
        }

        if (!nuevoMedico
                .getEspecialidad()
                .getId()
                .equals(
                        cita.getEspecialidad()
                                .getId()
                )) {

            throw new IllegalArgumentException(
                    "El médico seleccionado no pertenece a la especialidad de la cita."
            );
        }

        String diaSemana =
                convertirDiaSemanaReasignacion(
                        cita.getFechaHoraInicio()
                                .getDayOfWeek()
                );

        List<HorarioMedico> horarios =
                horarioMedicoRepository
                        .findByMedicoIdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
                                nuevoMedico.getId(),
                                diaSemana
                        );

        boolean cubreHorario =
                horarios.stream()
                        .anyMatch(horario ->

                                !cita.getFechaHoraInicio()
                                        .toLocalTime()
                                        .isBefore(
                                                horario.getHoraInicio()
                                        )

                                        &&

                                        !cita.getFechaHoraFin()
                                                .toLocalTime()
                                                .isAfter(
                                                        horario.getHoraFin()
                                                )
                        );

        if (!cubreHorario) {

            throw new IllegalArgumentException(
                    "El médico seleccionado no está disponible en el horario de la cita."
            );
        }

        boolean bloqueado =
                eventoAgendaRepository
                        .existeBloqueoEnRango(
                                nuevoMedico.getId(),
                                cita.getFechaHoraInicio(),
                                cita.getFechaHoraFin()
                        );

        if (bloqueado) {

            throw new IllegalArgumentException(
                    "El médico seleccionado no está disponible en el horario de la cita."
            );
        }

        boolean ocupado =
                citaRepository
                        .existeCitaEnRango(
                                nuevoMedico.getId(),
                                cita.getFechaHoraInicio(),
                                cita.getFechaHoraFin()
                        );

        if (ocupado) {

            throw new IllegalArgumentException(
                    "El médico seleccionado ya tiene una cita en ese horario."
            );
        }

        String nombreMedicoAnterior =
                medicoActual
                        .getPersonal()
                        .getNombreCompleto();

        String nombreMedicoNuevo =
                nuevoMedico
                        .getPersonal()
                        .getNombreCompleto();

        cita.setMedico(
                nuevoMedico
        );

        citaRepository.save(
                cita
        );

        Usuario usuario =
                usuarioRepository
                        .findByNombreUsuarioIgnoreCase(
                                usuarioRecepcionista
                        )
                        .orElse(null);

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                "REASIGNAR_MEDICO"
        );

        auditoria.setTipoEntidad(
                "CITA"
        );

        auditoria.setEntidadId(
                String.valueOf(
                        cita.getId()
                )
        );

        String detalle =
                "Reasignación de médico en cita "
                        + cita.getNumeroCita()
                        + ". Médico anterior: "
                        + nombreMedicoAnterior
                        + ". Nuevo médico: "
                        + nombreMedicoNuevo
                        + ".";

        if (motivo != null
                && !motivo.trim().isEmpty()) {

            detalle +=
                    " Motivo: "
                            + motivo.trim();
        }

        auditoria.setDetalle(
                detalle
        );

        auditoriaRepository.save(
                auditoria
        );
    }

    private String convertirDiaSemanaReasignacion(
            java.time.DayOfWeek dia
    ) {

        return switch (dia) {

            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }

    @Override
    @Transactional
    public CitaRecepcionDTO registrarEmergencia(
            EmergenciaRecepcionDTO dto,
            String usuarioRecepcionista
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Datos de emergencia requeridos."
            );
        }

        String nombre =
                dto.getNombreCompleto() == null
                        ? ""
                        : dto.getNombreCompleto()
                        .trim();

        if (nombre.isEmpty()) {

            throw new IllegalArgumentException(
                    "El campo Nombre es obligatorio."
            );
        }

        String dpi =
                dto.getDpi() == null
                        ? ""
                        : dto.getDpi()
                        .trim();

        if (dpi.isEmpty()) {

            throw new IllegalArgumentException(
                    "El campo DPI es obligatorio. Por favor, ingrese su número de DPI."
            );
        }

        if (!dpi.matches("^[0-9]+$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales."
            );
        }

        if (dpi.length() != 13) {

            throw new IllegalArgumentException(
                    "El DPI debe contener exactamente 13 dígitos. Usted ingresó "
                            + dpi.length()
                            + " dígitos."
            );
        }

        Usuario usuario =
                usuarioRepository
                        .findByNombreUsuarioIgnoreCase(
                                usuarioRecepcionista
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Usuario de recepción no encontrado."
                                )
                        );

        Personal personal =
                personalRepository
                        .findByUsuarioId(
                                usuario.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No se encontró la información del recepcionista."
                                )
                        );

        Sucursal sucursal =
                personal.getSucursal();

        String dpiHash =
                datosSensiblesService
                        .generarHashBusqueda(
                                dpi
                        );

        Paciente paciente =
                pacienteRepository
                        .findByDpiHash(
                                dpiHash
                        )
                        .orElse(null);

        if (paciente == null) {

            paciente =
                    new Paciente();

            paciente.setNombreCompleto(
                    nombre
            );

            paciente.setDpiHash(
                    dpiHash
            );

            paciente.setDpiCifrado(
                    datosSensiblesService
                            .cifrar(
                                    dpi
                            )
            );

            paciente.setUsuario(
                    null
            );

            paciente.setNitCifrado(
                    null
            );

            paciente.setTelefono(
                    null
            );

            paciente.setCorreo(
                    null
            );

            paciente.setNumeroSeguro(
                    null
            );

            paciente =
                    pacienteRepository
                            .save(
                                    paciente
                            );

        } else {

            if (paciente.getNombreCompleto() == null
                    || paciente.getNombreCompleto()
                    .isBlank()) {

                paciente.setNombreCompleto(
                        nombre
                );

                paciente =
                        pacienteRepository
                                .save(
                                        paciente
                                );
            }
        }

        EstadoCita estadoPresente =
                estadoCitaRepository
                        .findByCodigo(
                                "PACIENTE_PRESENTE"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el estado PACIENTE_PRESENTE"
                                )
                        );

        LocalDateTime ahora =
                LocalDateTime.now();

        Cita cita =
                new Cita();

        String numeroCita;

        do {

            numeroCita =
                    "EMG-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase();

        } while (
                citaRepository
                        .existsByNumeroCita(
                                numeroCita
                        )
        );

        cita.setNumeroCita(
                numeroCita
        );

        cita.setPaciente(
                paciente
        );

        cita.setSucursal(
                sucursal
        );

        cita.setEspecialidad(
                null
        );

        cita.setMedico(
                null
        );

        cita.setEstadoCita(
                estadoPresente
        );

        cita.setFechaHoraInicio(
                ahora
        );

        cita.setFechaHoraFin(
                ahora.plusMinutes(30)
        );

        cita.setMotivoConsulta(
                "Atención de emergencia"
        );

        cita.setOrigen(
                "EMERGENCIA"
        );

        cita.setReservaExpiraEn(
                null
        );

        cita.setPrioridadEmergencia(
                true
        );

        cita.setFechaHoraLlegada(
                ahora
        );

        cita =
                citaRepository
                        .save(
                                cita
                        );

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                "REGISTRAR_EMERGENCIA"
        );

        auditoria.setTipoEntidad(
                "CITA"
        );

        auditoria.setEntidadId(
                String.valueOf(
                        cita.getId()
                )
        );

        auditoria.setDetalle(
                "Paciente "
                        + paciente.getNombreCompleto()
                        + " registrado con prioridad de EMERGENCIA. "
                        + "Cita "
                        + cita.getNumeroCita()
                        + " creada en estado PACIENTE_PRESENTE."
        );

        auditoriaRepository.save(
                auditoria
        );

        return convertir(
                cita
        );
    }
}