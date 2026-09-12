package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.service.RecepcionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recepcion")
@PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
public class RecepcionController {

    private final RecepcionService recepcionService;

    public RecepcionController(
            RecepcionService recepcionService
    ) {
        this.recepcionService =
                recepcionService;
    }

    @GetMapping
    public String inicio(
            @RequestParam(
                    required = false
            )
            String criterio,
            Model model
    ) {

        model.addAttribute(
                "criterio",
                criterio
        );

        if (criterio != null
                && !criterio.isBlank()) {

            model.addAttribute(
                    "resultados",
                    recepcionService
                            .buscar(criterio)
            );
        }

        return "recepcion/index";
    }

    @PostMapping(
            "/citas/{citaId}/llegada"
    )
    @PreAuthorize("hasAuthority('APPOINTMENT_CHECK_IN')")
    public String registrarLlegada(
            @PathVariable Long citaId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            recepcionService
                    .registrarLlegada(
                            citaId,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Llegada registrada correctamente"
                    );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/recepcion";
    }
}