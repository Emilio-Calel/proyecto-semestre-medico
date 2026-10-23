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

    @InitBinder("laboratorio")
    public void camposEditables(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "descripcion");
    }

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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int pagina,
            Model model
    ) {

        CatalogoListado.cargar(model, "laboratorios", laboratorioCatalogoService.listarTodos(), q, pagina,
                r -> r.getId() + " " + r.getNombre());

        return "admin/laboratorios/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_CREATE')")
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
            "hasRole('ADMINISTRADOR') and ((#laboratorio.id == null and hasAuthority('CATALOG_CREATE')) or (#laboratorio.id != null and hasAuthority('CATALOG_UPDATE')))"
    )
    public String guardar(
            @ModelAttribute("laboratorio")
            Laboratorio laboratorio,
            org.springframework.validation.BindingResult errores,

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
                    laboratorio.getId() == null;

            laboratorio = laboratorioCatalogoService
                    .guardar(
                            laboratorio,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            nuevo
                                    ? "El registro " + laboratorio.getNombre() + " ha sido creado exitosamente."
                                    : "El registro " + laboratorio.getNombre() + " ha sido actualizado correctamente."
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

            laboratorioCatalogoService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "El registro " + laboratorioCatalogoService.buscarPorId(id).getNombre() + " ha sido eliminado correctamente."
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

            laboratorioCatalogoService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "El registro " + laboratorioCatalogoService.buscarPorId(id).getNombre() + " ha sido actualizado correctamente."
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