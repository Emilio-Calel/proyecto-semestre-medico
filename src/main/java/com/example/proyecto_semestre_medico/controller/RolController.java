package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.service.RolService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/roles")

public class RolController {

    @InitBinder("rol")
    public void camposEditables(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "codigo", "nombre", "descripcion");
    }

    private final RolService rolService;

    public RolController(
            RolService rolService
    ) {
        this.rolService = rolService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String listar(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        CatalogoListado.cargar(model, "roles", rolService.listarTodos(), q, pagina,
                r -> r.getId() + " " + r.getNombre());

        return "admin/roles/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_CREATE')")
    public String nuevo(Model model) {

        model.addAttribute(
                "rol",
                new Rol()
        );

        return "admin/roles/formulario";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "rol",
                rolService.buscarPorId(id)
        );

        return "admin/roles/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize(
            "hasRole('ADMINISTRADOR') and ((#rol.id == null and hasAuthority('CATALOG_CREATE')) or (#rol.id != null and hasAuthority('CATALOG_UPDATE')))"
    )
    public String guardar(
            @ModelAttribute("rol")
            Rol rol,
            org.springframework.validation.BindingResult errores,

            Principal principal,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {
            if (errores.hasErrors()) throw new IllegalArgumentException("Revise el formato de los campos ingresados.");

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            boolean nuevo =
                    rol.getId() == null;

            rol = rolService.guardar(
                    rol,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    nuevo
                            ? "El registro " + rol.getNombre() + " ha sido creado exitosamente."
                                    : "El registro " + rol.getNombre() + " ha sido actualizado correctamente."
            );

            return "redirect:/admin/roles";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/roles/formulario";
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

            rolService.desactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El registro " + rolService.buscarPorId(id).getNombre() + " ha sido eliminado correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/roles";
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

            rolService.reactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El registro " + rolService.buscarPorId(id).getNombre() + " ha sido actualizado correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/roles";
    }
}