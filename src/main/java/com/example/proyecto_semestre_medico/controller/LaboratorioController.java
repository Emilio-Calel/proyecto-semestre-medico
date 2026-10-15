package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioDetalleDTO;
import com.example.proyecto_semestre_medico.dto.ResultadoLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.service.OrdenLaboratorioService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/laboratorio")
@PreAuthorize("hasRole('LABORATORISTA')")
public class LaboratorioController {

    private final OrdenLaboratorioService
            ordenLaboratorioService;

    public LaboratorioController(
            OrdenLaboratorioService ordenLaboratorioService
    ) {
        this.ordenLaboratorioService =
                ordenLaboratorioService;
    }

    // =====================================================
    // LISTADO PRINCIPAL
    // =====================================================

    @GetMapping
    public String listar(
            @RequestParam(
                    required = false
            )
            String estado,

            @RequestParam(
                    required = false
            )
            String paciente,

            @RequestParam(
                    required = false
            )
            String medico,

            Model model
    ) {

        model.addAttribute(
                "ordenes",
                ordenLaboratorioService
                        .listarOrdenes(
                                estado,
                                paciente,
                                medico
                        )
        );

        model.addAttribute(
                "estadoFiltro",
                estado
        );

        model.addAttribute(
                "pacienteFiltro",
                paciente
        );

        model.addAttribute(
                "medicoFiltro",
                medico
        );

        return "laboratorio/ordenes";
    }

    // =====================================================
    // DETALLE
    // =====================================================

    @GetMapping("/{ordenId}")
    public String detalle(
            @PathVariable Long ordenId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            OrdenLaboratorioDetalleDTO orden =
                    ordenLaboratorioService
                            .obtenerDetalle(
                                    ordenId
                            );

            model.addAttribute(
                    "orden",
                    orden
            );

            ResultadoLaboratorioFormDTO resultadoForm =
                    new ResultadoLaboratorioFormDTO();

            resultadoForm.setFechaResultado(
                    java.time.LocalDateTime.now()
            );

            model.addAttribute(
                    "resultadoForm",
                    resultadoForm
            );

            return "laboratorio/detalle";

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            return "redirect:/laboratorio";
        }
    }

    // =====================================================
    // GUARDAR RESULTADO
    // =====================================================

    @PostMapping("/resultado/guardar")
    public String guardarResultado(
            @ModelAttribute
            ResultadoLaboratorioFormDTO resultadoForm,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ordenLaboratorioService
                    .guardarResultado(
                            resultadoForm,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Resultado guardado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        if (resultadoForm.getOrdenId() == null) {

            return "redirect:/laboratorio";
        }

        return "redirect:/laboratorio/"
                + resultadoForm.getOrdenId();
    }

    // =====================================================
    // PUBLICAR RESULTADO
    // =====================================================

    @PostMapping("/resultado/publicar/{detalleId}")
    public String publicarResultado(
            @PathVariable Long detalleId,

            @RequestParam Long ordenId,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            ordenLaboratorioService
                    .publicarResultado(
                            detalleId,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Resultado publicado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/laboratorio/"
                + ordenId;
    }

    /*
     * Este helper lo reemplazaremos en un momento
     * por una solución más directa desde el formulario.
     *
     * Por ahora NO lo implementes.
     */

}