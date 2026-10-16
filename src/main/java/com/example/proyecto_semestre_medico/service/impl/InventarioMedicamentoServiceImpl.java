package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.InventarioMedicamento;
import com.example.proyecto_semestre_medico.model.Medicamento;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.InventarioMedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.MedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.InventarioMedicamentoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventarioMedicamentoServiceImpl
        implements InventarioMedicamentoService {

    private final InventarioMedicamentoRepository
            inventarioRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final SucursalRepository
            sucursalRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public InventarioMedicamentoServiceImpl(
            InventarioMedicamentoRepository inventarioRepository,
            MedicamentoRepository medicamentoRepository,
            SucursalRepository sucursalRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository
    ) {
        this.inventarioRepository =
                inventarioRepository;

        this.medicamentoRepository =
                medicamentoRepository;

        this.sucursalRepository =
                sucursalRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventarioMedicamento>
    listarTodos() {

        return inventarioRepository
                .findAllByOrderByIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public InventarioMedicamento buscarPorId(
            Long id
    ) {

        return inventarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Inventario no encontrado."
                        )
                );
    }

    @Override
    @Transactional
    public InventarioMedicamento guardar(
            Long medicamentoId,
            Long sucursalId,
            Integer stockActual,
            String nombreUsuario
    ) {

        if (medicamentoId == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un medicamento."
            );
        }

        if (sucursalId == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal."
            );
        }

        if (stockActual == null
                || stockActual < 0) {

            throw new IllegalArgumentException(
                    "El stock actual debe ser mayor o igual a 0."
            );
        }

        Medicamento medicamento =
                medicamentoRepository
                        .findById(medicamentoId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Medicamento no válido."
                                )
                        );

        if (!Boolean.TRUE.equals(
                medicamento.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El medicamento está inactivo."
            );
        }

        Sucursal sucursal =
                sucursalRepository
                        .findById(sucursalId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sucursal no válida."
                                )
                        );

        if (!Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal está inactiva."
            );
        }

        Optional<InventarioMedicamento>
                existente =
                inventarioRepository
                        .findByMedicamentoIdAndSucursalId(
                                medicamentoId,
                                sucursalId
                        );

        if (existente.isPresent()) {

            InventarioMedicamento inventario =
                    existente.get();

            if (Boolean.TRUE.equals(
                    inventario.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "Ya existe inventario para este medicamento en esta sucursal."
                );
            }

            inventario.setActivo(true);
            inventario.setStockActual(
                    stockActual
            );

            inventario =
                    inventarioRepository
                            .save(inventario);

            registrarAuditoria(
                    nombreUsuario,
                    "REACTIVAR_INVENTARIO_MEDICAMENTO",
                    inventario,
                    "Se reactivó el inventario de "
                            + medicamento.getNombre()
                            + " en "
                            + sucursal.getNombre()
                            + ". Stock inicial: "
                            + stockActual
            );

            return inventario;
        }

        InventarioMedicamento inventario =
                new InventarioMedicamento();

        inventario.setMedicamento(
                medicamento
        );

        inventario.setSucursal(
                sucursal
        );

        inventario.setStockActual(
                stockActual
        );

        inventario.setActivo(true);

        inventario =
                inventarioRepository
                        .save(inventario);

        registrarAuditoria(
                nombreUsuario,
                "CREAR_INVENTARIO_MEDICAMENTO",
                inventario,
                "Se creó inventario de "
                        + medicamento.getNombre()
                        + " en "
                        + sucursal.getNombre()
                        + ". Stock inicial: "
                        + stockActual
        );

        return inventario;
    }

    @Override
    @Transactional
    public void desactivar(
            Long id,
            String nombreUsuario
    ) {

        InventarioMedicamento inventario =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                inventario.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El inventario ya está inactivo."
            );
        }

        inventario.setActivo(false);

        inventarioRepository
                .save(inventario);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_INVENTARIO_MEDICAMENTO",
                inventario,
                "Se desactivó el inventario de "
                        + inventario
                        .getMedicamento()
                        .getNombre()
                        + " en "
                        + inventario
                        .getSucursal()
                        .getNombre()
        );
    }

    @Override
    @Transactional
    public void reactivar(
            Long id,
            String nombreUsuario
    ) {

        InventarioMedicamento inventario =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                inventario.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El inventario ya está activo."
            );
        }

        inventario.setActivo(true);

        inventarioRepository
                .save(inventario);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_INVENTARIO_MEDICAMENTO",
                inventario,
                "Se reactivó el inventario de "
                        + inventario
                        .getMedicamento()
                        .getNombre()
                        + " en "
                        + inventario
                        .getSucursal()
                        .getNombre()
        );
    }

    private void registrarAuditoria(
            String nombreUsuario,
            String accion,
            InventarioMedicamento inventario,
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

        auditoria.setAccion(
                accion
        );

        auditoria.setTipoEntidad(
                "INVENTARIO_MEDICAMENTO"
        );

        auditoria.setEntidadId(
                inventario
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