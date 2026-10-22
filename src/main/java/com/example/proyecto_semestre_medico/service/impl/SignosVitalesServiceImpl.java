package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.SignosVitalesDTO;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.SignosVitalesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SignosVitalesServiceImpl
        implements SignosVitalesService {

    private final CitaRepository citaRepository;
    private final SignosVitalesRepository signosVitalesRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final NotificacionInternaRepository notificacionInternaRepository;
    private final AuditoriaRepository auditoriaRepository;

    public SignosVitalesServiceImpl(
            CitaRepository citaRepository,
            SignosVitalesRepository signosVitalesRepository,
            UsuarioRepository usuarioRepository,
            EstadoCitaRepository estadoCitaRepository,
            NotificacionInternaRepository notificacionInternaRepository,
            AuditoriaRepository auditoriaRepository
    ) {
        this.citaRepository =
                citaRepository;

        this.signosVitalesRepository =
                signosVitalesRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.estadoCitaRepository =
                estadoCitaRepository;

        this.notificacionInternaRepository =
                notificacionInternaRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> listarPacientesPresentes() {

        return citaRepository
                .listarPacientesParaEnfermeria();
    }

    @Override
    @Transactional
    public void iniciarToma(
            Long citaId
    ) {

        Cita cita =
                citaRepository
                        .findById(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada"
                                )
                        );

        if (!"PACIENTE_PRESENTE".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "El paciente no se encuentra disponible para toma de signos"
            );
        }

        if (signosVitalesRepository
                .existsByCitaId(citaId)) {

            throw new IllegalArgumentException(
                    "La cita ya posee signos vitales registrados"
            );
        }

        EstadoCita estado =
                estadoCitaRepository
                        .findByCodigo(
                                "SIGNOS_VITALES"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el estado SIGNOS_VITALES"
                                )
                        );

        cita.setEstadoCita(estado);

        citaRepository.save(cita);
    }

    @Override
    @Transactional(readOnly = true)
    public Cita buscarCitaParaSignos(
            Long citaId
    ) {

        if (citaId == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una cita"
            );
        }

        Cita cita =
                citaRepository
                        .buscarCitaCompletaPorId(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada"
                                )
                        );

        String estado =
                cita.getEstadoCita()
                        .getCodigo();

        if (!"SIGNOS_VITALES".equals(estado)) {

            throw new IllegalArgumentException(
                    "La cita no se encuentra en toma de signos vitales. Estado actual: "
                            + estado
            );
        }

        return cita;
    }

    @Override
    @Transactional
    public SignosVitales registrar(
            String usuarioEnfermeria,
            SignosVitalesDTO dto
    ) {

        validar(dto);

        Cita cita =
                citaRepository
                        .findById(dto.getCitaId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada"
                                )
                        );

        if (!"SIGNOS_VITALES".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "La cita no está en toma de signos vitales"
            );
        }

        if (signosVitalesRepository
                .existsByCitaId(cita.getId())) {

            throw new IllegalArgumentException(
                    "Ya existen signos vitales registrados para esta cita"
            );
        }

        Usuario enfermeria =
                usuarioRepository
                        .findByNombreUsuarioIgnoreCase(
                                usuarioEnfermeria
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Usuario de enfermería no encontrado"
                                )
                        );

        SignosVitales signos =
                new SignosVitales();

        signos.setCita(cita);
        signos.setUsuarioEnfermeria(
                enfermeria
        );

        signos.setPresionSistolica(
                dto.getPresionSistolica()
        );

        signos.setPresionDiastolica(
                dto.getPresionDiastolica()
        );

        signos.setPulso(
                dto.getPulso()
        );

        signos.setTemperatura(
                dto.getTemperatura()
        );

        signos.setPeso(
                dto.getPeso()
        );

        signos.setTalla(
                dto.getTalla()
        );

        signos.setEmergencia(
                Boolean.TRUE.equals(
                        dto.getEmergencia()
                )
        );

        signos.setObservaciones(
                dto.getObservaciones() == null
                        ? null
                        : dto.getObservaciones()
                        .trim()
        );

        SignosVitales guardados =
                signosVitalesRepository
                        .save(signos);

        if (Boolean.TRUE.equals(
                dto.getEmergencia()
        )) {

            cita.setPrioridadEmergencia(
                    true
            );
        }

        /*
         * Finalizada la toma de signos,
         * el paciente pasa a espera del médico.
         */
        EstadoCita espera =
                estadoCitaRepository
                        .findByCodigo(
                                "EN_ESPERA"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el estado EN_ESPERA"
                                )
                        );

        cita.setEstadoCita(espera);

        citaRepository.save(cita);

        if (cita.getMedico() != null) {

            notificarMedico(
                    cita
            );
        }

        registrarAuditoria(
                cita,
                guardados,
                enfermeria
        );

        return guardados;
    }

    private void validar(
            SignosVitalesDTO dto
    ) {

        if (dto == null
                || dto.getCitaId() == null) {

            throw new IllegalArgumentException(
                    "Cita requerida"
            );
        }

        // RN-CU07-01
        if (dto.getPresionSistolica() == null
                || dto.getPresionDiastolica() == null
                || dto.getPresionSistolica() < 60
                || dto.getPresionSistolica() > 250
                || dto.getPresionDiastolica() < 40
                || dto.getPresionDiastolica() > 150
                || dto.getPresionSistolica()
                <= dto.getPresionDiastolica()) {

            throw new IllegalArgumentException(
                    "La presión arterial debe ingresarse en formato sistólica/diastólica (ej: 120/80) dentro de rangos válidos."
            );
        }

        // RN-CU07-02
        if (dto.getTemperatura() == null
                || dto.getTemperatura()
                .compareTo(
                        new BigDecimal("34.0")
                ) < 0
                || dto.getTemperatura()
                .compareTo(
                        new BigDecimal("42.0")
                ) > 0
                || dto.getTemperatura()
                .scale() > 1) {

            throw new IllegalArgumentException(
                    "La temperatura debe estar entre 34.0 y 42.0°C con un decimal."
            );
        }

        // RN-CU07-03
        if (dto.getPeso() == null
                || dto.getPeso()
                .compareTo(
                        new BigDecimal("0.50")
                ) < 0
                || dto.getPeso()
                .compareTo(
                        new BigDecimal("300.00")
                ) > 0
                || dto.getPeso()
                .scale() > 2) {

            throw new IllegalArgumentException(
                    "El peso debe estar entre 0.5 y 300 kg con dos decimales."
            );
        }

        // RN-CU07-04
        if (dto.getTalla() == null
                || dto.getTalla()
                .compareTo(
                        new BigDecimal("30.00")
                ) < 0
                || dto.getTalla()
                .compareTo(
                        new BigDecimal("250.00")
                ) > 0
                || dto.getTalla()
                .scale() > 2) {

            throw new IllegalArgumentException(
                    "La talla debe estar entre 30 y 250 cm con dos decimales."
            );
        }

        // RN-CU07-05
        if (dto.getPulso() == null
                || dto.getPulso() < 30
                || dto.getPulso() > 220) {

            throw new IllegalArgumentException(
                    "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto."
            );
        }

        if (dto.getObservaciones() != null
                && dto.getObservaciones()
                .trim()
                .length() > 1000) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar 1000 caracteres"
            );
        }
    }

    private void validarEntero(
            Integer valor,
            int minimo,
            int maximo,
            String campo
    ) {

        if (valor == null
                || valor < minimo
                || valor > maximo) {

            throw new IllegalArgumentException(
                    campo
                            + " fuera del rango permitido"
            );
        }
    }

    private void validarDecimal(
            BigDecimal valor,
            BigDecimal minimo,
            BigDecimal maximo,
            String campo
    ) {

        if (valor == null
                || valor.compareTo(minimo) < 0
                || valor.compareTo(maximo) > 0) {

            throw new IllegalArgumentException(
                    campo
                            + " fuera del rango permitido"
            );
        }
    }

    private void notificarMedico(
            Cita cita
    ) {

        Usuario usuarioMedico =
                cita.getMedico()
                        .getPersonal()
                        .getUsuario();

        NotificacionInterna notificacion =
                new NotificacionInterna();

        notificacion.setUsuarioDestino(
                usuarioMedico
        );

        notificacion.setTipo(
                "PACIENTE_LISTO"
        );

        notificacion.setTitulo(
                "Paciente listo para consulta"
        );

        notificacion.setMensaje(
                "El paciente "
                        + cita.getPaciente()
                        .getNombreCompleto()
                        + " finalizó la toma de signos vitales para la cita "
                        + cita.getNumeroCita()
        );

        notificacion.setReferenciaTipo(
                "CITA"
        );

        notificacion.setReferenciaId(
                String.valueOf(
                        cita.getId()
                )
        );

        notificacionInternaRepository.save(
                notificacion
        );
    }

    private void registrarAuditoria(
            Cita cita,
            SignosVitales signos,
            Usuario usuario
    ) {

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuario);

        auditoria.setAccion(
                "REGISTRAR_SIGNOS_VITALES"
        );

        auditoria.setTipoEntidad(
                "SIGNOS_VITALES"
        );

        auditoria.setEntidadId(
                String.valueOf(
                        signos.getId()
                )
        );

        auditoria.setDetalle(
                "Signos vitales registrados para cita "
                        + cita.getNumeroCita()
                        + ". Nuevo estado: EN_ESPERA"
        );

        auditoriaRepository.save(
                auditoria
        );
    }
}