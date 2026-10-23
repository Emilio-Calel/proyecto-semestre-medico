package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.RolRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.RolService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RolServiceImpl
        implements RolService {

    private final ControlCatalogo control;

    private final RolRepository
            rolRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public RolServiceImpl(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            ControlCatalogo control
    ) {
        this.control = control;
        this.rolRepository =
                rolRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Cacheable("rolesActivos")
    @Transactional(readOnly = true)
    public List<Rol> listarActivos() {

        return rolRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Cacheable("rolesInternosActivos")
    @Transactional(readOnly = true)
    public List<Rol> listarRolesInternosActivos() {

        return rolRepository
                .findByActivoTrueAndCodigoNotOrderByNombreAsc(
                        "PACIENTE"
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Rol buscarPorId(
            Long id
    ) {

        return rolRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rol no encontrado."
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rol> listarTodos() {

        return rolRepository
                .findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional
    public Rol guardar(
            Rol rol,
            String nombreUsuario
    ) {
        control.iniciar(nombreUsuario, rol != null && rol.getId() != null ? "CATALOG_UPDATE" : "CATALOG_CREATE");

        validar(rol);
        control.nombreUnico(Rol.class, rol.getId(), rol.getNombre());

        String codigo =
                rol.getCodigo()
                        .trim()
                        .toUpperCase();

        String nombre =
                rol.getNombre()
                        .trim();

        Optional<Rol> porCodigo =
                rolRepository
                        .findByCodigoIgnoreCase(
                                codigo
                        );

        Optional<Rol> porNombre =
                control.porNombre(Rol.class, nombre);

        /*
         * ============================================
         * CREACIÓN
         * ============================================
         */
        if (rol.getId() == null) {
            var inactivo = control.inactivo(Rol.class, nombre);
            if (inactivo.isPresent()) {
                var anterior = inactivo.get();
                if (!anterior.getCodigo().equalsIgnoreCase(codigo)) {
                    throw new IllegalArgumentException("Existe un registro inactivo con este nombre. Conserve su código para reactivarlo.");
                }
                anterior.setNombre(nombre);
                anterior.setDescripcion(normalizarOpcional(rol.getDescripcion()));
                anterior.setActivo(true);
                rolRepository.save(anterior);
                registrarAuditoria(nombreUsuario, "REACTIVAR_ROL", anterior, "Se reactivó el registro: " + nombre);
                return anterior;
            }


            if (porCodigo.isPresent()) {

                throw new IllegalArgumentException(
                        "Ya existe un rol con el código "
                                + codigo
                                + "."
                );
            }

            if (porNombre.isPresent()
                    && Boolean.TRUE.equals(
                    porNombre.get().getActivo()
            )) {

                throw new IllegalArgumentException(
                        "Ya existe un rol activo con el nombre "
                                + nombre
                                + "."
                );
            }

            rol.setCodigo(
                    codigo
            );

            rol.setNombre(
                    nombre
            );

            rol.setDescripcion(
                    normalizarOpcional(
                            rol.getDescripcion()
                    )
            );

            rol.setActivo(true);
            rol.setPermisos(new java.util.HashSet<>());

            Rol creado =
                    rolRepository.save(rol);

            registrarAuditoria(
                    nombreUsuario,
                    "CREAR_ROL",
                    creado,
                    "Se creó el rol "
                            + creado.getCodigo()
                            + " - "
                            + creado.getNombre()
            );

            return creado;
        }

        /*
         * ============================================
         * EDICIÓN
         * ============================================
         */
        Rol actual =
                rolRepository
                        .findById(
                                rol.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Rol no encontrado."
                                )
                        );

        /*
         * IMPORTANTE:
         *
         * El código NO se cambia.
         * Los permisos tampoco se modifican aquí.
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
                    "Ya existe otro rol activo con el nombre "
                            + nombre
                            + "."
            );
        }

        actual.setNombre(
                nombre
        );

        actual.setDescripcion(
                normalizarOpcional(
                        rol.getDescripcion()
                )
        );

        actual =
                rolRepository.save(actual);

        registrarAuditoria(
                nombreUsuario,
                "ACTUALIZAR_ROL",
                actual,
                "Se actualizó el rol "
                        + actual.getCodigo()
                        + " - "
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

        Rol rol =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                rol.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El rol ya está inactivo."
            );
        }

        /*
         * Protegemos el rol Administrador.
         *
         * Desactivarlo podría dejar el sistema sin
         * administración disponible.
         */
        if ("ADMINISTRADOR".equalsIgnoreCase(
                rol.getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "El rol ADMINISTRADOR no puede desactivarse."
            );
        }

        /*
         * PACIENTE también es estructural para el
         * registro de usuarios externos.
         */
        if ("PACIENTE".equalsIgnoreCase(
                rol.getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "El rol PACIENTE no puede desactivarse."
            );
        }

        if (java.util.Set.of("MEDICO", "ENFERMERO", "RECEPCIONISTA", "CAJERO", "LABORATORISTA", "FARMACEUTICO").contains(rol.getCodigo())
                || usuarioRepository.countByRolCodigoAndActivoTrue(rol.getCodigo()) > 0) {
            throw new IllegalArgumentException("No se puede desactivar un rol estructural o asignado a usuarios activos.");
        }
        rol.setActivo(false);

        rolRepository.save(rol);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_ROL",
                rol,
                "Se desactivó el rol "
                        + rol.getCodigo()
                        + " - "
                        + rol.getNombre()
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
        control.nombreUnico(Rol.class, id, registro.getNombre());

        Rol rol =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                rol.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El rol ya está activo."
            );
        }

        Optional<Rol> mismoNombre =
                control.porNombre(Rol.class, rol.getNombre());

        if (mismoNombre.isPresent()
                && !mismoNombre
                .get()
                .getId()
                .equals(id)
                && Boolean.TRUE.equals(
                mismoNombre.get().getActivo()
        )) {

            throw new IllegalArgumentException(
                    "No puede reactivar este rol porque existe otro activo con el mismo nombre."
            );
        }

        rol.setActivo(true);

        rolRepository.save(rol);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_ROL",
                rol,
                "Se reactivó el rol "
                        + rol.getCodigo()
                        + " - "
                        + rol.getNombre()
        );
    }

    private void validar(
            Rol rol
    ) {

        if (rol == null) {

            throw new IllegalArgumentException(
                    "Datos del rol requeridos."
            );
        }

        if (rol.getCodigo() == null
                || rol.getCodigo()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El código es obligatorio."
            );
        }

        if (rol.getCodigo()
                .trim()
                .length() > 50) {

            throw new IllegalArgumentException(
                    "El código no puede exceder 50 caracteres."
            );
        }

        if (!rol.getCodigo()
                .trim()
                .matches("[A-Za-z0-9_]+")) {

            throw new IllegalArgumentException(
                    "El código solo puede contener letras, números y guion bajo."
            );
        }

        if (rol.getNombre() == null
                || rol.getNombre()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (rol.getNombre()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder 200 caracteres."
            );
        }

        if (rol.getDescripcion() != null
                && rol.getDescripcion()
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
            Rol rol,
            String detalle
    ) {

        Usuario usuario = control.usuario(nombreUsuario);
        control.invalidarTrasCommit("rolesActivos", "rolesInternosActivos");

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                accion
        );

        auditoria.setTipoEntidad(
                "ROL"
        );

        auditoria.setEntidadId(
                rol.getId().toString()
        );

        auditoria.setDetalle(
                detalle
        );

        auditoriaRepository.save(
                auditoria
        );
    }
}