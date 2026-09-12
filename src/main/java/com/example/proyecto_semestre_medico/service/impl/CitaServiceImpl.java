package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.AgendarCitaDTO;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.CitaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.proyecto_semestre_medico.service.AlmacenamientoDocumentoService;
import com.example.proyecto_semestre_medico.service.AntivirusService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final SucursalRepository sucursalRepository;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final SucursalEspecialidadRepository sucursalEspecialidadRepository;
    private final EventoAgendaRepository eventoAgendaRepository;
    private final HorarioMedicoRepository horarioMedicoRepository;
    private final DocumentoCitaRepository documentoCitaRepository;
    private final AlmacenamientoDocumentoService almacenamientoDocumentoService;
    private final AntivirusService antivirusService;

    private final int duracionMinutos;
    private final int reservaMinutos;

    public CitaServiceImpl(
            CitaRepository citaRepository,
            PacienteRepository pacienteRepository,
            SucursalRepository sucursalRepository,
            EspecialidadRepository especialidadRepository,
            MedicoRepository medicoRepository,
            EstadoCitaRepository estadoCitaRepository,
            SucursalEspecialidadRepository sucursalEspecialidadRepository,
            EventoAgendaRepository eventoAgendaRepository,
            HorarioMedicoRepository horarioMedicoRepository,
            DocumentoCitaRepository documentoCitaRepository,
            AlmacenamientoDocumentoService almacenamientoDocumentoService,
            AntivirusService antivirusService,
            @Value("${app.citas.duracion-minutos:30}")
            int duracionMinutos,
            @Value("${app.citas.reserva-minutos:5}")
            int reservaMinutos
    ) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.sucursalRepository = sucursalRepository;
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.sucursalEspecialidadRepository = sucursalEspecialidadRepository;
        this.eventoAgendaRepository = eventoAgendaRepository;
        this.horarioMedicoRepository = horarioMedicoRepository;
        this.duracionMinutos = duracionMinutos;
        this.reservaMinutos = reservaMinutos;
        this.documentoCitaRepository =
                documentoCitaRepository;

        this.almacenamientoDocumentoService =
                almacenamientoDocumentoService;

        this.antivirusService =
                antivirusService;
    }

    @Override
    @Transactional
    public Cita agendar(
            String nombreUsuarioPaciente,
            AgendarCitaDTO dto
    ) {

        validar(dto);

        Paciente paciente = pacienteRepository
                .findByUsuarioNombreUsuarioIgnoreCase(
                        nombreUsuarioPaciente
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El usuario autenticado no corresponde a un paciente"
                        )
                );

        Sucursal sucursal = sucursalRepository
                .findById(dto.getSucursalId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sucursal no válida"
                        )
                );

        Especialidad especialidad = especialidadRepository
                .findById(dto.getEspecialidadId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Especialidad no válida"
                        )
                );

        Medico medico = medicoRepository
                .findById(dto.getMedicoId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Médico no válido"
                        )
                );

        boolean especialidadEnSucursal =
                sucursalEspecialidadRepository
                        .existsBySucursalIdAndEspecialidadIdAndActivoTrue(
                                sucursal.getId(),
                                especialidad.getId()
                        );

        if (!especialidadEnSucursal) {
            throw new IllegalArgumentException(
                    "La especialidad no está disponible en la sucursal"
            );
        }

        if (!medico.getPersonal()
                .getSucursal()
                .getId()
                .equals(sucursal.getId())) {

            throw new IllegalArgumentException(
                    "El médico no pertenece a la sucursal seleccionada"
            );
        }

        if (!medico.getEspecialidad()
                .getId()
                .equals(especialidad.getId())) {

            throw new IllegalArgumentException(
                    "El médico no pertenece a la especialidad seleccionada"
            );
        }

        LocalDateTime inicio =
                dto.getFechaHoraInicio();

        LocalDateTime fin =
                inicio.plusMinutes(
                        duracionMinutos
                );

        if (!inicio.isAfter(
                LocalDateTime.now()
        )) {
            throw new IllegalArgumentException(
                    "La fecha y hora deben ser futuras"
            );
        }

        String diaSemana =
                convertirDiaSemana(
                        inicio.getDayOfWeek()
                                .name()
                );

        boolean dentroDeHorario =
                horarioMedicoRepository
                        .findByMedicoIdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
                                medico.getId(),
                                diaSemana
                        )
                        .stream()
                        .anyMatch(h ->
                                !inicio.toLocalTime()
                                        .isBefore(
                                                h.getHoraInicio()
                                        )
                                        &&
                                        !fin.toLocalTime()
                                                .isAfter(
                                                        h.getHoraFin()
                                                )
                        );

        if (!dentroDeHorario) {
            throw new IllegalArgumentException(
                    "El horario seleccionado no corresponde a la jornada del médico"
            );
        }

        if (eventoAgendaRepository
                .existeBloqueoEnRango(
                        medico.getId(),
                        inicio,
                        fin
                )) {

            throw new IllegalArgumentException(
                    "El médico no está disponible en ese horario"
            );
        }

        /*
         * REVALIDACIÓN JUSTO ANTES DE GUARDAR.
         */
        if (citaRepository
                .existeCitaEnRango(
                        medico.getId(),
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
                                        "No existe el estado PENDIENTE_PAGO"
                                )
                        );

        Cita cita = new Cita();

        cita.setNumeroCita(
                generarNumeroCita()
        );

        cita.setPaciente(paciente);
        cita.setSucursal(sucursal);
        cita.setEspecialidad(especialidad);
        cita.setMedico(medico);
        cita.setEstadoCita(estado);

        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(fin);

        cita.setMotivoConsulta(
                dto.getMotivoConsulta()
                        .trim()
        );

        cita.setOrigen("PORTAL");

        cita.setReservaExpiraEn(
                LocalDateTime.now()
                        .plusMinutes(
                                reservaMinutos
                        )
        );

        Cita citaGuardada =
                citaRepository.save(cita);

        guardarDocumentoSiExiste(
                citaGuardada,
                dto
        );

        return citaGuardada;
    }

    private void validar(
            AgendarCitaDTO dto
    ) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Datos de cita requeridos"
            );
        }

        if (dto.getSucursalId() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal"
            );
        }

        if (dto.getEspecialidadId() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una especialidad"
            );
        }

        if (dto.getMedicoId() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un médico"
            );
        }

        if (dto.getFechaHoraInicio() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar fecha y hora"
            );
        }

        if (dto.getMotivoConsulta() == null
                || dto.getMotivoConsulta()
                .trim()
                .length() < 10
                || dto.getMotivoConsulta()
                .trim()
                .length() > 2000) {

            throw new IllegalArgumentException(
                    "El motivo debe tener entre 10 y 2000 caracteres"
            );
        }
    }

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
                        .existsByNumeroCita(numero)
        );

        return numero;
    }

    private String convertirDiaSemana(
            String dayOfWeek
    ) {

        return switch (dayOfWeek) {
            case "MONDAY" -> "LUNES";
            case "TUESDAY" -> "MARTES";
            case "WEDNESDAY" -> "MIERCOLES";
            case "THURSDAY" -> "JUEVES";
            case "FRIDAY" -> "VIERNES";
            case "SATURDAY" -> "SABADO";
            case "SUNDAY" -> "DOMINGO";
            default -> throw new IllegalArgumentException(
                    "Día de semana no válido"
            );
        };
    }

    private void guardarDocumentoSiExiste(
            Cita cita,
            AgendarCitaDTO dto
    ) {

        if (dto.getDocumento() == null
                || dto.getDocumento().isEmpty()) {

            return;
        }

        var archivo =
                dto.getDocumento();

        if (archivo.getSize() > 2 * 1024 * 1024) {

            throw new IllegalArgumentException(
                    "El documento no puede superar 2 MB"
            );
        }

        if (!"application/pdf".equalsIgnoreCase(
                archivo.getContentType()
        )) {

            throw new IllegalArgumentException(
                    "Solo se permiten archivos PDF"
            );
        }

        String nombreOriginal =
                archivo.getOriginalFilename();

        if (nombreOriginal == null
                || !nombreOriginal
                .toLowerCase()
                .endsWith(".pdf")) {

            throw new IllegalArgumentException(
                    "El archivo debe tener extensión PDF"
            );
        }

        antivirusService.validar(
                archivo
        );

        var resultado =
                almacenamientoDocumentoService
                        .guardar(archivo);

        DocumentoCita documento =
                new DocumentoCita();

        documento.setCita(cita);
        documento.setNombreOriginal(
                nombreOriginal
        );
        documento.setNombreAlmacenado(
                resultado.nombreAlmacenado()
        );
        documento.setRutaArchivo(
                resultado.rutaArchivo()
        );
        documento.setTipoMime(
                archivo.getContentType()
        );
        documento.setTamanoBytes(
                archivo.getSize()
        );
        documento.setActivo(true);

        documentoCitaRepository.save(
                documento
        );
    }
}