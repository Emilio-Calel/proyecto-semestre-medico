package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.RecetaDetalleFormDTO;
import com.example.proyecto_semestre_medico.dto.RecetaFormDTO;
import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.ConsultaMedica;
import com.example.proyecto_semestre_medico.model.EstadoReceta;
import com.example.proyecto_semestre_medico.model.Medicamento;
import com.example.proyecto_semestre_medico.model.Receta;
import com.example.proyecto_semestre_medico.model.RecetaDetalle;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.ConsultaMedicaRepository;
import com.example.proyecto_semestre_medico.repository.MedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.RecetaRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.RecetaService;
import com.example.proyecto_semestre_medico.model.Medico;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecetaServiceImpl
        implements RecetaService {

    private final RecetaRepository
            recetaRepository;

    private final ConsultaMedicaRepository
            consultaMedicaRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    private final MedicoRepository
            medicoRepository;

    public RecetaServiceImpl(
            RecetaRepository recetaRepository,
            ConsultaMedicaRepository consultaMedicaRepository,
            MedicamentoRepository medicamentoRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            MedicoRepository medicoRepository
    ) {
        this.recetaRepository =
                recetaRepository;

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.medicamentoRepository =
                medicamentoRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;

        this.medicoRepository =
                medicoRepository;
    }

    @Override
    @Transactional
    public Receta crearReceta(
            RecetaFormDTO dto,
            String nombreUsuario
    ) {

        validar(dto);

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findByCitaId(
                                dto.getCitaId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consulta médica no encontrada."
                                )
                        );

        /*
         * Verificamos que la consulta realmente
         * pertenezca al médico autenticado.
         *
         * No confiamos únicamente en el citaId
         * recibido desde el formulario.
         */
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

        if (consulta.getMedico() == null
                || consulta.getMedico().getId() == null
                || !consulta
                .getMedico()
                .getId()
                .equals(
                        medicoAutenticado.getId()
                )) {

            throw new IllegalArgumentException(
                    "No tiene autorización para generar una receta para esta consulta."
            );
        }

        if (!"FINALIZADA".equalsIgnoreCase(
                consulta.getEstado()
        )) {

            throw new IllegalArgumentException(
                    "La consulta debe estar finalizada antes de generar una receta."
            );
        }

        if (!"PENDIENTE_CIERRE".equals(consulta.getCita().getEstadoCita().getCodigo())) {
            throw new IllegalArgumentException("Solo pueden generarse recetas para citas pendientes de cierre.");
        }

        Usuario usuario =
                buscarUsuario(
                        nombreUsuario
                );

        Receta receta =
                new Receta();

        receta.setConsultaMedica(
                consulta
        );

        LocalDateTime ahora =
                LocalDateTime.now();

        receta.setFechaEmision(
                ahora
        );

        receta.setFechaVencimiento(
                ahora.plusDays(7)
        );

        receta.setObservaciones(
                normalizarOpcional(
                        dto.getObservaciones()
                )
        );

        receta.setEstado(
                EstadoReceta.PENDIENTE
        );

        receta.setActivo(true);

        List<RecetaDetalle> detalles =
                new ArrayList<>();

        for (RecetaDetalleFormDTO detalleDTO
                : dto.getDetalles()) {

            Medicamento medicamento =
                    medicamentoRepository
                            .findById(
                                    detalleDTO
                                            .getMedicamentoId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Medicamento no encontrado."
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    medicamento.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "El medicamento "
                                + medicamento.getNombre()
                                + " está inactivo."
                );
            }

            RecetaDetalle detalle =
                    new RecetaDetalle();

            detalle.setReceta(
                    receta
            );

            detalle.setMedicamento(
                    medicamento
            );

            detalle.setDosis(
                    detalleDTO
                            .getDosis()
                            .trim()
            );

            detalle.setCantidad(
                    detalleDTO
                            .getCantidad()
            );

            detalle.setIndicaciones(
                    indicacionesCompletas(detalleDTO)
            );

            detalle.setActivo(true);

            detalles.add(
                    detalle
            );
        }

        receta.setDetalles(
                detalles
        );

        receta =
                recetaRepository
                        .save(receta);

        registrarAuditoria(
                usuario,
                receta
        );

        return receta;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('FARMACEUTICO') or hasRole('ADMINISTRADOR')")
    public List<Receta> buscarActivasFarmacia(Long recetaId, Long consultaId) {
        if ((recetaId != null && recetaId <= 0) || (consultaId != null && consultaId <= 0)) {
            throw new IllegalArgumentException("Los identificadores de receta y consulta deben ser positivos.");
        }
        return recetaRepository.buscarActivasFarmacia(recetaId, consultaId);
    }

    @Override
    @Transactional
    public Receta buscarPorId(
            Long id
    ) {

        Receta receta =
                recetaRepository
                        .buscarParaDespacho(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Receta no encontrada."
                                )
                        );

        if (verificarVencimiento(receta)) {

            receta =
                    recetaRepository
                            .save(receta);
        }

        return receta;
    }

    @Override
    @Transactional
    public List<Receta> listarTodas() {

        List<Receta> recetas =
                recetaRepository
                        .findAllByOrderByFechaEmisionDesc();

        for (Receta receta : recetas) {

            if (verificarVencimiento(
                    receta
            )) {

                recetaRepository.save(
                        receta
                );
            }
        }

        return recetas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicamento>
    listarMedicamentosActivos() {

        return medicamentoRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional
    @PreAuthorize("(hasRole('FARMACEUTICO') or hasRole('ADMINISTRADOR')) and #nombreUsuario == authentication.name")
    public Receta marcarComoNoAdquirida(
            Long recetaId,
            String nombreUsuario
    ) {

        if (recetaId == null) {

            throw new IllegalArgumentException(
                    "La receta es obligatoria."
            );
        }

        Receta receta =
                recetaRepository
                        .buscarParaDespacho(recetaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Receta no encontrada."
                                )
                        );

        /*
         * Antes de cambiar el estado verificamos
         * si la receta ya venció.
         */
        if (verificarVencimiento(receta)) {

            recetaRepository.save(
                    receta
            );
        }

        if (!Boolean.TRUE.equals(
                receta.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La receta se encuentra inactiva."
            );
        }

        if (receta.getEstado()
                == EstadoReceta.VENCIDA) {

            throw new IllegalArgumentException(
                    "Una receta vencida no puede marcarse como no adquirida."
            );
        }

        if (receta.getEstado()
                == EstadoReceta.DESPACHADA) {

            throw new IllegalArgumentException(
                    "Una receta completamente despachada no puede marcarse como no adquirida."
            );
        }

        if (receta.getEstado()
                == EstadoReceta.NO_ADQUIRIDA) {

            throw new IllegalArgumentException(
                    "La receta ya fue marcada como no adquirida."
            );
        }

        if (receta.getEstado()
                != EstadoReceta.PENDIENTE
                && receta.getEstado()
                != EstadoReceta.PARCIAL) {

            throw new IllegalArgumentException(
                    "El estado actual de la receta no permite esta operación."
            );
        }

        Usuario usuarioNoAdquiere = buscarUsuario(nombreUsuario);
        if (usuarioNoAdquiere == null) throw new IllegalArgumentException("El usuario de farmacia no existe.");

        receta.setEstado(EstadoReceta.NO_ADQUIRIDA);

        receta =
                recetaRepository.save(
                        receta
                );

        Usuario usuario =
                buscarUsuario(
                        nombreUsuario
                );

        registrarAuditoriaNoAdquirida(
                usuario,
                receta
        );

        return receta;
    }

    private void validar(
            RecetaFormDTO dto
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Datos de receta requeridos."
            );
        }

        if (dto.getCitaId() == null) {

            throw new IllegalArgumentException(
                    "La cita es obligatoria."
            );
        }

        if (dto.getObservaciones() != null
                && dto.getObservaciones()
                .trim()
                .length() > 1000) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden exceder 1000 caracteres."
            );
        }

        if (dto.getDetalles() == null
                || dto.getDetalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
            );
        }

        for (RecetaDetalleFormDTO detalle
                : dto.getDetalles()) {

            if (detalle == null) {

                throw new IllegalArgumentException(
                        "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
                );
            }

            if (detalle.getMedicamentoId()
                    == null) {

                throw new IllegalArgumentException(
                        "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
                );
            }

            if (detalle.getDosis() == null
                    || detalle.getDosis()
                    .trim()
                    .isEmpty()) {

                throw new IllegalArgumentException(
                        "Todos los campos de la receta son obligatorios excepto indicaciones especiales."
                );
            }

            if (detalle.getFrecuencia() == null || detalle.getFrecuencia().isBlank()
                    || detalle.getDuracion() == null || detalle.getDuracion().isBlank()) {
                throw new IllegalArgumentException(
                        "Todos los campos de la receta son obligatorios excepto indicaciones especiales.");
            }

            if (indicacionesCompletas(detalle).length() > 1000) {
                throw new IllegalArgumentException("Las indicaciones no pueden exceder 1000 caracteres.");
            }

            if (detalle.getDosis()
                    .trim()
                    .length() > 200) {

                throw new IllegalArgumentException(
                        "La dosis no puede exceder 200 caracteres."
                );
            }

            if (detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0) {

                throw new IllegalArgumentException(
                        "La cantidad debe ser mayor que 0."
                );
            }

            if (detalle.getIndicaciones() != null
                    && detalle.getIndicaciones()
                    .trim()
                    .length() > 1000) {

                throw new IllegalArgumentException(
                        "Las indicaciones no pueden exceder 1000 caracteres."
                );
            }
        }
    }

    private String indicacionesCompletas(RecetaDetalleFormDTO detalle) {
        return "Frecuencia: " + detalle.getFrecuencia().trim()
                + ". Duración: " + detalle.getDuracion().trim()
                + (detalle.getIndicaciones() == null || detalle.getIndicaciones().isBlank()
                    ? "" : ". Indicaciones especiales: " + detalle.getIndicaciones().trim());
    }

    private boolean verificarVencimiento(
            Receta receta
    ) {

        if (receta == null) {
            return false;
        }

        if (receta.getEstado()
                != EstadoReceta.PENDIENTE
                && receta.getEstado()
                != EstadoReceta.PARCIAL) {

            return false;
        }

        if (receta.isVencidaParaDespacho()) {

            receta.setEstado(
                    EstadoReceta.VENCIDA
            );

            return true;
        }

        return false;
    }

    private Usuario buscarUsuario(
            String nombreUsuario
    ) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            return null;
        }

        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuario
                )
                .orElse(null);
    }

    private String normalizarOpcional(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        return valor.trim();
    }

    private void registrarAuditoria(
            Usuario usuario,
            Receta receta
    ) {

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                "CREAR_RECETA"
        );

        auditoria.setTipoEntidad(
                "RECETA"
        );

        auditoria.setEntidadId(
                receta
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(
                "Se generó una receta para la consulta "
                        + receta
                        .getConsultaMedica()
                        .getId()
                        + " con "
                        + receta
                        .getDetalles()
                        .size()
                        + " medicamento(s)."
        );

        auditoriaRepository.save(
                auditoria
        );
    }

    private void registrarAuditoriaNoAdquirida(
            Usuario usuario,
            Receta receta
    ) {

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                "MARCAR_RECETA_NO_ADQUIRIDA"
        );

        auditoria.setTipoEntidad(
                "RECETA"
        );

        auditoria.setEntidadId(
                receta
                        .getId()
                        .toString()
        );

        auditoria.setDetalle("Se ha registrado que el paciente "
                + receta.getConsultaMedica().getCita().getPaciente().getNombreCompleto()
                + " no adquirió los medicamentos recetados en farmacia interna. Receta: " + receta.getId() + ".");

        auditoriaRepository.save(
                auditoria
        );
    }
}
