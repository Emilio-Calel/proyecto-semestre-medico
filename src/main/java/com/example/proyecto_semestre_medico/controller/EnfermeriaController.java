package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.SignosVitalesDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.service.SignosVitalesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/enfermeria")
@PreAuthorize("hasAuthority('VITAL_SIGNS_VIEW')")
public class EnfermeriaController {

    private final SignosVitalesService signosVitalesService;

    public EnfermeriaController(
            SignosVitalesService signosVitalesService
    ) {
        this.signosVitalesService = signosVitalesService;
    }

    @GetMapping
    public String inicio(
            Model model
    ) {

        model.addAttribute(
                "pacientes",
                signosVitalesService
                        .listarPacientesPresentes()
        );

        return "enfermeria/index";
    }

    @PostMapping("/citas/{citaId}/iniciar")
    @PreAuthorize("hasAuthority('VITAL_SIGNS_CREATE')")
    public String iniciarToma(
            @PathVariable Long citaId,
            RedirectAttributes redirectAttributes
    ) {

        try {

            signosVitalesService
                    .iniciarToma(citaId);

            return "redirect:/enfermeria/citas/"
                    + citaId
                    + "/signos";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            return "redirect:/enfermeria";
        }
    }

    @GetMapping("/citas/{citaId}/signos")
    @PreAuthorize("hasAuthority('VITAL_SIGNS_CREATE')")
    public String formulario(
            @PathVariable Long citaId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Cita cita =
                    signosVitalesService
                            .buscarCitaParaSignos(
                                    citaId
                            );

            SignosVitalesDTO dto =
                    new SignosVitalesDTO();

            dto.setCitaId(citaId);

            model.addAttribute(
                    "cita",
                    cita
            );

            model.addAttribute(
                    "signosForm",
                    dto
            );

            return "enfermeria/signos";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            return "redirect:/enfermeria";
        }
    }

    @PostMapping("/signos/guardar")
    @PreAuthorize("hasAuthority('VITAL_SIGNS_CREATE')")
    public String guardar(
            @ModelAttribute("signosForm")
            SignosVitalesDTO dto,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            signosVitalesService
                    .registrar(
                            authentication.getName(),
                            dto
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Signos vitales registrados correctamente"
                    );

            return "redirect:/enfermeria";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            Cita cita =
                    signosVitalesService
                            .buscarCitaParaSignos(
                                    dto.getCitaId()
                            );

            model.addAttribute(
                    "cita",
                    cita
            );

            return "enfermeria/signos";
        }
    }
}