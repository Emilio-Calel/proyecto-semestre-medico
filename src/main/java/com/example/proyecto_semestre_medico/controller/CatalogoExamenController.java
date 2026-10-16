package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.CatalogoExamen;
import com.example.proyecto_semestre_medico.service.CatalogoExamenService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/examenes-laboratorio")

public class CatalogoExamenController {

    private final CatalogoExamenService
            catalogoExamenService;

    public CatalogoExamenController(
            CatalogoExamenService catalogoExamenService
    ) {
        this.catalogoExamenService =
                catalogoExamenService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOG_VIEW')")
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "examenes",
                catalogoExamenService
                        .listarTodos()
        );

        return "admin/examenes-laboratorio/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('CATALOG_CREATE')")
    public String nuevo(
            Model model
    ) {

        model.addAttribute(
                "examen",
                new CatalogoExamen()
        );

        model.addAttribute(
                "laboratorios",
                catalogoExamenService
                        .listarLaboratoriosActivos()
        );

        return "admin/examenes-laboratorio/formulario";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('CATALOG_UPDATE')")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        CatalogoExamen examen =
                catalogoExamenService
                        .buscarPorId(id);

        model.addAttribute(
                "examen",
                examen
        );

        model.addAttribute(
                "laboratorios",
                catalogoExamenService
                        .listarLaboratoriosActivos()
        );

        model.addAttribute(
                "laboratorioSeleccionado",
                examen.getLaboratorio()
                        .getId()
        );

        return "admin/examenes-laboratorio/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize(
            "hasAuthority('CATALOG_CREATE') or hasAuthority('CATALOG_UPDATE')"
    )
    public String guardar(
            @ModelAttribute("examen")
            CatalogoExamen examen,

            @RequestParam(
                    name = "laboratorioId",
                    required = false
            )
            Long laboratorioId,

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
                    examen.getId() == null;

            catalogoExamenService
                    .guardar(
                            examen,
                            laboratorioId,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            nuevo
                                    ? "Examen creado correctamente."
                                    : "Examen actualizado correctamente."
                    );

            return "redirect:/admin/examenes-laboratorio";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "laboratorios",
                    catalogoExamenService
                            .listarLaboratoriosActivos()
            );

            model.addAttribute(
                    "laboratorioSeleccionado",
                    laboratorioId
            );

            return "admin/examenes-laboratorio/formulario";
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

            catalogoExamenService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Examen desactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/examenes-laboratorio";
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

            catalogoExamenService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Examen reactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/examenes-laboratorio";
    }
}