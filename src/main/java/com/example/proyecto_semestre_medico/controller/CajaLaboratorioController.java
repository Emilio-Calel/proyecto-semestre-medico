package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioPagoDTO;
import com.example.proyecto_semestre_medico.dto.PagoLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.PagoLaboratorio;
import com.example.proyecto_semestre_medico.service.PagoLaboratorioService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/caja/laboratorio")
@PreAuthorize("hasRole('CAJERO')")
public class CajaLaboratorioController {

    private final PagoLaboratorioService
            pagoLaboratorioService;

    public CajaLaboratorioController(
            PagoLaboratorioService pagoLaboratorioService
    ) {
        this.pagoLaboratorioService =
                pagoLaboratorioService;
    }

    // =====================================================
    // PANTALLA PRINCIPAL
    // =====================================================

    @GetMapping
    public String inicio() {

        return "caja/laboratorio/index";
    }

    // =====================================================
    // BÚSQUEDA
    // =====================================================

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam String tipoBusqueda,
            @RequestParam String valor,
            Model model
    ) {

        try {

            List<OrdenLaboratorioPagoDTO> ordenes;

            if ("DPI".equalsIgnoreCase(
                    tipoBusqueda
            )) {

                ordenes =
                        pagoLaboratorioService
                                .buscarPorDpi(
                                        valor
                                );

            } else if ("ORDEN".equalsIgnoreCase(
                    tipoBusqueda
            )) {

                ordenes =
                        pagoLaboratorioService
                                .buscarPorNumeroOrden(
                                        valor
                                );

            } else {

                throw new IllegalArgumentException(
                        "El tipo de búsqueda no es válido."
                );
            }

            model.addAttribute(
                    "ordenes",
                    ordenes
            );

            model.addAttribute(
                    "tipoBusqueda",
                    tipoBusqueda
            );

            model.addAttribute(
                    "valorBusqueda",
                    valor
            );

            if (ordenes.isEmpty()) {

                model.addAttribute(
                        "mensajeBusqueda",
                        "No se encontraron órdenes pendientes de pago."
                );
            }

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "caja/laboratorio/index";
    }

    // =====================================================
    // FORMULARIO DE PAGO
    // =====================================================

    @GetMapping("/pagar/{ordenId}")
    public String mostrarPago(
            @PathVariable Long ordenId,
            Model model
    ) {

        OrdenLaboratorioPagoDTO orden =
                pagoLaboratorioService
                        .obtenerOrdenParaPago(
                                ordenId
                        );

        PagoLaboratorioFormDTO pagoForm =
                new PagoLaboratorioFormDTO();

        pagoForm.setOrdenId(
                ordenId
        );

        model.addAttribute(
                "orden",
                orden
        );

        model.addAttribute(
                "pagoForm",
                pagoForm
        );

        return "caja/laboratorio/pago";
    }

    // =====================================================
    // PROCESAR
    // =====================================================

    @PostMapping("/procesar")
    public String procesar(
            @ModelAttribute("pagoForm")
            PagoLaboratorioFormDTO pagoForm,

            Authentication authentication,

            Model model
    ) {

        try {

            PagoLaboratorio pago =
                    pagoLaboratorioService
                            .procesarPago(
                                    authentication.getName(),
                                    pagoForm
                            );

            /*
             * Si fue rechazado, la orden continúa
             * PENDIENTE y permitimos intentar nuevamente.
             */
            if (!"APROBADO".equals(
                    pago.getEstado()
            )) {

                model.addAttribute(
                        "error",
                        "El pago con tarjeta fue rechazado."
                );

                model.addAttribute(
                        "orden",
                        pagoLaboratorioService
                                .obtenerOrdenParaPago(
                                        pagoForm.getOrdenId()
                                )
                );

                pagoForm.setUltimos4(
                        null
                );

                return "caja/laboratorio/pago";
            }

            model.addAttribute(
                    "pago",
                    pago
            );

            model.addAttribute(
                    "orden",
                    pago.getOrdenLaboratorio()
            );

            return "caja/laboratorio/comprobante";

        } catch (
                IllegalArgumentException
                | IllegalStateException e
        ) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            try {

                model.addAttribute(
                        "orden",
                        pagoLaboratorioService
                                .obtenerOrdenParaPago(
                                        pagoForm.getOrdenId()
                                )
                );

            } catch (Exception ignored) {
            }

            return "caja/laboratorio/pago";
        }
    }
}