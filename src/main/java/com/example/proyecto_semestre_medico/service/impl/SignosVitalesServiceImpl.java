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

        signos.setObservaciones(
                dto.getObservaciones() == null
                        ? null
                        : dto.getObservaciones()
                        .trim()
        );

        SignosVitales guardados =
                signosVitalesRepository
                        .save(signos);

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

        notificarMedico(cita);

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

        validarEntero(
                dto.getPresionSistolica(),
                40,
                300,
                "Presión sistólica"
        );

        validarEntero(
                dto.getPresionDiastolica(),
                20,
                200,
                "Presión diastólica"
        );

        if (dto.getPresionSistolica()
                <= dto.getPresionDiastolica()) {

            throw new IllegalArgumentException(
                    "La presión sistólica debe ser mayor que la diastólica"
            );
        }

        validarEntero(
                dto.getPulso(),
                20,
                250,
                "Pulso"
        );

        validarDecimal(
                dto.getTemperatura(),
                new BigDecimal("30.0"),
                new BigDecimal("45.0"),
                "Temperatura"
        );

        validarDecimal(
                dto.getPeso(),
                new BigDecimal("0.01"),
                new BigDecimal("500.00"),
                "Peso"
        );

        validarDecimal(
                dto.getTalla(),
                new BigDecimal("1.00"),
                new BigDecimal("300.00"),
                "Talla"
        );

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