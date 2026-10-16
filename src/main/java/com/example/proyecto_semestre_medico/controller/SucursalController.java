package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.service.SucursalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/sucursales")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class SucursalController {

    private final SucursalService sucursalService;

    public SucursalController(
            SucursalService sucursalService
    ) {
        this.sucursalService = sucursalService;
    }

    @GetMapping
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "sucursales",
                sucursalService.listarTodas()
        );

        return "admin/sucursales/lista";
    }

    @GetMapping("/nueva")
    public String nueva(
            Model model
    ) {

        model.addAttribute(
                "sucursal",
                new Sucursal()
        );

        return "admin/sucursales/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "sucursal",
                sucursalService.buscarPorId(id)
        );

        return "admin/sucursales/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("sucursal")
            Sucursal sucursal,

            Principal principal,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            boolean nueva =
                    sucursal.getId() == null;

            sucursalService.guardar(
                    sucursal,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    nueva
                            ? "Sucursal creada correctamente."
                            : "Sucursal actualizada correctamente."
            );

            return "redirect:/admin/sucursales";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/sucursales/formulario";
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

            sucursalService.desactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Sucursal desactivada correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/sucursales";
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

            sucursalService.reactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Sucursal reactivada correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/sucursales";
    }
}