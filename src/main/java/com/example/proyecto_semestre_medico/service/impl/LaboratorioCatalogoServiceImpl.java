package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Laboratorio;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.LaboratorioRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.LaboratorioCatalogoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LaboratorioCatalogoServiceImpl
        implements LaboratorioCatalogoService {

    private final ControlCatalogo control;

    private final LaboratorioRepository
            laboratorioRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public LaboratorioCatalogoServiceImpl(
            LaboratorioRepository laboratorioRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            ControlCatalogo control
    ) {
        this.control = control;
        this.laboratorioRepository =
                laboratorioRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Laboratorio> listarTodos() {

        return laboratorioRepository
                .findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Laboratorio> listarActivos() {

        return laboratorioRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Laboratorio buscarPorId(
            Long id
    ) {

        return laboratorioRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Laboratorio no encontrado."
                        )
                );
    }

    @Override
    @Transactional
    public Laboratorio guardar(
            Laboratorio laboratorio,
            String nombreUsuario
    ) {
        control.iniciar(nombreUsuario, laboratorio != null && laboratorio.getId() != null ? "CATALOG_UPDATE" : "CATALOG_CREATE");

        validar(laboratorio);
        control.nombreUnico(Laboratorio.class, laboratorio.getId(), laboratorio.getNombre());

        String nombre =
                laboratorio
                        .getNombre()
                        .trim();

        Optional<Laboratorio> existente =
                control.porNombre(Laboratorio.class, nombre);

        if (laboratorio.getId() == null) {

            if (existente.isPresent()) {

                Laboratorio encontrado =
                        existente.get();

                if (Boolean.TRUE.equals(
                        encontrado.getActivo()
                )) {

                    throw new IllegalArgumentException(
                            "Ya existe un laboratorio activo con el nombre "
                                    + nombre
                                    + "."
                    );
                }

                encontrado.setNombre(
                        nombre
                );

                encontrado.setDescripcion(
                        normalizarOpcional(
                                laboratorio
                                        .getDescripcion()
                        )
                );

                encontrado.setActivo(true);

                encontrado =
                        laboratorioRepository
                                .save(encontrado);

                registrarAuditoria(
                        nombreUsuario,
                        "REACTIVAR_LABORATORIO",
                        encontrado,
                        "Se reactivó el laboratorio: "
                                + encontrado.getNombre()
                );

                return encontrado;
            }

            laboratorio.setNombre(
                    nombre
            );

            laboratorio.setDescripcion(
                    normalizarOpcional(
                            laboratorio
                                    .getDescripcion()
                    )
            );

            laboratorio.setActivo(true);

            Laboratorio creado =
                    laboratorioRepository
                            .save(laboratorio);

            registrarAuditoria(
                    nombreUsuario,
                    "CREAR_LABORATORIO",
                    creado,
                    "Se creó el laboratorio: "
                            + creado.getNombre()
            );

            return creado;
        }

        Laboratorio actual =
                laboratorioRepository
                        .findById(
                                laboratorio.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Laboratorio no encontrado."
                                )
                        );

        if (existente.isPresent()
                && !existente
                .get()
                .getId()
                .equals(
                        actual.getId()
                )) {

            throw new IllegalArgumentException(
                    "Ya existe un laboratorio con el nombre "
                            + nombre
                            + "."
            );
        }

        actual.setNombre(
                nombre
        );

        actual.setDescripcion(
                normalizarOpcional(
                        laboratorio
                                .getDescripcion()
                )
        );

        actual =
                laboratorioRepository
                        .save(actual);

        registrarAuditoria(
                nombreUsuario,
                "ACTUALIZAR_LABORATORIO",
                actual,
                "Se actualizó el laboratorio: "
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
        control.iniciar(nombreUsuario, "CATALOG_DISABLE");

        Laboratorio laboratorio =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                laboratorio.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El laboratorio ya está inactivo."
            );
        }

        laboratorio.setActivo(false);

        laboratorioRepository
                .save(laboratorio);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_LABORATORIO",
                laboratorio,
                "Se desactivó el laboratorio: "
                        + laboratorio.getNombre()
        );
    }

    @Override
    @Transactional
    public void reactivar(
            Long id,
            String nombreUsuario
    ) {
        control.iniciar(nombreUsuario, "CATALOG_UPDATE");
        var registro = buscarPorId(id);
        control.nombreUnico(Laboratorio.class, id, registro.getNombre());

        Laboratorio laboratorio =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                laboratorio.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El laboratorio ya está activo."
            );
        }

        laboratorio.setActivo(true);

        laboratorioRepository
                .save(laboratorio);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_LABORATORIO",
                laboratorio,
                "Se reactivó el laboratorio: "
                        + laboratorio.getNombre()
        );
    }

    private void validar(
            Laboratorio laboratorio
    ) {

        if (laboratorio == null) {

            throw new IllegalArgumentException(
                    "Datos del laboratorio requeridos."
            );
        }

        if (laboratorio.getNombre() == null
                || laboratorio
                .getNombre()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (laboratorio
                .getNombre()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder 200 caracteres."
            );
        }

        if (laboratorio.getDescripcion() != null
                && laboratorio
                .getDescripcion()
                .trim()
                .length() > 500) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder 500 caracteres."
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
            Laboratorio laboratorio,
            String detalle
    ) {

        Usuario usuario = control.usuario(nombreUsuario);
        control.invalidarTrasCommit();

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuario);

        auditoria.setAccion(
                accion
        );

        auditoria.setTipoEntidad(
                "LABORATORIO_CATALOGO"
        );

        auditoria.setEntidadId(
                laboratorio
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(
                detalle
        );

        auditoriaRepository
                .save(auditoria);
    }
}