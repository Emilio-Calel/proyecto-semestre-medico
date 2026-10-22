package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioPagoDTO;
import com.example.proyecto_semestre_medico.dto.PagoLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.PagoLaboratorio;
import com.example.proyecto_semestre_medico.service.PagoLaboratorioService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.dao.DataAccessException;

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

    @InitBinder("pagoForm")
    public void limitarCamposPago(WebDataBinder binder) {
        binder.setAllowedFields("ordenId", "metodoPago", "montoRecibido", "ultimos4");
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
                        "No se encontraron órdenes de laboratorio pendientes de pago. Verifique el DPI o número de orden e intente de nuevo."
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

        try {
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
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "caja/laboratorio/index";
        }
    }

    // =====================================================
    // PROCESAR
    // =====================================================

    @PostMapping("/procesar")
    public String procesar(
            @ModelAttribute("pagoForm")
            PagoLaboratorioFormDTO pagoForm,
            BindingResult bindingResult,

            Authentication authentication,

            Model model
    ) {

        try {

            if (bindingResult.hasErrors()) {
                throw new IllegalArgumentException("El monto recibido no es válido. Ingrese un número con hasta dos decimales.");
            }

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
                        "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago."
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
                | IllegalStateException | DataAccessException e
        ) {

            model.addAttribute(
                    "error",
                    e instanceof DataAccessException
                            ? "No fue posible registrar el pago. Verifique la orden antes de intentar nuevamente."
                            : e.getMessage()
            );

            try {

                model.addAttribute(
                        "orden",
                        pagoLaboratorioService
                                .obtenerOrdenParaPago(
                                        pagoForm.getOrdenId()
                                )
                );

            } catch (IllegalArgumentException | DataAccessException noDisponible) {
                return "caja/laboratorio/index";
            }

            return "caja/laboratorio/pago";
        }
    }
}
