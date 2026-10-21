package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.PagoOnlineDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.Pago;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.service.PagoOnlineService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/pagos/online")
public class PagoController {

    private final CitaRepository citaRepository;
    private final PagoOnlineService pagoOnlineService;

    private final BigDecimal precioBase;

    public PagoController(
            CitaRepository citaRepository,
            PagoOnlineService pagoOnlineService,
            @Value("${app.citas.precio-base:250.00}")
            BigDecimal precioBase
    ) {
        this.citaRepository = citaRepository;
        this.pagoOnlineService = pagoOnlineService;
        this.precioBase = precioBase;
    }

    @GetMapping("/{citaId}")
    @PreAuthorize("hasRole('PACIENTE')")
    public String mostrarPago(
            @PathVariable Long citaId,
            Authentication authentication,
            Model model
    ) {

        Cita cita = citaRepository
                .findById(citaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cita no encontrada"
                        )
                );

        if (!cita.getPaciente()
                .getUsuario()
                .getNombreUsuario()
                .equalsIgnoreCase(authentication.getName())) {

            throw new IllegalArgumentException(
                    "La cita no pertenece al paciente autenticado"
            );
        }

        if (!"PENDIENTE_PAGO".equals(
                cita.getEstadoCita().getCodigo()
        )) {
            throw new IllegalArgumentException(
                    "La cita ya no está pendiente de pago"
            );
        }

        PagoOnlineDTO dto = new PagoOnlineDTO();

        dto.setCitaId(citaId);

        dto.setClaveIdempotencia(
                UUID.randomUUID().toString()
        );

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

        return "pagos/online";
    }

    @PostMapping("/procesar")
    @PreAuthorize("hasRole('PACIENTE')")
    public String procesarPago(
            @ModelAttribute("pagoForm")
            PagoOnlineDTO dto,
            Authentication authentication,
            Model model
    ) {

        try {

            Pago pago = pagoOnlineService.procesar(
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

                model.addAttribute(
                        "mensajeExito",
                        "¡Pago realizado exitosamente! Número de transacción: "
                                + pago.getNumeroTransaccion()
                                + ". Su cita ha sido confirmada."
                );

                return "pagos/comprobante";
            }

            model.addAttribute(
                    "error",
                    pago.getMensajePasarela()
            );

            Cita cita = citaRepository
                    .findById(dto.getCitaId())
                    .orElseThrow();

            model.addAttribute(
                    "cita",
                    cita
            );

            model.addAttribute(
                    "monto",
                    precioBase
            );

            /*
             * Generamos una nueva clave para un nuevo intento.
             */
            dto.setClaveIdempotencia(
                    UUID.randomUUID().toString()
            );

            /*
             * Limpiamos datos sensibles.
             */
            dto.setNumeroTarjeta(null);
            dto.setCvv(null);
            dto.setExpiracion(null);

            return "pagos/online";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            Cita cita = citaRepository
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

            /*
             * No devolvemos al HTML los datos de tarjeta.
             */
            dto.setNumeroTarjeta(null);
            dto.setCvv(null);
            dto.setExpiracion(null);

            /*
             * En error de validación mantenemos la misma
             * clave de idempotencia.
             */
            return "pagos/online";
        }
    }
}