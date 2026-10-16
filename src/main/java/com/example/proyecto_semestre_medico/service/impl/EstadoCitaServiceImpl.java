package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.EstadoCitaService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EstadoCitaServiceImpl
        implements EstadoCitaService {

    private final EstadoCitaRepository
            estadoCitaRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public EstadoCitaServiceImpl(
            EstadoCitaRepository estadoCitaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository
    ) {
        this.estadoCitaRepository =
                estadoCitaRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Cacheable("estadosCitaActivos")
    @Transactional(readOnly = true)
    public List<EstadoCita> listarActivos() {

        return estadoCitaRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Cacheable(
            value = "estadoCitaPorCodigo",
            key = "#codigo"
    )
    @Transactional(readOnly = true)
    public EstadoCita buscarPorCodigo(
            String codigo
    ) {

        return estadoCitaRepository
                .findByCodigo(codigo)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el estado de cita: "
                                        + codigo
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoCita> listarTodos() {

        return estadoCitaRepository
                .findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoCita buscarPorId(
            Long id
    ) {

        return estadoCitaRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Estado de cita no encontrado."
                        )
                );
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {
                    "estadosCitaActivos",
                    "estadoCitaPorCodigo"
            },
            allEntries = true
    )
    public EstadoCita guardar(
            EstadoCita estadoCita,
            String nombreUsuario
    ) {

        validar(estadoCita);

        String codigo =
                estadoCita
                        .getCodigo()
                        .trim()
                        .toUpperCase();

        String nombre =
                estadoCita
                        .getNombre()
                        .trim();

        Optional<EstadoCita> porCodigo =
                estadoCitaRepository
                        .findByCodigoIgnoreCase(
                                codigo
                        );

        Optional<EstadoCita> porNombre =
                estadoCitaRepository
                        .findByNombreIgnoreCase(
                                nombre
                        );

        /*
         * =====================================================
         * CREACIÓN
         * =====================================================
         */
        if (estadoCita.getId() == null) {

            if (porCodigo.isPresent()) {

                throw new IllegalArgumentException(
                        "Ya existe un estado de cita con el código "
                                + codigo
                                + "."
                );
            }

            if (porNombre.isPresent()
                    && Boolean.TRUE.equals(
                    porNombre.get().getActivo()
            )) {

                throw new IllegalArgumentException(
                        "Ya existe un estado activo con el nombre "
                                + nombre
                                + "."
                );
            }

            estadoCita.setCodigo(
                    codigo
            );

            estadoCita.setNombre(
                    nombre
            );

            estadoCita.setDescripcion(
                    normalizarOpcional(
                            estadoCita.getDescripcion()
                    )
            );

            estadoCita.setActivo(true);

            EstadoCita creado =
                    estadoCitaRepository
                            .save(estadoCita);

            registrarAuditoria(
                    nombreUsuario,
                    "CREAR_ESTADO_CITA",
                    creado,
                    "Se creó el estado de cita "
                            + creado.getCodigo()
                            + " - "
                            + creado.getNombre()
            );

            return creado;
        }

        /*
         * =====================================================
         * EDICIÓN
         * =====================================================
         */
        EstadoCita actual =
                estadoCitaRepository
                        .findById(
                                estadoCita.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Estado de cita no encontrado."
                                )
                        );

        /*
         * IMPORTANTE:
         * NO actualizamos el código.
         *
         * El código almacenado en BD se conserva
         * porque forma parte de los flujos internos.
         */

        if (porNombre.isPresent()
                && !porNombre
                .get()
                .getId()
                .equals(actual.getId())
                && Boolean.TRUE.equals(
                porNombre.get().getActivo()
        )) {

            throw new IllegalArgumentException(
                    "Ya existe otro estado activo con el nombre "
                            + nombre
                            + "."
            );
        }

        actual.setNombre(
                nombre
        );

        actual.setDescripcion(
                normalizarOpcional(
                        estadoCita.getDescripcion()
                )
        );

        actual =
                estadoCitaRepository
                        .save(actual);

        registrarAuditoria(
                nombreUsuario,
                "ACTUALIZAR_ESTADO_CITA",
                actual,
                "Se actualizó el estado de cita "
                        + actual.getCodigo()
                        + " - "
                        + actual.getNombre()
        );

        return actual;
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {
                    "estadosCitaActivos",
                    "estadoCitaPorCodigo"
            },
            allEntries = true
    )
    public void desactivar(
            Long id,
            String nombreUsuario
    ) {

        EstadoCita estado =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                estado.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El estado de cita ya está inactivo."
            );
        }

        estado.setActivo(false);

        estadoCitaRepository
                .save(estado);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_ESTADO_CITA",
                estado,
                "Se desactivó el estado de cita "
                        + estado.getCodigo()
                        + " - "
                        + estado.getNombre()
        );
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {
                    "estadosCitaActivos",
                    "estadoCitaPorCodigo"
            },
            allEntries = true
    )
    public void reactivar(
            Long id,
            String nombreUsuario
    ) {

        EstadoCita estado =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                estado.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El estado de cita ya está activo."
            );
        }

        Optional<EstadoCita> mismoNombre =
                estadoCitaRepository
                        .findByNombreIgnoreCase(
                                estado.getNombre()
                        );

        if (mismoNombre.isPresent()
                && !mismoNombre
                .get()
                .getId()
                .equals(id)
                && Boolean.TRUE.equals(
                mismoNombre.get().getActivo()
        )) {

            throw new IllegalArgumentException(
                    "No puede reactivar este estado porque existe otro activo con el mismo nombre."
            );
        }

        estado.setActivo(true);

        estadoCitaRepository
                .save(estado);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_ESTADO_CITA",
                estado,
                "Se reactivó el estado de cita "
                        + estado.getCodigo()
                        + " - "
                        + estado.getNombre()
        );
    }

    private void validar(
            EstadoCita estadoCita
    ) {

        if (estadoCita == null) {

            throw new IllegalArgumentException(
                    "Datos del estado de cita requeridos."
            );
        }

        /*
         * El código es obligatorio al crear.
         *
         * También llegará desde el formulario de edición,
         * aunque no permitiremos modificarlo.
         */
        if (estadoCita.getCodigo() == null
                || estadoCita
                .getCodigo()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El código es obligatorio."
            );
        }

        if (estadoCita
                .getCodigo()
                .trim()
                .length() > 50) {

            throw new IllegalArgumentException(
                    "El código no puede exceder 50 caracteres."
            );
        }

        /*
         * Usamos un formato seguro para los códigos:
         *
         * EN_ESPERA
         * CONSULTA_MEDICA
         * PACIENTE_PRESENTE
         */
        if (!estadoCita
                .getCodigo()
                .trim()
                .matches("[A-Za-z0-9_]+")) {

            throw new IllegalArgumentException(
                    "El código solo puede contener letras, números y guion bajo."
            );
        }

        if (estadoCita.getNombre() == null
                || estadoCita
                .getNombre()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (estadoCita
                .getNombre()
                .trim()
                .length() > 50) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder 50 caracteres."
            );
        }

        if (estadoCita.getDescripcion() != null
                && estadoCita
                .getDescripcion()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder 200 caracteres."
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
            EstadoCita estadoCita,
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

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                accion
        );

        auditoria.setTipoEntidad(
                "ESTADO_CITA"
        );

        auditoria.setEntidadId(
                estadoCita
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