package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.ConsultaMedicaFormDTO;
import com.example.proyecto_semestre_medico.dto.MedicoCitaPanelDTO;
import com.example.proyecto_semestre_medico.model.Cie10;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.ConsultaMedica;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.repository.Cie10Repository;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.ConsultaMedicaRepository;
import com.example.proyecto_semestre_medico.service.ConsultaMedicaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import com.example.proyecto_semestre_medico.repository.SignosVitalesRepository;
import com.example.proyecto_semestre_medico.model.Medico;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultaMedicaServiceImpl
        implements ConsultaMedicaService {

    private final CitaRepository citaRepository;
    private final ConsultaMedicaRepository consultaMedicaRepository;
    private final Cie10Repository cie10Repository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final MedicoRepository medicoRepository;
    private final SignosVitalesRepository signosVitalesRepository;

    public ConsultaMedicaServiceImpl(
            CitaRepository citaRepository,
            ConsultaMedicaRepository consultaMedicaRepository,
            Cie10Repository cie10Repository,
            EstadoCitaRepository estadoCitaRepository,
            MedicoRepository medicoRepository,
            SignosVitalesRepository signosVitalesRepository
    ) {
        this.citaRepository = citaRepository;
        this.consultaMedicaRepository = consultaMedicaRepository;
        this.cie10Repository = cie10Repository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.medicoRepository = medicoRepository;
        this.signosVitalesRepository = signosVitalesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoCitaPanelDTO> listarEnEspera(
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedicoAutenticado(
                        nombreUsuario
                );

        return citaRepository
                .listarEnEsperaParaMedico(
                        medico.getId(),

                        medico.getPersonal()
                                .getSucursal()
                                .getId()
                )
                .stream()
                .map(this::convertirPanel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoCitaPanelDTO> listarEnConsulta(
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedicoAutenticado(
                        nombreUsuario
                );

        return citaRepository
                .findByMedicoIdAndEstadoCitaCodigoOrderByFechaHoraInicioAsc(
                        medico.getId(),
                        "CONSULTA_MEDICA"
                )
                .stream()
                .map(this::convertirPanel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoCitaPanelDTO> listarPendientesCierre(
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedicoAutenticado(
                        nombreUsuario
                );

        return citaRepository
                .findByMedicoIdAndEstadoCitaCodigoOrderByFechaHoraInicioAsc(
                        medico.getId(),
                        "PENDIENTE_CIERRE"
                )
                .stream()
                .map(this::convertirPanel)
                .toList();
    }

    private EstadoCita obtenerEstadoCita(String codigo) {

        return estadoCitaRepository
                .findByCodigo(codigo)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el estado de cita: " + codigo
                        )
                );
    }

    @Override
    @Transactional
    public void iniciarConsulta(
            Long citaId,
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedicoAutenticado(
                        nombreUsuario
                );

        Cita cita =
                citaRepository
                        .buscarParaIniciarConsulta(
                                citaId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La cita no existe."
                                )
                        );

        if (!"EN_ESPERA".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "Solo se puede iniciar una consulta para una cita en espera."
            );
        }

        if (!signosVitalesRepository.existsByCitaId(citaId)) {
            throw new IllegalArgumentException(
                    "El paciente debe tener signos vitales registrados antes de iniciar la consulta."
            );
        }

        /*
         * Emergencia todavía sin médico:
         * el médico que la toma queda asignado.
         */
        if (cita.getMedico() == null) {

            if (!Boolean.TRUE.equals(
                    cita.getPrioridadEmergencia()
            )) {

                throw new IllegalArgumentException(
                        "La cita no tiene un médico asignado."
                );
            }

            if (!cita.getSucursal()
                    .getId()
                    .equals(
                            medico.getPersonal()
                                    .getSucursal()
                                    .getId()
                    )) {

                throw new IllegalArgumentException(
                        "La emergencia pertenece a otra sucursal."
                );
            }

            cita.setMedico(
                    medico
            );

            cita.setEspecialidad(
                    medico.getEspecialidad()
            );

        } else {

            if (!cita.getMedico()
                    .getId()
                    .equals(
                            medico.getId()
                    )) {

                throw new IllegalArgumentException(
                        "La cita no está asignada al médico autenticado."
                );
            }
        }

        if (consultaMedicaRepository
                .existsByCitaId(
                        citaId
                )) {

            throw new IllegalArgumentException(
                    "La consulta médica ya fue iniciada."
            );
        }

        ConsultaMedica consulta =
                new ConsultaMedica();

        consulta.setCita(
                cita
        );

        consulta.setMedico(
                medico
        );

        consulta.setMotivoVisita(
                cita.getMotivoConsulta()
        );

        consulta.setEstado(
                "EN_CURSO"
        );

        consulta.setFechaInicio(
                LocalDateTime.now()
        );

        consultaMedicaRepository.save(
                consulta
        );

        cita.setEstadoCita(
                obtenerEstadoCita(
                        "CONSULTA_MEDICA"
                )
        );

        citaRepository.save(
                cita
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultaMedicaFormDTO obtenerConsulta(
            Long citaId,
            String nombreUsuario
    ) {

        obtenerCitaDelMedico(
                citaId,
                nombreUsuario
        );

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findByCitaId(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La consulta médica no existe."
                                )
                        );

        ConsultaMedicaFormDTO dto =
                new ConsultaMedicaFormDTO();

        dto.setConsultaId(
                consulta.getId()
        );

        dto.setCitaId(
                consulta.getCita().getId()
        );

        dto.setNumeroCita(
                consulta.getCita()
                        .getNumeroCita()
        );

        dto.setPaciente(
                consulta.getCita()
                        .getPaciente()
                        .getNombreCompleto()
        );

        dto.setMotivoVisita(
                consulta.getMotivoVisita()
        );

        dto.setHallazgosClinicos(
                consulta.getHallazgosClinicos()
        );

        dto.setDiagnostico(
                consulta.getDiagnostico()
        );

        dto.setPlanTratamiento(
                consulta.getPlanTratamiento()
        );

        dto.setNotasAdicionales(
                consulta.getNotasAdicionales()
        );

        dto.setEstado(
                consulta.getEstado()
        );

        if (consulta.getCie10() != null) {

            dto.setCie10Id(
                    consulta.getCie10().getId()
            );

            dto.setCodigoCie10(
                    consulta.getCie10().getCodigo()
            );

            dto.setDescripcionCie10(
                    consulta.getCie10()
                            .getDescripcion()
            );
        }

        return dto;
    }

    @Override
    @Transactional
    public void guardarConsulta(
            ConsultaMedicaFormDTO dto,
            String nombreUsuario
    ) {

        if (dto == null || dto.getCitaId() == null) {
            throw new IllegalArgumentException(
                    "La cita es obligatoria."
            );
        }
        Cita citaValidada = obtenerCitaDelMedico(
                dto.getCitaId(),
                nombreUsuario
        );

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findByCitaId(dto.getCitaId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La consulta médica no existe."
                                )
                        );

        if (!"CONSULTA_MEDICA".equals(citaValidada.getEstadoCita().getCodigo())
                || !"EN_CURSO".equals(consulta.getEstado())) {
            throw new IllegalArgumentException("La cita no se encuentra en consulta médica en curso.");
        }
        if (!"EN_CURSO".equals(dto.getEstado()) && !"FINALIZADA".equals(dto.getEstado())) {
            throw new IllegalArgumentException("Estado de consulta inválido.");
        }
        boolean finalizar = "FINALIZADA".equals(dto.getEstado());
        if (finalizar) {
            validarParaFinalizar(dto);
        }

        validarTextoObligatorio(
                dto.getMotivoVisita(),
                "El motivo de visita es obligatorio."
        );

        consulta.setMotivoVisita(
                dto.getMotivoVisita().trim()
        );

        consulta.setHallazgosClinicos(
                limpiar(dto.getHallazgosClinicos())
        );

        consulta.setDiagnostico(
                limpiar(dto.getDiagnostico())
        );

        consulta.setPlanTratamiento(
                limpiar(dto.getPlanTratamiento())
        );

        consulta.setNotasAdicionales(
                limpiar(dto.getNotasAdicionales())
        );

        if (dto.getCie10Id() != null) {

            Cie10 cie10 =
                    cie10Repository
                            .findById(dto.getCie10Id())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El código CIE-10 seleccionado no existe."
                                    )
                            );

            consulta.setCie10(cie10);

        } else {
            consulta.setCie10(null);
        }

        if (finalizar) {

            consulta.setEstado(
                    "FINALIZADA"
            );

            consulta.setFechaFinalizacion(
                    LocalDateTime.now()
            );

            Cita cita = consulta.getCita();

            if (!"CONSULTA_MEDICA".equals(
                    cita.getEstadoCita().getCodigo()
            )) {
                throw new IllegalArgumentException(
                        "La cita no se encuentra en consulta médica."
                );
            }

            cita.setEstadoCita(
                    obtenerEstadoCita(
                            "PENDIENTE_CIERRE"
                    )
            );

            citaRepository.save(cita);

        } else {

            consulta.setEstado(
                    "EN_CURSO"
            );
        }

        consultaMedicaRepository.save(consulta);
    }

    @Override
    @Transactional
    public String marcarNoAsistio(Long citaId,
                                String nombreUsuario) {

        Cita cita =
                obtenerCitaDelMedico(
                        citaId,
                        nombreUsuario
                );

        if (!"EN_ESPERA".equals(
                cita.getEstadoCita().getCodigo()
        )) {
            throw new IllegalArgumentException(
                    "Solo una cita en espera puede marcarse como No Asistió."
            );
        }

        if (consultaMedicaRepository
                .existsByCitaId(citaId)) {

            throw new IllegalArgumentException(
                    "No puede marcarse como No Asistió porque la consulta ya fue iniciada."
            );
        }

        cita.setEstadoCita(
                obtenerEstadoCita(
                        "NO_ASISTIO"
                )
        );

        citaRepository.save(cita);
        return cita.getNumeroCita();
    }

    private Medico obtenerMedicoAutenticado(
            String nombreUsuario
    ) {

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
    private Cita obtenerCitaDelMedico(
            Long citaId,
            String nombreUsuario
    ) {

        Medico medico =
                obtenerMedicoAutenticado(
                        nombreUsuario
                );

        Cita cita =
                citaRepository.findById(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La cita no existe."
                                )
                        );

        if (cita.getMedico() == null || !cita.getMedico()
                .getId()
                .equals(medico.getId())) {

            throw new IllegalArgumentException(
                    "La cita no está asignada al médico autenticado."
            );
        }

        return cita;
    }

    @Override
    @Transactional
    public String finalizarAtencion(Long citaId,
                                  String nombreUsuario) {

        Cita cita =
                obtenerCitaDelMedico(
                        citaId,
                        nombreUsuario
                );

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findByCitaId(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La consulta médica no existe."
                                )
                        );

        if (!"FINALIZADA".equals(
                consulta.getEstado()
        )) {
            throw new IllegalArgumentException(
                    "La consulta médica debe estar finalizada antes de cerrar la atención."
            );
        }

        if (!"PENDIENTE_CIERRE".equals(
                cita.getEstadoCita().getCodigo()
        )) {
            throw new IllegalArgumentException(
                    "La cita no se encuentra pendiente de cierre."
            );
        }

        cita.setEstadoCita(
                obtenerEstadoCita(
                        "ATENCION_FINALIZADA"
                )
        );

        citaRepository.save(cita);
        return cita.getNumeroCita();
    }

    private void validarParaFinalizar(
            ConsultaMedicaFormDTO dto
    ) {

        validarTextoObligatorio(
                dto.getMotivoVisita(),
                "Debe completar todos los campos obligatorios para cerrar la consulta."
        );

        validarTextoObligatorio(
                dto.getHallazgosClinicos(),
                "Debe completar todos los campos obligatorios para cerrar la consulta."
        );

        validarTextoObligatorio(
                dto.getPlanTratamiento(),
                "Debe completar todos los campos obligatorios para cerrar la consulta."
        );

        validarTextoObligatorio(
                dto.getDiagnostico(),
                "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres."
        );

        String diagnostico =
                dto.getDiagnostico().trim();

        if (diagnostico.length() < 10
                || diagnostico.length() > 5000) {

            throw new IllegalArgumentException(
                    "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres."
            );
        }
    }

    private void validarTextoObligatorio(
            String valor,
            String mensaje
    ) {

        if (valor == null
                || valor.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    mensaje
            );
        }
    }

    private String limpiar(String valor) {

        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();

        return limpio.isEmpty()
                ? null
                : limpio;
    }
    private MedicoCitaPanelDTO convertirPanel(
            Cita cita
    ) {

        MedicoCitaPanelDTO dto =
                new MedicoCitaPanelDTO();

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

        dto.setEspecialidad(
                cita.getEspecialidad() != null
                        ? cita.getEspecialidad()
                        .getNombre()
                        : "Emergencia - pendiente de asignación"
        );

        dto.setFechaHora(
                cita.getFechaHoraInicio()
        );

        dto.setEstado(
                cita.getEstadoCita()
                        .getCodigo()
        );

        dto.setEmergencia(
                Boolean.TRUE.equals(
                        cita.getPrioridadEmergencia()
                )
        );

        return dto;
    }


}
