package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.service.EspecialidadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/especialidades")

public class EspecialidadController {

    private final EspecialidadService
            especialidadService;

    public EspecialidadController(
            EspecialidadService especialidadService
    ) {
        this.especialidadService =
                especialidadService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOG_VIEW')")
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "especialidades",
                especialidadService
                        .listarTodas()
        );

        return "admin/especialidades/lista";
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasAuthority('CATALOG_CREATE')")
    public String nueva(
            Model model
    ) {

        model.addAttribute(
                "especialidad",
                new Especialidad()
        );

        return "admin/especialidades/formulario";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('CATALOG_UPDATE')")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "especialidad",
                especialidadService
                        .buscarPorId(id)
        );

        return "admin/especialidades/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize(
            "hasAuthority('CATALOG_CREATE') or hasAuthority('CATALOG_UPDATE')"
    )
    public String guardar(
            @ModelAttribute("especialidad")
            Especialidad especialidad,

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

            boolean nueva =
                    especialidad.getId() == null;

            especialidadService
                    .guardar(
                            especialidad,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            nueva
                                    ? "Especialidad creada correctamente."
                                    : "Especialidad actualizada correctamente."
                    );

            return "redirect:/admin/especialidades";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/especialidades/formulario";
        }
    }

    @PostMapping("/desactivar/{id}")
    @PreAuthorize("hasAuthority('CATALOG_DISABLE')")
    public String desactivar(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            especialidadService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Especialidad desactivada correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/especialidades";
    }

    @PostMapping("/reactivar/{id}")
    @PreAuthorize("hasAuthority('CATALOG_UPDATE')")
    public String reactivar(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            especialidadService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Especialidad reactivada correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/especialidades";
    }
}