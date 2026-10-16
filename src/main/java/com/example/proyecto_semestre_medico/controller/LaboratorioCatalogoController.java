package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.Laboratorio;
import com.example.proyecto_semestre_medico.service.LaboratorioCatalogoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/laboratorios")

public class LaboratorioCatalogoController {

    private final LaboratorioCatalogoService
            laboratorioCatalogoService;

    public LaboratorioCatalogoController(
            LaboratorioCatalogoService
                    laboratorioCatalogoService
    ) {
        this.laboratorioCatalogoService =
                laboratorioCatalogoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOG_VIEW')")
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "laboratorios",
                laboratorioCatalogoService
                        .listarTodos()
        );

        return "admin/laboratorios/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('CATALOG_CREATE')")
    public String nuevo(
            Model model
    ) {

        model.addAttribute(
                "laboratorio",
                new Laboratorio()
        );

        return "admin/laboratorios/formulario";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('CATALOG_UPDATE')")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "laboratorio",
                laboratorioCatalogoService
                        .buscarPorId(id)
        );

        return "admin/laboratorios/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize(
            "hasAuthority('CATALOG_CREATE') or hasAuthority('CATALOG_UPDATE')"
    )
    public String guardar(
            @ModelAttribute("laboratorio")
            Laboratorio laboratorio,

            Principal principal,

            Model model,

            RedirectAttributes
                    redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            boolean nuevo =
                    laboratorio.getId() == null;

            laboratorioCatalogoService
                    .guardar(
                            laboratorio,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            nuevo
                                    ? "Laboratorio creado correctamente."
                                    : "Laboratorio actualizado correctamente."
                    );

            return "redirect:/admin/laboratorios";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/laboratorios/formulario";
        }
    }

    @PostMapping("/desactivar/{id}")
    @PreAuthorize("hasAuthority('CATALOG_DISABLE')")
    public String desactivar(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes
                    redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            laboratorioCatalogoService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Laboratorio desactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/laboratorios";
    }

    @PostMapping("/reactivar/{id}")
    @PreAuthorize("hasAuthority('CATALOG_UPDATE')")
    public String reactivar(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes
                    redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            laboratorioCatalogoService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Laboratorio reactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/laboratorios";
    }
}