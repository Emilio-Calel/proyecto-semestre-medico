package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.PagoPresencialDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.Pago;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.service.PagoPresencialService;
import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.math.BigDecimal;

@Controller
@RequestMapping("/caja")
@PreAuthorize("hasAuthority('PAYMENT_VIEW')")
public class CajaController {

    private final CitaRepository citaRepository;
    private final PagoPresencialService pagoPresencialService;
    private final DatosSensiblesService datosSensiblesService;

    private final BigDecimal precioBase;

    public CajaController(
            CitaRepository citaRepository,
            PagoPresencialService pagoPresencialService,
            DatosSensiblesService datosSensiblesService,
            @Value("${app.citas.precio-base:250.00}")
            BigDecimal precioBase
    ) {
        this.citaRepository = citaRepository;
        this.pagoPresencialService =
                pagoPresencialService;
        this.datosSensiblesService =
                datosSensiblesService;
        this.precioBase = precioBase;
    }

    @GetMapping
    public String inicio() {
        return "caja/index";
    }

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam String criterio,
            @RequestParam(
                    defaultValue = "CITA"
            ) String tipoBusqueda,
            Model model
    ) {

        if (criterio == null
                || criterio.isBlank()) {

            model.addAttribute(
                    "error",
                    "No hay citas pendientes de pago bajo los parámetros indicados."
            );

            model.addAttribute(
                    "tipoBusqueda",
                    tipoBusqueda
            );

            return "caja/index";
        }

        String valor =
                criterio.trim();

        String tipo =
                tipoBusqueda == null
                        ? "CITA"
                        : tipoBusqueda
                        .trim()
                        .toUpperCase();

        List<Cita> resultados;

        if ("DPI".equals(tipo)) {

            /*
             * Aplicamos RN-GLOBAL-001
             * porque la búsqueda utiliza DPI.
             */
            if (!valor.matches("^[0-9]+$")) {

                model.addAttribute(
                        "error",
                        "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales."
                );

                model.addAttribute(
                        "tipoBusqueda",
                        "DPI"
                );

                model.addAttribute(
                        "criterio",
                        valor
                );

                return "caja/index";
            }

            if (valor.length() != 13) {

                model.addAttribute(
                        "error",
                        "El DPI debe contener exactamente 13 dígitos. Usted ingresó "
                                + valor.length()
                                + " dígitos."
                );

                model.addAttribute(
                        "tipoBusqueda",
                        "DPI"
                );

                model.addAttribute(
                        "criterio",
                        valor
                );

                return "caja/index";
            }

            String dpiHash =
                    datosSensiblesService
                            .generarHashBusqueda(
                                    valor
                            );

            resultados =
                    citaRepository
                            .buscarPendientesPagoPorDpiHash(
                                    dpiHash
                            );

        } else {

            resultados =
                    citaRepository
                            .buscarPendientePagoPorNumeroCita(
                                    valor
                            )
                            .map(List::of)
                            .orElseGet(List::of);
        }

        model.addAttribute(
                "tipoBusqueda",
                tipo
        );

        model.addAttribute(
                "criterio",
                valor
        );

        model.addAttribute(
                "resultados",
                resultados
        );

        if (resultados.isEmpty()) {

            model.addAttribute(
                    "error",
                    "No hay citas pendientes de pago bajo los parámetros indicados."
            );
        }

        return "caja/index";
    }

    @GetMapping("/pagar/{citaId}")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public String mostrarPago(
            @PathVariable Long citaId,
            Model model
    ) {

        Cita cita =
                citaRepository
                        .findById(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada"
                                )
                        );

        if (!"PENDIENTE_PAGO".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "La cita no está pendiente de pago"
            );
        }

        PagoPresencialDTO dto =
                new PagoPresencialDTO();

        dto.setCitaId(citaId);

        model.addAttribute(
                "pagoForm",
                dto
        );

        model.addAttribute(
                "cita",
                cita
        );

        model.addAttribute(
                "monto",
                precioBase
        );

        return "caja/pago";
    }

    @PostMapping("/procesar")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public String procesar(
            @ModelAttribute("pagoForm")
            PagoPresencialDTO dto,
            Authentication authentication,
            Model model
    ) {

        try {

            Pago pago =
                    pagoPresencialService
                            .procesar(
                                    authentication.getName(),
                                    dto
                            );

            if ("APROBADO".equals(
                    pago.getEstado()
            )) {

                model.addAttribute(
                        "pago",
                        pago
                );

                model.addAttribute(
                        "cita",
                        pago.getCita()
                );

                if ("EFECTIVO".equals(
                        pago.getMetodoPago()
                )) {

                    BigDecimal montoRecibido =
                            dto.getMontoRecibido();

                    BigDecimal cambio =
                            montoRecibido.subtract(
                                    pago.getMonto()
                            );

                    model.addAttribute(
                            "montoRecibido",
                            montoRecibido
                    );

                    model.addAttribute(
                            "cambio",
                            cambio
                    );

                    model.addAttribute(
                            "mensajeCambio",
                            "Monto recibido: Q"
                                    + montoRecibido
                                    + ". Cambio a devolver: Q"
                                    + cambio
                                    + "."
                    );
                }

                model.addAttribute(
                        "mensajeExito",
                        "¡Pago registrado exitosamente! Paciente: "
                                + pago.getCita()
                                .getPaciente()
                                .getNombreCompleto()
                                + ". La cita ha sido actualizada a estado Confirmada."
                );

                return "caja/comprobante";
            }

            model.addAttribute(
                    "error",
                    "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );
        }

        Cita cita =
                citaRepository
                        .findById(dto.getCitaId())
                        .orElse(null);

        model.addAttribute(
                "cita",
                cita
        );

        model.addAttribute(
                "monto",
                precioBase
        );

        return "caja/pago";
    }
}