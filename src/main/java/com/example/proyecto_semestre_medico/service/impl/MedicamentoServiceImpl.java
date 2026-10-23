package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Medicamento;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.MedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.MedicamentoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class MedicamentoServiceImpl
        implements MedicamentoService {

    private final ControlCatalogo control;

    private final MedicamentoRepository
            medicamentoRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public MedicamentoServiceImpl(
            MedicamentoRepository medicamentoRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            ControlCatalogo control
    ) {
        this.control = control;
        this.medicamentoRepository =
                medicamentoRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicamento> listarTodos() {

        return medicamentoRepository
                .findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicamento> listarActivos() {

        return medicamentoRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Medicamento buscarPorId(
            Long id
    ) {

        return medicamentoRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medicamento no encontrado."
                        )
                );
    }

    @Override
    @Transactional
    public Medicamento guardar(
            Medicamento medicamento,
            String nombreUsuario
    ) {
        control.iniciar(nombreUsuario, medicamento != null && medicamento.getId() != null ? "CATALOG_UPDATE" : "CATALOG_CREATE");

        validar(medicamento);
        control.nombreUnico(Medicamento.class, medicamento.getId(), medicamento.getNombre());

        String nombreNormalizado =
                medicamento
                        .getNombre()
                        .trim();

        Optional<Medicamento> existente =
                control.porNombre(Medicamento.class, nombreNormalizado);

        if (medicamento.getId() == null) {

            if (existente.isPresent()) {

                Medicamento encontrado =
                        existente.get();

                if (Boolean.TRUE.equals(
                        encontrado.getActivo()
                )) {

                    throw new IllegalArgumentException(
                            "Ya existe un medicamento activo con el nombre "
                                    + nombreNormalizado
                                    + "."
                    );
                }

                encontrado.setNombre(
                        nombreNormalizado
                );

                encontrado.setDescripcion(
                        medicamento
                                .getDescripcion()
                                .trim()
                );

                encontrado.setPrecio(
                        medicamento.getPrecio()
                );

                encontrado.setUnidad(
                        medicamento
                                .getUnidad()
                                .trim()
                );

                encontrado.setEsControlado(
                        Boolean.TRUE.equals(
                                medicamento
                                        .getEsControlado()
                        )
                );

                encontrado.setStockMinimo(
                        medicamento
                                .getStockMinimo()
                );

                encontrado.setActivo(true);

                encontrado =
                        medicamentoRepository
                                .save(encontrado);

                registrarAuditoria(
                        nombreUsuario,
                        "REACTIVAR_MEDICAMENTO",
                        encontrado,
                        "Se reactivó el medicamento: "
                                + encontrado
                                .getNombre()
                );

                return encontrado;
            }

            medicamento.setNombre(
                    nombreNormalizado
            );

            medicamento.setDescripcion(
                    medicamento
                            .getDescripcion()
                            .trim()
            );

            medicamento.setUnidad(
                    medicamento
                            .getUnidad()
                            .trim()
            );

            medicamento.setEsControlado(
                    Boolean.TRUE.equals(
                            medicamento
                                    .getEsControlado()
                    )
            );

            medicamento.setActivo(true);

            Medicamento creado =
                    medicamentoRepository
                            .save(medicamento);

            registrarAuditoria(
                    nombreUsuario,
                    "CREAR_MEDICAMENTO",
                    creado,
                    "Se creó el medicamento: "
                            + creado.getNombre()
            );

            return creado;
        }

        Medicamento actual =
                medicamentoRepository
                        .findById(
                                medicamento.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Medicamento no encontrado."
                                )
                        );

        if (existente.isPresent()
                && !existente
                .get()
                .getId()
                .equals(actual.getId())) {

            throw new IllegalArgumentException(
                    "Ya existe un medicamento con el nombre "
                            + nombreNormalizado
                            + "."
            );
        }

        actual.setNombre(
                nombreNormalizado
        );

        actual.setDescripcion(
                medicamento
                        .getDescripcion()
                        .trim()
        );

        actual.setPrecio(
                medicamento.getPrecio()
        );

        actual.setUnidad(
                medicamento
                        .getUnidad()
                        .trim()
        );

        actual.setEsControlado(
                Boolean.TRUE.equals(
                        medicamento
                                .getEsControlado()
                )
        );

        actual.setStockMinimo(
                medicamento
                        .getStockMinimo()
        );

        actual =
                medicamentoRepository
                        .save(actual);

        registrarAuditoria(
                nombreUsuario,
                "ACTUALIZAR_MEDICAMENTO",
                actual,
                "Se actualizó el medicamento: "
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

        Medicamento medicamento =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                medicamento.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El medicamento ya está inactivo."
            );
        }

        medicamento.setActivo(false);

        medicamentoRepository
                .save(medicamento);

        registrarAuditoria(
                nombreUsuario,
                "DESACTIVAR_MEDICAMENTO",
                medicamento,
                "Se desactivó el medicamento: "
                        + medicamento.getNombre()
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
        control.nombreUnico(Medicamento.class, id, registro.getNombre());

        Medicamento medicamento =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                medicamento.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El medicamento ya está activo."
            );
        }

        medicamento.setActivo(true);

        medicamentoRepository
                .save(medicamento);

        registrarAuditoria(
                nombreUsuario,
                "REACTIVAR_MEDICAMENTO",
                medicamento,
                "Se reactivó el medicamento: "
                        + medicamento.getNombre()
        );
    }

    private void validar(
            Medicamento medicamento
    ) {

        if (medicamento == null) {

            throw new IllegalArgumentException(
                    "Datos del medicamento requeridos."
            );
        }

        if (medicamento.getNombre() == null
                || medicamento
                .getNombre()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (medicamento
                .getNombre()
                .trim()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "El nombre no puede exceder 200 caracteres."
            );
        }

        if (medicamento.getDescripcion() == null
                || medicamento
                .getDescripcion()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "La descripción es obligatoria."
            );
        }

        if (medicamento
                .getDescripcion()
                .trim()
                .length() > 500) {

            throw new IllegalArgumentException(
                    "La descripción no puede exceder 500 caracteres."
            );
        }

        BigDecimal precio =
                medicamento.getPrecio();

        if (precio == null
                || precio.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "El precio debe ser mayor a 0."
            );
        }

        if (precio.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("El precio excede la capacidad de 10 dígitos y 2 decimales.");
        }
        if (precio.scale() > 2) {

            throw new IllegalArgumentException(
                    "El precio debe tener máximo 2 decimales."
            );
        }

        if (medicamento.getUnidad() == null
                || medicamento
                .getUnidad()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "La unidad es obligatoria."
            );
        }

        if (medicamento
                .getUnidad()
                .trim()
                .length() > 50) {

            throw new IllegalArgumentException(
                    "La unidad no puede exceder 50 caracteres."
            );
        }

        if (medicamento.getStockMinimo() != null
                && medicamento
                .getStockMinimo() < 0) {

            throw new IllegalArgumentException(
                    "El stock mínimo debe ser mayor o igual a 0."
            );
        }
    }

    private void registrarAuditoria(
            String nombreUsuario,
            String accion,
            Medicamento medicamento,
            String detalle
    ) {

        Usuario usuario = control.usuario(nombreUsuario);
        control.invalidarTrasCommit();

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuario);
        auditoria.setAccion(accion);

        auditoria.setTipoEntidad(
                "MEDICAMENTO"
        );

        auditoria.setEntidadId(
                medicamento
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(detalle);

        auditoriaRepository
                .save(auditoria);
    }
}