package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.ExamenOpcionDTO;
import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.OrdenLaboratorioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public OrdenLaboratorioServiceImpl(
            OrdenLaboratorioRepository ordenLaboratorioRepository,
            ConsultaMedicaRepository consultaMedicaRepository,
            CitaRepository citaRepository,
            CatalogoExamenRepository catalogoExamenRepository,
            MedicoRepository medicoRepository
    ) {
        this.ordenLaboratorioRepository = ordenLaboratorioRepository;
        this.consultaMedicaRepository = consultaMedicaRepository;
        this.citaRepository = citaRepository;
        this.catalogoExamenRepository = catalogoExamenRepository;
        this.medicoRepository = medicoRepository;
    }

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

        /*
         * Seguridad:
         * el médico solamente puede generar órdenes
         * para sus propias citas.
         */
        if (!cita.getMedico()
                .getId()
                .equals(medico.getId())) {

            throw new IllegalArgumentException(
                    "La cita no está asignada al médico autenticado."
            );
        }

        /*
         * CU-08 indica que las órdenes se generan
         * desde la sección Evaluados.
         */
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

        /*
         * Eliminamos posibles IDs repetidos.
         */
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

            /*
             * Guardamos una copia del precio actual.
             * Si el catálogo cambia mañana, la orden
             * conserva el precio histórico.
             */
            detalle.setPrecio(
                    examen.getPrecio()
            );

            orden.getDetalles().add(
                    detalle
            );

            total = total.add(
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
                        .findByNumeroOrden(numero)
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