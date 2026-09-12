package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.AgendarCitaDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.service.CitaCatalogoService;
import com.example.proyecto_semestre_medico.service.CitaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CitaController {

    private final CitaCatalogoService citaCatalogoService;
    private final CitaService citaService;

    public CitaController(
            CitaCatalogoService citaCatalogoService,
            CitaService citaService
    ) {
        this.citaCatalogoService =
                citaCatalogoService;
        this.citaService =
                citaService;
    }

    @GetMapping("/citas/nueva")
    @PreAuthorize("hasRole('PACIENTE')")
    public String nueva(
            Model model
    ) {

        model.addAttribute(
                "citaForm",
                new AgendarCitaDTO()
        );

        model.addAttribute(
                "sucursales",
                citaCatalogoService
                        .listarSucursales()
        );

        return "citas/nueva";
    }

    @PostMapping("/citas/guardar")
    @PreAuthorize("hasRole('PACIENTE')")
    public String guardar(
            @ModelAttribute("citaForm")
            AgendarCitaDTO dto,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Cita cita =
                    citaService.agendar(
                            authentication
                                    .getName(),
                            dto
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Cita reservada correctamente"
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "numeroCita",
                            cita.getNumeroCita()
                    );

            redirectAttributes.addFlashAttribute(
                    "citaId",
                    cita.getId()
            );

            return "redirect:/citas/confirmacion";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "sucursales",
                    citaCatalogoService
                            .listarSucursales()
            );

            return "citas/nueva";
        }
    }

    @GetMapping("/citas/confirmacion")
    @PreAuthorize("hasRole('PACIENTE')")
    public String confirmacion() {
        return "citas/confirmacion";
    }


}