package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.model.SucursalEspecialidad;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.EspecialidadRepository;
import com.example.proyecto_semestre_medico.repository.SucursalEspecialidadRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.SucursalEspecialidadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SucursalEspecialidadServiceImpl
        implements SucursalEspecialidadService {

    private final SucursalEspecialidadRepository
            sucursalEspecialidadRepository;

    private final SucursalRepository
            sucursalRepository;

    private final EspecialidadRepository
            especialidadRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public SucursalEspecialidadServiceImpl(
            SucursalEspecialidadRepository
                    sucursalEspecialidadRepository,
            SucursalRepository
                    sucursalRepository,
            EspecialidadRepository
                    especialidadRepository,
            UsuarioRepository
                    usuarioRepository,
            AuditoriaRepository
                    auditoriaRepository
    ) {
        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;

        this.sucursalRepository =
                sucursalRepository;

        this.especialidadRepository =
                especialidadRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalEspecialidad> listar(
            Long id
    ) {

        return sucursalEspecialidadRepository
                .buscar(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sucursal>
    listarSucursalesActivas() {

        return sucursalRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Especialidad>
    listarEspecialidadesActivas() {

        return especialidadRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional
    public SucursalEspecialidad asignar(
            Long sucursalId,
            Long especialidadId,
            String nombreUsuario
    ) {

        if (sucursalId == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sede."
            );
        }

        if (especialidadId == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una especialidad."
            );
        }

        Sucursal sucursal =
                sucursalRepository
                        .findById(sucursalId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La sede seleccionada no existe."
                                )
                        );

        if (!Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sede seleccionada está inactiva."
            );
        }

        Especialidad especialidad =
                especialidadRepository
                        .findById(especialidadId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La especialidad seleccionada no existe."
                                )
                        );

        if (!Boolean.TRUE.equals(
                especialidad.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La especialidad seleccionada está inactiva."
            );
        }

        Optional<SucursalEspecialidad>
                existente =
                sucursalEspecialidadRepository
                        .findBySucursalIdAndEspecialidadId(
                                sucursalId,
                                especialidadId
                        );

        if (existente.isPresent()) {

            SucursalEspecialidad relacion =
                    existente.get();

            if (Boolean.TRUE.equals(
                    relacion.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "Esta combinación de sede y especialidad ya existe en el sistema."
                );
            }

            relacion.setActivo(true);

            relacion =
                    sucursalEspecialidadRepository
                            .save(relacion);

            registrarAuditoria(
                    nombreUsuario,
                    "REACTIVAR_SUCURSAL_ESPECIALIDAD",
                    relacion,
                    "Se reactivó la asignación "
                            + sucursal.getNombre()
                            + " - "
                            + especialidad.getNombre()
            );

            return relacion;
        }

        SucursalEspecialidad nueva =
                new SucursalEspecialidad();

        nueva.setSucursal(sucursal);
        nueva.setEspecialidad(especialidad);
        nueva.setActivo(true);

        nueva =
                sucursalEspecialidadRepository
                        .save(nueva);

        registrarAuditoria(
                nombreUsuario,
                "CREAR_SUCURSAL_ESPECIALIDAD",
                nueva,
                "Se asignó la especialidad "
                        + especialidad.getNombre()
                        + " a la sede "
                        + sucursal.getNombre()
        );

        return nueva;
    }

    @Override
    @Transactional
    public void desactivar(
            Long id,
            String nombreUsuario
    ) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Asignación requerida."
            );
        }

        SucursalEspecialidad relacion =
                sucursalEspecialidadRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La asignación no existe."
                                )
                        );

        if (!Boolean.TRUE.equals(
                relacion.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La asignación ya se encuentra inactiva."
            );
        }

        relacion.setActivo(false);

        sucursalEspecialidadRepository
                .save(relacion);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_SUCURSAL_ESPECIALIDAD",
                relacion,
                "Se desactivó la asignación "
                        + relacion.getSucursal()
                        .getNombre()
                        + " - "
                        + relacion.getEspecialidad()
                        .getNombre()
        );
    }

    private void registrarAuditoria(
            String nombreUsuario,
            String accion,
            SucursalEspecialidad relacion,
            String detalle
    ) {

        Usuario usuario =
                null;

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

        auditoria.setAccion(
                accion
        );

        auditoria.setTipoEntidad(
                "SUCURSAL_ESPECIALIDAD"
        );

        auditoria.setEntidadId(
                relacion.getId()
                        .toString()
        );

        auditoria.setDetalle(
                detalle
        );

        auditoriaRepository
                .save(auditoria);
    }
}