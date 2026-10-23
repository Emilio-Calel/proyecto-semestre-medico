package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.SucursalService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SucursalServiceImpl
        implements SucursalService {

    private final ControlCatalogo control;

    private final SucursalRepository
            sucursalRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public SucursalServiceImpl(
            SucursalRepository sucursalRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            ControlCatalogo control
    ) {
        this.control = control;
        this.sucursalRepository =
                sucursalRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Cacheable("sucursalesActivas")
    @Transactional(readOnly = true)
    public List<Sucursal> listarActivas() {

        return sucursalRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Sucursal buscarPorId(
            Long id
    ) {

        return sucursalRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sucursal no encontrada."
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarTodas() {

        return sucursalRepository
                .findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional
    public Sucursal guardar(
            Sucursal sucursal,
            String nombreUsuario
    ) {
        control.iniciar(nombreUsuario, sucursal != null && sucursal.getId() != null ? "CATALOG_UPDATE" : "CATALOG_CREATE");

        validar(sucursal);
        control.nombreUnico(Sucursal.class, sucursal.getId(), sucursal.getNombre());

        String nombre =
                sucursal
                        .getNombre()
                        .trim();

        Optional<Sucursal> existente =
                control.porNombre(Sucursal.class, nombre);

        /*
         * CREACIÓN
         */
        if (sucursal.getId() == null) {

            if (existente.isPresent()) {

                Sucursal encontrada =
                        existente.get();

                if (Boolean.TRUE.equals(
                        encontrada.getActivo()
                )) {

                    throw new IllegalArgumentException(
                            "Ya existe una sucursal activa con el nombre "
                                    + nombre
                                    + "."
                    );
                }

                /*
                 * Si existe inactiva con el mismo nombre,
                 * la reactivamos.
                 */
                encontrada.setNombre(
                        nombre
                );

                encontrada.setTelefono(
                        normalizarOpcional(
                                sucursal.getTelefono()
                        )
                );

                encontrada.setDireccion(
                        normalizarOpcional(
                                sucursal.getDireccion()
                        )
                );

                encontrada.setDescripcion(
                        normalizarOpcional(
                                sucursal.getDescripcion()
                        )
                );

                encontrada.setActivo(true);

                encontrada =
                        sucursalRepository
                                .save(encontrada);

                registrarAuditoria(
                        nombreUsuario,
                        "REACTIVAR_SUCURSAL",
                        encontrada,
                        "Se reactivó la sucursal: "
                                + encontrada.getNombre()
                );

                return encontrada;
            }

            sucursal.setNombre(
                    nombre
            );

            sucursal.setTelefono(
                    normalizarOpcional(
                            sucursal.getTelefono()
                    )
            );

            sucursal.setDireccion(
                    normalizarOpcional(
                            sucursal.getDireccion()
                    )
            );

            sucursal.setDescripcion(
                    normalizarOpcional(
                            sucursal.getDescripcion()
                    )
            );

            sucursal.setActivo(true);

            Sucursal creada =
                    sucursalRepository
                            .save(sucursal);

            registrarAuditoria(
                    nombreUsuario,
                    "CREAR_SUCURSAL",
                    creada,
                    "Se creó la sucursal: "
                            + creada.getNombre()
            );

            return creada;
        }

        /*
         * EDICIÓN
         */
        Sucursal actual =
                sucursalRepository
                        .findById(
                                sucursal.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sucursal no encontrada."
                                )
                        );

        if (existente.isPresent()
                && !existente
                .get()
                .getId()
                .equals(actual.getId())) {

            throw new IllegalArgumentException(
                    "Ya existe una sucursal con el nombre "
                            + nombre
                            + "."
            );
        }

        actual.setNombre(
                nombre
        );

        actual.setTelefono(
                normalizarOpcional(
                        sucursal.getTelefono()
                )
        );

        actual.setDireccion(
                normalizarOpcional(
                        sucursal.getDireccion()
                )
        );

        actual.setDescripcion(
                normalizarOpcional(
                        sucursal.getDescripcion()
                )
        );

        actual =
                sucursalRepository
                        .save(actual);

        registrarAuditoria(
                nombreUsuario,
                "ACTUALIZAR_SUCURSAL",
                actual,
                "Se actualizó la sucursal: "
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

        Sucursal sucursal =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal ya está inactiva."
            );
        }

        sucursal.setActivo(false);

        sucursalRepository
                .save(sucursal);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_SUCURSAL",
                sucursal,
                "Se desactivó la sucursal: "
                        + sucursal.getNombre()
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
        control.nombreUnico(Sucursal.class, id, registro.getNombre());

        Sucursal sucursal =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal ya está activa."
            );
        }

        Optional<Sucursal> existente =
                control.porNombre(Sucursal.class, sucursal.getNombre());

        if (existente.isPresent()
                && !existente
                .get()
                .getId()
                .equals(id)
                && Boolean.TRUE.equals(
                existente.get().getActivo()
        )) {

            throw new IllegalArgumentException(
                    "No puede reactivar esta sucursal porque ya existe otra activa con el mismo nombre."
            );
        }

        sucursal.setActivo(true);

        sucursalRepository
                .save(sucursal);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_SUCURSAL",
                sucursal,
                "Se reactivó la sucursal: "
                        + sucursal.getNombre()
        );
    }

    private void validar(
            Sucursal sucursal
    ) {

        if (sucursal == null) {

            throw new IllegalArgumentException(
                    "Datos de la sucursal requeridos."
            );
        }

        if (sucursal.getNombre() == null
                || sucursal
                .getNombre()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (sucursal
                .getNombre()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder 100 caracteres."
            );
        }

        /*
         * El teléfono es opcional,
         * pero si se ingresa deben ser exactamente 8 dígitos.
         */
        if (sucursal.getTelefono() != null && !sucursal.getTelefono().isEmpty()
                && !sucursal.getTelefono().matches("[0-9]{8}")) {
            throw new IllegalArgumentException("El teléfono debe tener exactamente 8 dígitos.");
        }

        if (sucursal.getDireccion() != null
                && sucursal
                .getDireccion()
                .trim()
                .length() > 500) {

            throw new IllegalArgumentException(
                    "La dirección no puede exceder 500 caracteres."
            );
        }

        if (sucursal.getDescripcion() != null
                && sucursal
                .getDescripcion()
                .trim()
                .length() > 250) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder 250 caracteres."
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
            Sucursal sucursal,
            String detalle
    ) {

        Usuario usuario = control.usuario(nombreUsuario);
        control.invalidarTrasCommit("sucursalesActivas", "citaSucursalesActivas", "citaEspecialidadesPorSucursal", "citaMedicosPorSucursalEspecialidad");

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                accion
        );

        auditoria.setTipoEntidad(
                "SUCURSAL"
        );

        auditoria.setEntidadId(
                sucursal
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