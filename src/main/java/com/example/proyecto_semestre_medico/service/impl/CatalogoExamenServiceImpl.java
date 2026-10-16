package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.CatalogoExamen;
import com.example.proyecto_semestre_medico.model.Laboratorio;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.CatalogoExamenRepository;
import com.example.proyecto_semestre_medico.repository.LaboratorioRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.CatalogoExamenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogoExamenServiceImpl
        implements CatalogoExamenService {

    private final CatalogoExamenRepository catalogoExamenRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    public CatalogoExamenServiceImpl(
            CatalogoExamenRepository catalogoExamenRepository,
            LaboratorioRepository laboratorioRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository
    ) {
        this.catalogoExamenRepository = catalogoExamenRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoExamen> listarTodos() {

        return catalogoExamenRepository
                .findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoExamen buscarPorId(
            Long id
    ) {

        return catalogoExamenRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Examen no encontrado."
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Laboratorio>
    listarLaboratoriosActivos() {

        return laboratorioRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional
    public CatalogoExamen guardar(
            CatalogoExamen examen,
            Long laboratorioId,
            String nombreUsuario
    ) {

        validar(examen);

        if (laboratorioId == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un laboratorio."
            );
        }

        Laboratorio laboratorio =
                laboratorioRepository
                        .findById(laboratorioId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Laboratorio no válido."
                                )
                        );

        if (!Boolean.TRUE.equals(
                laboratorio.getActivo()
        )) {
            throw new IllegalArgumentException(
                    "El laboratorio seleccionado está inactivo."
            );
        }

        String codigo =
                examen.getCodigo()
                        .trim()
                        .toUpperCase();

        String nombre =
                examen.getNombre()
                        .trim();

        Optional<CatalogoExamen> porCodigo =
                catalogoExamenRepository
                        .findByCodigoIgnoreCase(codigo);

        Optional<CatalogoExamen> porNombre =
                catalogoExamenRepository
                        .findByNombreIgnoreCase(nombre);

        if (examen.getId() == null) {

            if (porCodigo.isPresent()) {
                throw new IllegalArgumentException(
                        "Ya existe un examen con el código "
                                + codigo
                                + "."
                );
            }

            if (porNombre.isPresent()) {
                throw new IllegalArgumentException(
                        "Ya existe un examen con el nombre "
                                + nombre
                                + "."
                );
            }

            examen.setCodigo(codigo);
            examen.setNombre(nombre);

            examen.setDescripcion(
                    normalizarOpcional(
                            examen.getDescripcion()
                    )
            );

            examen.setUnidadReferencia(
                    normalizarOpcional(
                            examen.getUnidadReferencia()
                    )
            );

            examen.setRangoReferencia(
                    normalizarOpcional(
                            examen.getRangoReferencia()
                    )
            );

            examen.setLaboratorio(laboratorio);
            examen.setActivo(true);

            CatalogoExamen creado =
                    catalogoExamenRepository
                            .save(examen);

            registrarAuditoria(
                    nombreUsuario,
                    "CREAR_EXAMEN_LABORATORIO",
                    creado,
                    "Se creó el examen de laboratorio: "
                            + creado.getNombre()
            );

            return creado;
        }

        CatalogoExamen actual =
                buscarPorId(
                        examen.getId()
                );

        if (porCodigo.isPresent()
                && !porCodigo.get()
                .getId()
                .equals(actual.getId())) {

            throw new IllegalArgumentException(
                    "Ya existe un examen con el código "
                            + codigo
                            + "."
            );
        }

        if (porNombre.isPresent()
                && !porNombre.get()
                .getId()
                .equals(actual.getId())) {

            throw new IllegalArgumentException(
                    "Ya existe un examen con el nombre "
                            + nombre
                            + "."
            );
        }

        actual.setCodigo(codigo);
        actual.setNombre(nombre);

        actual.setDescripcion(
                normalizarOpcional(
                        examen.getDescripcion()
                )
        );

        actual.setUnidadReferencia(
                normalizarOpcional(
                        examen.getUnidadReferencia()
                )
        );

        actual.setRangoReferencia(
                normalizarOpcional(
                        examen.getRangoReferencia()
                )
        );

        actual.setPrecio(
                examen.getPrecio()
        );

        actual.setLaboratorio(
                laboratorio
        );

        actual =
                catalogoExamenRepository
                        .save(actual);

        registrarAuditoria(
                nombreUsuario,
                "ACTUALIZAR_EXAMEN_LABORATORIO",
                actual,
                "Se actualizó el examen: "
                        + actual.getNombre()
        );

        return actual;
    }

    @Override
    @Transactional
    public void desactivar(
            Long id,
            String nombreUsuario
    ) {

        CatalogoExamen examen =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                examen.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El examen ya está inactivo."
            );
        }

        examen.setActivo(false);

        catalogoExamenRepository
                .save(examen);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_EXAMEN_LABORATORIO",
                examen,
                "Se desactivó el examen: "
                        + examen.getNombre()
        );
    }

    @Override
    @Transactional
    public void reactivar(
            Long id,
            String nombreUsuario
    ) {

        CatalogoExamen examen =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                examen.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El examen ya está activo."
            );
        }

        if (!Boolean.TRUE.equals(
                examen.getLaboratorio().getActivo()
        )) {

            throw new IllegalArgumentException(
                    "No puede reactivar el examen porque su laboratorio está inactivo."
            );
        }

        examen.setActivo(true);

        catalogoExamenRepository
                .save(examen);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_EXAMEN_LABORATORIO",
                examen,
                "Se reactivó el examen: "
                        + examen.getNombre()
        );
    }

    private void validar(
            CatalogoExamen examen
    ) {

        if (examen == null) {
            throw new IllegalArgumentException(
                    "Datos del examen requeridos."
            );
        }

        if (examen.getCodigo() == null
                || examen.getCodigo()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El código es obligatorio."
            );
        }

        if (examen.getCodigo()
                .trim()
                .length() > 30) {

            throw new IllegalArgumentException(
                    "El código no puede exceder 30 caracteres."
            );
        }

        if (examen.getNombre() == null
                || examen.getNombre()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (examen.getNombre()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder 200 caracteres."
            );
        }

        if (examen.getDescripcion() != null
                && examen.getDescripcion()
                .trim()
                .length() > 500) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder 500 caracteres."
            );
        }

        BigDecimal precio =
                examen.getPrecio();

        if (precio == null
                || precio.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "El precio base debe ser mayor a 0."
            );
        }

        if (precio.scale() > 2) {

            throw new IllegalArgumentException(
                    "El precio debe tener máximo 2 decimales."
            );
        }

        if (examen.getUnidadReferencia() != null
                && examen.getUnidadReferencia()
                .trim()
                .length() > 50) {

            throw new IllegalArgumentException(
                    "La unidad no puede exceder 50 caracteres."
            );
        }

        if (examen.getRangoReferencia() != null
                && examen.getRangoReferencia()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "El rango de referencia no puede exceder 100 caracteres."
            );
        }
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
            String nombreUsuario,
            String accion,
            CatalogoExamen examen,
            String detalle
    ) {

        Usuario usuario = null;

        if (nombreUsuario != null
                && !nombreUsuario.isBlank()) {

            usuario =
                    usuarioRepository
                            .findByNombreUsuarioIgnoreCase(
                                    nombreUsuario
                            )
                            .orElse(null);
        }

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuario);
        auditoria.setAccion(accion);

        auditoria.setTipoEntidad(
                "CATALOGO_EXAMEN"
        );

        auditoria.setEntidadId(
                examen.getId()
                        .toString()
        );

        auditoria.setDetalle(
                detalle
        );

        auditoriaRepository
                .save(auditoria);
    }
}