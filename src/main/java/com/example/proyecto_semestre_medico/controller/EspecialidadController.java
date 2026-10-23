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

    @InitBinder("especialidad")
    public void camposEditables(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "descripcion");
    }

    private final EspecialidadService
            especialidadService;

    public EspecialidadController(
            EspecialidadService especialidadService
    ) {
        this.especialidadService =
                especialidadService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int pagina,
            Model model
    ) {

        CatalogoListado.cargar(model, "especialidades", especialidadService.listarTodas(), q, pagina,
                r -> r.getId() + " " + r.getNombre());

        return "admin/especialidades/lista";
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_CREATE')")
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
            "hasRole('ADMINISTRADOR') and ((#especialidad.id == null and hasAuthority('CATALOG_CREATE')) or (#especialidad.id != null and hasAuthority('CATALOG_UPDATE')))"
    )
    public String guardar(
            @ModelAttribute("especialidad")
            Especialidad especialidad,
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

            boolean nueva =
                    especialidad.getId() == null;

            especialidad = especialidadService
                    .guardar(
                            especialidad,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            nueva
                                    ? "El registro " + especialidad.getNombre() + " ha sido creado exitosamente."
                                    : "El registro " + especialidad.getNombre() + " ha sido actualizado correctamente."
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_DISABLE')")
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
                            "El registro " + especialidadService.buscarPorId(id).getNombre() + " ha sido eliminado correctamente."
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
                            "El registro " + especialidadService.buscarPorId(id).getNombre() + " ha sido actualizado correctamente."
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