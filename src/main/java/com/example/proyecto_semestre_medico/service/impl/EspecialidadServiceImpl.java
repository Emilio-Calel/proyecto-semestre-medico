package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.EspecialidadRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.EspecialidadService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EspecialidadServiceImpl
        implements EspecialidadService {

    private final EspecialidadRepository
            especialidadRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public EspecialidadServiceImpl(
            EspecialidadRepository especialidadRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository
    ) {
        this.especialidadRepository =
                especialidadRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    /*
     * IMPORTANTE:
     * Este método conserva la caché que ya tenía
     * el sistema antes de CU-14.
     */
    @Override
    @Cacheable("especialidadesActivas")
    @Transactional(readOnly = true)
    public List<Especialidad> listarActivas() {

        return especialidadRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Especialidad buscarPorId(
            Long id
    ) {

        return especialidadRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Especialidad no encontrada."
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Especialidad> listarTodas() {

        return especialidadRepository
                .findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {
                    "especialidadesActivas",
                    "citaEspecialidadesPorSucursal",
                    "citaMedicosPorSucursalEspecialidad"
            },
            allEntries = true
    )
    public Especialidad guardar(
            Especialidad especialidad,
            String nombreUsuario
    ) {

        validar(especialidad);

        String nombre =
                especialidad
                        .getNombre()
                        .trim();

        /*
         * =========================================================
         * CREACIÓN
         * =========================================================
         */
        if (especialidad.getId() == null) {

            Optional<Especialidad> activa =
                    especialidadRepository
                            .findFirstByNombreIgnoreCaseAndActivoTrue(
                                    nombre
                            );

            if (activa.isPresent()) {

                throw new IllegalArgumentException(
                        "Ya existe una especialidad activa con el nombre "
                                + nombre
                                + "."
                );
            }

            /*
             * Si ya existía pero estaba inactiva,
             * reutilizamos el mismo registro.
             */
            Optional<Especialidad> inactiva =
                    especialidadRepository
                            .findFirstByNombreIgnoreCaseAndActivoFalse(
                                    nombre
                            );

            if (inactiva.isPresent()) {

                Especialidad existente =
                        inactiva.get();

                existente.setNombre(
                        nombre
                );

                existente.setDescripcion(
                        especialidad
                                .getDescripcion()
                                .trim()
                );

                existente.setActivo(true);

                existente =
                        especialidadRepository
                                .save(existente);

                registrarAuditoria(
                        nombreUsuario,
                        "REACTIVAR_ESPECIALIDAD",
                        existente,
                        "Se reactivó la especialidad: "
                                + existente.getNombre()
                );

                return existente;
            }

            especialidad.setNombre(
                    nombre
            );

            especialidad.setDescripcion(
                    especialidad
                            .getDescripcion()
                            .trim()
            );

            especialidad.setActivo(true);

            Especialidad creada =
                    especialidadRepository
                            .save(especialidad);

            registrarAuditoria(
                    nombreUsuario,
                    "CREAR_ESPECIALIDAD",
                    creada,
                    "Se creó la especialidad: "
                            + creada.getNombre()
            );

            return creada;
        }

        /*
         * =========================================================
         * EDICIÓN
         * =========================================================
         */

        Especialidad actual =
                especialidadRepository
                        .findById(
                                especialidad.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Especialidad no encontrada."
                                )
                        );

        Optional<Especialidad> otraActiva =
                especialidadRepository
                        .findFirstByNombreIgnoreCaseAndActivoTrue(
                                nombre
                        );

        if (otraActiva.isPresent()
                && !otraActiva
                .get()
                .getId()
                .equals(actual.getId())) {

            throw new IllegalArgumentException(
                    "Ya existe una especialidad activa con el nombre "
                            + nombre
                            + "."
            );
        }

        actual.setNombre(
                nombre
        );

        actual.setDescripcion(
                especialidad
                        .getDescripcion()
                        .trim()
        );

        actual =
                especialidadRepository
                        .save(actual);

        registrarAuditoria(
                nombreUsuario,
                "ACTUALIZAR_ESPECIALIDAD",
                actual,
                "Se actualizó la especialidad: "
                        + actual.getNombre()
        );

        return actual;
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {
                    "especialidadesActivas",
                    "citaEspecialidadesPorSucursal",
                    "citaMedicosPorSucursalEspecialidad"
            },
            allEntries = true
    )
    public void desactivar(
            Long id,
            String nombreUsuario
    ) {

        Especialidad especialidad =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                especialidad.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La especialidad ya está inactiva."
            );
        }

        especialidad.setActivo(false);

        especialidadRepository
                .save(especialidad);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_ESPECIALIDAD",
                especialidad,
                "Se desactivó la especialidad: "
                        + especialidad.getNombre()
        );
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {
                    "especialidadesActivas",
                    "citaEspecialidadesPorSucursal",
                    "citaMedicosPorSucursalEspecialidad"
            },
            allEntries = true
    )
    public void reactivar(
            Long id,
            String nombreUsuario
    ) {

        Especialidad especialidad =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                especialidad.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La especialidad ya está activa."
            );
        }

        Optional<Especialidad> existente =
                especialidadRepository
                        .findFirstByNombreIgnoreCaseAndActivoTrue(
                                especialidad.getNombre()
                        );

        if (existente.isPresent()
                && !existente
                .get()
                .getId()
                .equals(id)) {

            throw new IllegalArgumentException(
                    "No puede reactivar esta especialidad porque ya existe otra activa con el mismo nombre."
            );
        }

        especialidad.setActivo(true);

        especialidadRepository
                .save(especialidad);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_ESPECIALIDAD",
                especialidad,
                "Se reactivó la especialidad: "
                        + especialidad.getNombre()
        );
    }

    private void validar(
            Especialidad especialidad
    ) {

        if (especialidad == null) {

            throw new IllegalArgumentException(
                    "Datos de la especialidad requeridos."
            );
        }

        if (especialidad.getNombre() == null
                || especialidad
                .getNombre()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (especialidad
                .getNombre()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder 200 caracteres."
            );
        }

        /*
         * En Especialidades la descripción sí es
         * obligatoria según CU-14.
         */
        if (especialidad.getDescripcion() == null
                || especialidad
                .getDescripcion()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "La descripción es obligatoria."
            );
        }

        if (especialidad
                .getDescripcion()
                .trim()
                .length() > 500) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder 500 caracteres."
            );
        }
    }

    private void registrarAuditoria(
            String nombreUsuario,
            String accion,
            Especialidad especialidad,
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
                "ESPECIALIDAD"
        );

        auditoria.setEntidadId(
                especialidad
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