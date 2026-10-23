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

    @InitBinder("examen")
    public void camposEditables(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "codigo", "nombre", "descripcion", "precio", "unidadReferencia", "rangoReferencia");
    }

    private final CatalogoExamenService
            catalogoExamenService;

    public CatalogoExamenController(
            CatalogoExamenService catalogoExamenService
    ) {
        this.catalogoExamenService =
                catalogoExamenService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int pagina,
            Model model
    ) {

        CatalogoListado.cargar(model, "examenes", catalogoExamenService.listarTodos(), q, pagina,
                r -> r.getId() + " " + r.getNombre());

        return "admin/examenes-laboratorio/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_CREATE')")
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
            "hasRole('ADMINISTRADOR') and ((#examen.id == null and hasAuthority('CATALOG_CREATE')) or (#examen.id != null and hasAuthority('CATALOG_UPDATE')))"
    )
    public String guardar(
            @ModelAttribute("examen")
            CatalogoExamen examen,
            org.springframework.validation.BindingResult errores,

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
            if (errores.hasErrors()) throw new IllegalArgumentException("Revise el formato de los campos ingresados.");

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            boolean nuevo =
                    examen.getId() == null;

            examen = catalogoExamenService
                    .guardar(
                            examen,
                            laboratorioId,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            nuevo
                                    ? "El registro " + examen.getNombre() + " ha sido creado exitosamente."
                                    : "El registro " + examen.getNombre() + " ha sido actualizado correctamente."
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_DISABLE')")
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
                            "El registro " + catalogoExamenService.buscarPorId(id).getNombre() + " ha sido eliminado correctamente."
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
                            "El registro " + catalogoExamenService.buscarPorId(id).getNombre() + " ha sido actualizado correctamente."
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