package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.PagoPresencialDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.Pago;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.service.PagoPresencialService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/caja")
@PreAuthorize("hasAuthority('PAYMENT_VIEW')")
public class CajaController {

    private final CitaRepository citaRepository;
    private final PagoPresencialService pagoPresencialService;

    private final BigDecimal precioBase;

    public CajaController(
            CitaRepository citaRepository,
            PagoPresencialService pagoPresencialService,
            @Value("${app.citas.precio-base:250.00}")
            BigDecimal precioBase
    ) {
        this.citaRepository = citaRepository;
        this.pagoPresencialService =
                pagoPresencialService;
        this.precioBase = precioBase;
    }

    @GetMapping
    public String inicio() {
        return "caja/index";
    }

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam String numeroCita,
            Model model
    ) {

        if (numeroCita == null
                || numeroCita.isBlank()) {

            model.addAttribute(
                    "error",
                    "Ingrese un número de cita"
            );

            return "caja/index";
        }

        Cita cita =
                citaRepository
                        .findByNumeroCitaIgnoreCase(
                                numeroCita.trim()
                        )
                        .orElse(null);

        if (cita == null) {

            model.addAttribute(
                    "error",
                    "Cita no encontrada"
            );

            return "caja/index";
        }

        model.addAttribute(
                "cita",
                cita
        );

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

                return "caja/comprobante";
            }

            model.addAttribute(
                    "error",
                    "El pago fue rechazado"
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

        dto.setNumeroTarjeta(null);
        dto.setCvv(null);
        dto.setExpiracion(null);

        return "caja/pago";
    }
}