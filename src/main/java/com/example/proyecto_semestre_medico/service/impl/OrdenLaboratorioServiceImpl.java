package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.*;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.OrdenLaboratorioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrdenLaboratorioServiceImpl
        implements OrdenLaboratorioService {

    private final OrdenLaboratorioRepository ordenLaboratorioRepository;
    private final ConsultaMedicaRepository consultaMedicaRepository;
    private final CitaRepository citaRepository;
    private final CatalogoExamenRepository catalogoExamenRepository;
    private final MedicoRepository medicoRepository;

    private final OrdenLaboratorioDetalleRepository
            ordenLaboratorioDetalleRepository;

    private final ResultadoLaboratorioRepository
            resultadoLaboratorioRepository;

    private final UsuarioRepository usuarioRepository;

    public OrdenLaboratorioServiceImpl(
            OrdenLaboratorioRepository ordenLaboratorioRepository,
            ConsultaMedicaRepository consultaMedicaRepository,
            CitaRepository citaRepository,
            CatalogoExamenRepository catalogoExamenRepository,
            MedicoRepository medicoRepository,
            OrdenLaboratorioDetalleRepository ordenLaboratorioDetalleRepository,
            ResultadoLaboratorioRepository resultadoLaboratorioRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.ordenLaboratorioRepository =
                ordenLaboratorioRepository;

        this.consultaMedicaRepository =
                consultaMedicaRepository;

        this.citaRepository =
                citaRepository;

        this.catalogoExamenRepository =
                catalogoExamenRepository;

        this.medicoRepository =
                medicoRepository;

        this.ordenLaboratorioDetalleRepository =
                ordenLaboratorioDetalleRepository;

        this.resultadoLaboratorioRepository =
                resultadoLaboratorioRepository;

        this.usuarioRepository =
                usuarioRepository;
    }

    // =====================================================
    // CATÁLOGO DE EXÁMENES
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<ExamenOpcionDTO> listarExamenesActivos() {

        return catalogoExamenRepository
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(examen ->
                        new ExamenOpcionDTO(
                                examen.getId(),
                                examen.getCodigo(),
                                examen.getNombre(),
                                examen.getDescripcion(),
                                examen.getRangoReferencia(),
                                examen.getPrecio()
                        )
                )
                .toList();
    }

    // =====================================================
    // CREACIÓN DE ORDEN DESDE CU-08
    // =====================================================

    @Override
    @Transactional
    public OrdenLaboratorio crearOrden(
            OrdenLaboratorioFormDTO dto,
            String nombreUsuario
    ) {

        if (dto.getCitaId() == null) {
            throw new IllegalArgumentException(
                    "La cita es obligatoria."
            );
        }

        Medico medico =
                medicoRepository
                        .findByPersonalUsuarioNombreUsuario(
                                nombreUsuario
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El usuario autenticado no está registrado como médico."
                                )
                        );

        Cita cita =
                citaRepository
                        .findById(dto.getCitaId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La cita no existe."
                                )
                        );

        if (!cita.getMedico()
                .getId()
                .equals(medico.getId())) {

            throw new IllegalArgumentException(
                    "La cita no está asignada al médico autenticado."
            );
        }

        if (!"PENDIENTE_CIERRE".equals(
                cita.getEstadoCita().getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "Solo pueden generarse órdenes para citas pendientes de cierre."
            );
        }

        ConsultaMedica consulta =
                consultaMedicaRepository
                        .findByCitaId(dto.getCitaId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La consulta médica no existe."
                                )
                        );

        if (!"FINALIZADA".equals(
                consulta.getEstado()
        )) {

            throw new IllegalArgumentException(
                    "La consulta médica debe estar finalizada antes de generar una orden."
            );
        }

        if (dto.getExamenesIds() == null
                || dto.getExamenesIds().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar al menos un examen de laboratorio."
            );
        }

        List<Long> idsUnicos =
                dto.getExamenesIds()
                        .stream()
                        .distinct()
                        .toList();

        List<CatalogoExamen> examenes =
                new ArrayList<>();

        for (Long examenId : idsUnicos) {

            CatalogoExamen examen =
                    catalogoExamenRepository
                            .findById(examenId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Uno de los exámenes seleccionados no existe."
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    examen.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "El examen "
                                + examen.getNombre()
                                + " se encuentra inactivo."
                );
            }

            examenes.add(examen);
        }

        OrdenLaboratorio orden =
                new OrdenLaboratorio();

        orden.setNumeroOrden(
                generarNumeroOrden()
        );

        orden.setConsultaMedica(
                consulta
        );

        orden.setPaciente(
                cita.getPaciente()
        );

        orden.setMedico(
                medico
        );

        orden.setEstado(
                "PENDIENTE"
        );

        orden.setExterna(
                Boolean.TRUE.equals(
                        dto.getExterna()
                )
        );

        orden.setNotas(
                limpiar(dto.getNotas())
        );

        BigDecimal total =
                BigDecimal.ZERO;

        for (CatalogoExamen examen : examenes) {

            OrdenLaboratorioDetalle detalle =
                    new OrdenLaboratorioDetalle();

            detalle.setOrdenLaboratorio(
                    orden
            );

            detalle.setExamen(
                    examen
            );

            detalle.setPrecio(
                    examen.getPrecio()
            );

            orden.getDetalles().add(
                    detalle
            );

            total =
                    total.add(
                            examen.getPrecio()
                    );
        }

        orden.setMontoTotal(
                total
        );

        return ordenLaboratorioRepository.save(
                orden
        );
    }

    // =====================================================
    // LISTADO Y FILTROS CU-09
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrdenLaboratorioListaDTO> listarOrdenes(
            String estado,
            String paciente,
            String medico
    ) {

        return ordenLaboratorioRepository
                .buscar(
                        limpiar(estado),
                        limpiar(paciente),
                        limpiar(medico)
                )
                .stream()
                .map(this::convertirLista)
                .toList();
    }

    // =====================================================
    // DETALLE DE ORDEN
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public OrdenLaboratorioDetalleDTO obtenerDetalle(
            Long ordenId
    ) {

        OrdenLaboratorio orden =
                obtenerOrden(ordenId);

        OrdenLaboratorioDetalleDTO dto =
                new OrdenLaboratorioDetalleDTO();

        dto.setId(
                orden.getId()
        );

        dto.setNumeroOrden(
                orden.getNumeroOrden()
        );

        dto.setPaciente(
                orden.getPaciente()
                        .getNombreCompleto()
        );

        dto.setMedico(
                orden.getMedico()
                        .getPersonal()
                        .getNombreCompleto()
        );

        dto.setEstado(
                orden.getEstado()
        );

        dto.setMontoTotal(
                orden.getMontoTotal()
        );

        dto.setExterna(
                Boolean.TRUE.equals(
                        orden.getExterna()
                )
        );

        dto.setNotas(
                orden.getNotas()
        );

        dto.setFechaCreacion(
                orden.getFechaCreacion()
        );

        List<OrdenLaboratorioDetalle> detalles =
                ordenLaboratorioDetalleRepository
                        .findByOrdenLaboratorioId(
                                ordenId
                        );

        List<ExamenOrdenDetalleDTO> examenes =
                new ArrayList<>();

        for (OrdenLaboratorioDetalle detalle : detalles) {

            ExamenOrdenDetalleDTO examenDto =
                    new ExamenOrdenDetalleDTO();

            CatalogoExamen examen =
                    detalle.getExamen();

            examenDto.setDetalleId(
                    detalle.getId()
            );

            examenDto.setCodigo(
                    examen.getCodigo()
            );

            examenDto.setNombre(
                    examen.getNombre()
            );

            examenDto.setPrecio(
                    detalle.getPrecio()
            );

            examenDto.setUnidadReferencia(
                    examen.getUnidadReferencia()
            );

            examenDto.setRangoReferencia(
                    examen.getRangoReferencia()
            );

            ResultadoLaboratorio resultado =
                    resultadoLaboratorioRepository
                            .findByOrdenDetalleId(
                                    detalle.getId()
                            )
                            .orElse(null);

            if (resultado != null) {

                examenDto.setValorResultado(
                        resultado.getValorResultado()
                );

                examenDto.setUnidad(
                        resultado.getUnidad()
                );

                examenDto.setFechaResultado(
                        resultado.getFechaResultado()
                );

                examenDto.setFueraRango(
                        resultado.getFueraRango()
                );

                examenDto.setNotasResultado(
                        resultado.getNotas()
                );

                examenDto.setPublicado(
                        resultado.getPublicado()
                );

            } else {

                examenDto.setPublicado(
                        false
                );

                examenDto.setFueraRango(
                        false
                );
            }

            examenes.add(
                    examenDto
            );
        }

        dto.setExamenes(
                examenes
        );

        return dto;
    }

    // =====================================================
    // REGISTRAR / MODIFICAR RESULTADO
    // =====================================================

    @Override
    @Transactional
    public void guardarResultado(
            ResultadoLaboratorioFormDTO dto,
            String nombreUsuario
    ) {

        if (dto == null
                || dto.getDetalleId() == null) {

            throw new IllegalArgumentException(
                    "El examen es obligatorio."
            );
        }

        OrdenLaboratorioDetalle detalle =
                ordenLaboratorioDetalleRepository
                        .findById(
                                dto.getDetalleId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El examen de la orden no existe."
                                )
                        );

        OrdenLaboratorio orden =
                detalle.getOrdenLaboratorio();

        /*
         * Regla CU-09:
         * no se pueden registrar resultados
         * hasta que la orden haya sido pagada.
         *
         * CU-10 será quien cambie:
         * PENDIENTE -> EN_PROCESO
         */
        if (!"EN_PROCESO".equals(
                orden.getEstado()
        )) {

            throw new IllegalArgumentException(
                    "La orden debe estar en estado EN_PROCESO para registrar resultados."
            );
        }

        String valor =
                limpiar(
                        dto.getValorResultado()
                );

        if (valor == null) {

            throw new IllegalArgumentException(
                    "El valor del resultado es obligatorio."
            );
        }

        String unidad =
                limpiar(
                        dto.getUnidad()
                );

        if (unidad == null) {

            throw new IllegalArgumentException(
                    "La unidad del resultado es obligatoria."
            );
        }

        Usuario usuario =
                obtenerUsuario(
                        nombreUsuario
                );

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findByOrdenDetalleId(
                                detalle.getId()
                        )
                        .orElse(null);

        if (resultado != null
                && Boolean.TRUE.equals(
                resultado.getPublicado()
        )) {

            throw new IllegalArgumentException(
                    "El resultado ya fue publicado y no puede modificarse."
            );
        }

        if (resultado == null) {

            resultado =
                    new ResultadoLaboratorio();

            resultado.setOrdenDetalle(
                    detalle
            );

            resultado.setUsuarioRegistro(
                    usuario
            );
        }

        resultado.setValorResultado(
                valor
        );

        resultado.setUnidad(
                unidad
        );

        /*
         * Guardamos una copia del rango actual
         * del catálogo para conservar historial.
         */
        resultado.setRangoReferencia(
                limpiar(
                        detalle.getExamen()
                                .getRangoReferencia()
                )
        );

        resultado.setFechaResultado(
                dto.getFechaResultado() != null
                        ? dto.getFechaResultado()
                        : LocalDateTime.now()
        );

        resultado.setFueraRango(
                Boolean.TRUE.equals(
                        dto.getFueraRango()
                )
        );

        resultado.setNotas(
                limpiar(
                        dto.getNotas()
                )
        );

        if (resultado.getPublicado() == null) {
            resultado.setPublicado(
                    false
            );
        }

        resultadoLaboratorioRepository.save(
                resultado
        );
    }

    // =====================================================
    // PUBLICAR RESULTADO
    // =====================================================

    @Override
    @Transactional
    public void publicarResultado(
            Long detalleId,
            String nombreUsuario
    ) {

        OrdenLaboratorioDetalle detalle =
                ordenLaboratorioDetalleRepository
                        .findById(detalleId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El examen de la orden no existe."
                                )
                        );

        OrdenLaboratorio orden =
                detalle.getOrdenLaboratorio();

        if (!"EN_PROCESO".equals(
                orden.getEstado()
        )) {

            throw new IllegalArgumentException(
                    "Solo pueden publicarse resultados de órdenes EN_PROCESO."
            );
        }

        ResultadoLaboratorio resultado =
                resultadoLaboratorioRepository
                        .findByOrdenDetalleId(
                                detalleId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Debe registrar el resultado antes de publicarlo."
                                )
                        );

        if (Boolean.TRUE.equals(
                resultado.getPublicado()
        )) {

            throw new IllegalArgumentException(
                    "El resultado ya fue publicado."
            );
        }

        Usuario usuario =
                obtenerUsuario(
                        nombreUsuario
                );

        resultado.setPublicado(
                true
        );

        resultado.setFechaPublicacion(
                LocalDateTime.now()
        );

        resultado.setUsuarioPublicacion(
                usuario
        );

        resultadoLaboratorioRepository.save(
                resultado
        );

        /*
         * Después de publicar un examen,
         * verificamos si todos los resultados
         * de la orden ya fueron publicados.
         */
        verificarOrdenCompletada(
                orden
        );
    }

    // =====================================================
    // COMPLETAR ORDEN AUTOMÁTICAMENTE
    // =====================================================

    private void verificarOrdenCompletada(
            OrdenLaboratorio orden
    ) {

        List<OrdenLaboratorioDetalle> detalles =
                ordenLaboratorioDetalleRepository
                        .findByOrdenLaboratorioId(
                                orden.getId()
                        );

        if (detalles.isEmpty()) {
            return;
        }

        for (OrdenLaboratorioDetalle detalle : detalles) {

            ResultadoLaboratorio resultado =
                    resultadoLaboratorioRepository
                            .findByOrdenDetalleId(
                                    detalle.getId()
                            )
                            .orElse(null);

            if (resultado == null
                    || !Boolean.TRUE.equals(
                    resultado.getPublicado()
            )) {

                return;
            }
        }

        orden.setEstado(
                "COMPLETADA"
        );

        ordenLaboratorioRepository.save(
                orden
        );
    }

    // =====================================================
    // CONVERSIÓN PARA LA TABLA
    // =====================================================

    private OrdenLaboratorioListaDTO convertirLista(
            OrdenLaboratorio orden
    ) {

        OrdenLaboratorioListaDTO dto =
                new OrdenLaboratorioListaDTO();

        dto.setId(
                orden.getId()
        );

        dto.setNumeroOrden(
                orden.getNumeroOrden()
        );

        dto.setPaciente(
                orden.getPaciente()
                        .getNombreCompleto()
        );

        dto.setMedico(
                orden.getMedico()
                        .getPersonal()
                        .getNombreCompleto()
        );

        dto.setEstado(
                orden.getEstado()
        );

        dto.setMontoTotal(
                orden.getMontoTotal()
        );

        dto.setExterna(
                Boolean.TRUE.equals(
                        orden.getExterna()
                )
        );

        dto.setFechaCreacion(
                orden.getFechaCreacion()
        );

        return dto;
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private OrdenLaboratorio obtenerOrden(
            Long ordenId
    ) {

        if (ordenId == null) {

            throw new IllegalArgumentException(
                    "La orden es obligatoria."
            );
        }

        return ordenLaboratorioRepository
                .findById(
                        ordenId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La orden de laboratorio no existe."
                        )
                );
    }

    private Usuario obtenerUsuario(
            String nombreUsuario
    ) {

        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuario
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El usuario autenticado no existe."
                        )
                );
    }

    private String generarNumeroOrden() {

        String numero;

        do {

            numero =
                    "LAB-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase();

        } while (
                ordenLaboratorioRepository
                        .findByNumeroOrden(
                                numero
                        )
                        .isPresent()
        );

        return numero;
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