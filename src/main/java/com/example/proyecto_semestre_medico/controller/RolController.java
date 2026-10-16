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
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolController {

    private final RolService rolService;

    public RolController(
            RolService rolService
    ) {
        this.rolService = rolService;
    }

    @GetMapping
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "roles",
                rolService.listarTodos()
        );

        return "admin/roles/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(
            Model model
    ) {

        model.addAttribute(
                "rol",
                new Rol()
        );

        return "admin/roles/formulario";
    }

    @GetMapping("/editar/{id}")
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
    public String guardar(
            @ModelAttribute("rol")
            Rol rol,

            Principal principal,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            boolean nuevo =
                    rol.getId() == null;

            rolService.guardar(
                    rol,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    nuevo
                            ? "Rol creado correctamente."
                            : "Rol actualizado correctamente."
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
                    "Rol desactivado correctamente."
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
                    "Rol reactivado correctamente."
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