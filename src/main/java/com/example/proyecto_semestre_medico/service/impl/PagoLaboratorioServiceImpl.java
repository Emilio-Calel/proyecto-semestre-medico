package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioPagoDTO;
import com.example.proyecto_semestre_medico.dto.PagoLaboratorioFormDTO;

import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.OrdenLaboratorio;
import com.example.proyecto_semestre_medico.model.PagoLaboratorio;
import com.example.proyecto_semestre_medico.model.Usuario;

import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.OrdenLaboratorioRepository;
import com.example.proyecto_semestre_medico.repository.PagoLaboratorioRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;

import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import com.example.proyecto_semestre_medico.service.PagoLaboratorioService;

import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@PreAuthorize("hasRole('CAJERO')")
public class PagoLaboratorioServiceImpl
        implements PagoLaboratorioService {

    private final OrdenLaboratorioRepository
            ordenLaboratorioRepository;

    private final PagoLaboratorioRepository
            pagoLaboratorioRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    private final DatosSensiblesService
            datosSensiblesService;

    public PagoLaboratorioServiceImpl(
            OrdenLaboratorioRepository ordenLaboratorioRepository,
            PagoLaboratorioRepository pagoLaboratorioRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            DatosSensiblesService datosSensiblesService
    ) {
        this.ordenLaboratorioRepository =
                ordenLaboratorioRepository;

        this.pagoLaboratorioRepository =
                pagoLaboratorioRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;

        this.datosSensiblesService =
                datosSensiblesService;
    }

    // =====================================================
    // BÚSQUEDA POR DPI
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrdenLaboratorioPagoDTO> buscarPorDpi(
            String dpi
    ) {

        validarDpi(
                dpi
        );

        String dpiLimpio =
                dpi.trim();

        String dpiHash =
                datosSensiblesService
                        .generarHashBusqueda(
                                dpiLimpio
                        );

        return ordenLaboratorioRepository
                .findByPacienteDpiHashAndEstadoAndExternaFalseOrderByFechaCreacionDesc(
                        dpiHash,
                        "PENDIENTE"
                )
                .stream()
                .filter(this::estaPendienteDePago)
                .map(this::convertirOrden)
                .toList();
    }

    // =====================================================
    // BÚSQUEDA POR No. ORDEN
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrdenLaboratorioPagoDTO> buscarPorNumeroOrden(
            String numeroOrden
    ) {

        if (numeroOrden == null
                || numeroOrden.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe ingresar el número de orden."
            );
        }

        OrdenLaboratorio orden =
                ordenLaboratorioRepository
                        .findByNumeroOrden(
                                numeroOrden
                                        .trim()
                                        .toUpperCase()
                        )
                        .orElse(null);

        if (orden == null) {
            return List.of();
        }

        /*
         * Caja solamente cobra órdenes internas
         * que todavía estén pendientes.
         */
        if (!"PENDIENTE".equals(
                orden.getEstado()
        )) {
            return List.of();
        }

        if (Boolean.TRUE.equals(
                orden.getExterna()
        )) {
            return List.of();
        }

        if (!estaPendienteDePago(orden)) {
            return List.of();
        }

        return List.of(
                convertirOrden(
                        orden
                )
        );
    }

    // =====================================================
    // OBTENER ORDEN PARA PANTALLA DE PAGO
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public OrdenLaboratorioPagoDTO obtenerOrdenParaPago(
            Long ordenId
    ) {

        OrdenLaboratorio orden =
                obtenerOrdenPendiente(
                        ordenId
                );

        return convertirOrden(
                orden
        );
    }

    // =====================================================
    // PROCESAR PAGO
    // =====================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('CAJERO') and #nombreUsuarioCajero == authentication.name")
    public PagoLaboratorio procesarPago(
            String nombreUsuarioCajero,
            PagoLaboratorioFormDTO dto
    ) {

        validarBasico(
                dto
        );

        OrdenLaboratorio orden =
                obtenerOrdenPendiente(
                        dto.getOrdenId(), true
                );

        Usuario cajero =
                usuarioRepository
                        .findByNombreUsuarioIgnoreCase(
                                nombreUsuarioCajero
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El usuario cajero no existe."
                                )
                        );

        String metodo =
                dto.getMetodoPago()
                        .trim()
                        .toUpperCase();

        PagoLaboratorio pago =
                new PagoLaboratorio();

        pago.setOrdenLaboratorio(
                orden
        );

        pago.setNumeroTransaccion(
                generarNumeroTransaccion()
        );

        pago.setMonto(
                orden.getMontoTotal()
        );

        pago.setMetodoPago(
                metodo
        );

        pago.setUsuarioCajero(
                cajero
        );

        /*
         * =================================================
         * EFECTIVO
         * =================================================
         */
        if ("EFECTIVO".equals(
                metodo
        )) {

            BigDecimal recibido =
                    dto.getMontoRecibido();

            if (recibido == null) {

                throw new IllegalArgumentException(
                        "Debe ingresar el monto recibido."
                );
            }

            try {
                recibido = recibido.setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("El monto recibido debe tener como máximo dos decimales.");
            }
            if (recibido.signum() < 0 || recibido.precision() > 10) {
                throw new IllegalArgumentException("El monto recibido no es válido.");
            }

            if (recibido.compareTo(
                    orden.getMontoTotal()
            ) < 0) {

                throw new IllegalArgumentException(
                        "El monto recibido (Q" + recibido.toPlainString()
                                + ") es menor al monto a cobrar (Q" + orden.getMontoTotal().setScale(2).toPlainString() + ")"
                );
            }

            BigDecimal cambio =
                    recibido.subtract(
                            orden.getMontoTotal()
                    );

            pago.setMontoRecibido(
                    recibido
            );

            pago.setCambio(
                    cambio
            );

            pago.setUltimos4(
                    null
            );

            pago.setEstado(
                    "APROBADO"
            );

            pago.setReferencia(
                    "EFECTIVO"
            );

        } else {

            /*
             * =============================================
             * TARJETA
             * =============================================
             */

            String ultimos4 =
                    dto.getUltimos4() == null
                            ? ""
                            : dto.getUltimos4()
                            .trim();

            if (!ultimos4.matches(
                    "^[0-9]{4}$"
            )) {

                throw new IllegalArgumentException(
                        "Ingrese los últimos 4 dígitos de la tarjeta."
                );
            }

            pago.setUltimos4(
                    ultimos4
            );

            pago.setMontoRecibido(
                    null
            );

            pago.setCambio(
                    null
            );

            /*
             * Simulación controlada de rechazo.
             *
             * Para pruebas:
             * 0000 = RECHAZADO
             *
             * Cualquier otro grupo de 4 dígitos
             * se considera aprobado.
             */
            if ("0000".equals(
                    ultimos4
            )) {

                throw new IllegalArgumentException(
                        "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.");

            } else {

                pago.setEstado(
                        "APROBADO"
                );

                pago.setReferencia(
                        "SIM-"
                                + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase()
                );
            }
        }

        PagoLaboratorio pagoGuardado =
                pagoLaboratorioRepository.save(
                        pago
                );

        /*
         * SOLO si el pago fue aprobado:
         *
         * PENDIENTE -> EN_PROCESO
         */
        if ("APROBADO".equals(
                pagoGuardado.getEstado()
        )) {

            orden.setEstado(
                    "EN_PROCESO"
            );

            ordenLaboratorioRepository.save(
                    orden
            );

            registrarAuditoria(
                    orden,
                    pagoGuardado,
                    cajero
            );
        }

        return pagoGuardado;
    }

    // =====================================================
    // VALIDACIONES
    // =====================================================

    private void validarDpi(
            String dpi
    ) {

        if (dpi == null
                || !dpi.trim()
                .matches("^[0-9]{13}$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener exactamente 13 dígitos numéricos."
            );
        }
    }

    private void validarBasico(
            PagoLaboratorioFormDTO dto
    ) {

        if (dto == null
                || dto.getOrdenId() == null) {

            throw new IllegalArgumentException(
                    "La orden de laboratorio es obligatoria."
            );
        }

        if (dto.getMetodoPago() == null
                || dto.getMetodoPago()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un método de pago."
            );
        }

        String metodo =
                dto.getMetodoPago()
                        .trim()
                        .toUpperCase();

        if (!metodo.equals("EFECTIVO")
                && !metodo.equals("VISA")
                && !metodo.equals("MASTERCARD")
                && !metodo.equals("DEBITO")) {

            throw new IllegalArgumentException(
                    "El método de pago seleccionado no es válido."
            );
        }
    }

    private OrdenLaboratorio obtenerOrdenPendiente(
            Long ordenId
    ) {
        return obtenerOrdenPendiente(ordenId, false);
    }

    private OrdenLaboratorio obtenerOrdenPendiente(Long ordenId, boolean bloquear) {

        if (ordenId == null) {

            throw new IllegalArgumentException(
                    "La orden es obligatoria."
            );
        }

        OrdenLaboratorio orden =
                (bloquear ? ordenLaboratorioRepository.buscarParaProcesar(ordenId)
                        : ordenLaboratorioRepository.findById(ordenId))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La orden de laboratorio no existe."
                                )
                        );

        if (Boolean.TRUE.equals(
                orden.getExterna()
        )) {

            throw new IllegalArgumentException(
                    "Las órdenes externas no se cobran en caja."
            );
        }

        if (!"PENDIENTE".equals(
                orden.getEstado()
        )) {

            throw new IllegalArgumentException(
                    "La orden no se encuentra pendiente de pago."
            );
        }

        if (pagoLaboratorioRepository.existsByOrdenLaboratorioIdAndEstado(ordenId, "APROBADO")) {
            throw new IllegalArgumentException("La orden ya posee un pago aprobado.");
        }
        if (orden.getMontoTotal() == null || orden.getMontoTotal().signum() < 0
                || orden.getMontoTotal().stripTrailingZeros().scale() > 2
                || orden.getMontoTotal().setScale(2).precision() > 10) {
            throw new IllegalArgumentException("El monto total de la orden no es válido.");
        }

        return orden;
    }

    private boolean estaPendienteDePago(OrdenLaboratorio orden) {
        return "PENDIENTE".equals(orden.getEstado()) && !Boolean.TRUE.equals(orden.getExterna())
                && !pagoLaboratorioRepository.existsByOrdenLaboratorioIdAndEstado(orden.getId(), "APROBADO");
    }

    // =====================================================
    // DTO
    // =====================================================

    private OrdenLaboratorioPagoDTO convertirOrden(
            OrdenLaboratorio orden
    ) {

        OrdenLaboratorioPagoDTO dto =
                new OrdenLaboratorioPagoDTO();

        dto.setId(
                orden.getId()
        );

        dto.setNumeroOrden(
                orden.getNumeroOrden()
        );

        dto.setPaciente(
                orden.getPaciente()
                        .getNombreCompleto()
        );

        /*
         * Para mostrar el DPI solamente en caja
         * lo desciframos aquí.
         */
        dto.setDpi(
                datosSensiblesService
                        .descifrar(
                                orden.getPaciente()
                                        .getDpiCifrado()
                        )
        );

        dto.setCantidadExamenes(
                orden.getDetalles() == null
                        ? 0
                        : orden.getDetalles()
                        .size()
        );

        dto.setFechaCreacion(
                orden.getFechaCreacion()
        );

        dto.setMontoTotal(
                orden.getMontoTotal()
        );

        return dto;
    }

    // =====================================================
    // TRANSACCIÓN
    // =====================================================

    private String generarNumeroTransaccion() {

        String numero;

        do {

            numero =
                    "LABPAY-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 10)
                            .toUpperCase();

        } while (
                pagoLaboratorioRepository
                        .existsByNumeroTransaccion(
                                numero
                        )
        );

        return numero;
    }

    // =====================================================
    // AUDITORÍA
    // =====================================================

    private void registrarAuditoria(
            OrdenLaboratorio orden,
            PagoLaboratorio pago,
            Usuario cajero
    ) {

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                cajero
        );

        auditoria.setAccion(
                "PAGO_LABORATORIO"
        );

        auditoria.setTipoEntidad(
                "PAGO_LABORATORIO"
        );

        auditoria.setEntidadId(
                String.valueOf(
                        pago.getId()
                )
        );

        auditoria.setDetalle(
                "Pago de orden "
                        + orden.getNumeroOrden()
                        + ". Método: "
                        + pago.getMetodoPago()
                        + ". Estado: "
                        + pago.getEstado()
        );

        auditoriaRepository.save(
                auditoria
        );
    }
}
