package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.service.EstadoCitaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/estados-cita")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EstadoCitaController {

    private final EstadoCitaService estadoCitaService;

    public EstadoCitaController(
            EstadoCitaService estadoCitaService
    ) {
        this.estadoCitaService = estadoCitaService;
    }

    @GetMapping
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "estados",
                estadoCitaService.listarTodos()
        );

        return "admin/estados-cita/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(
            Model model
    ) {

        model.addAttribute(
                "estado",
                new EstadoCita()
        );

        return "admin/estados-cita/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "estado",
                estadoCitaService.buscarPorId(id)
        );

        return "admin/estados-cita/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("estado")
            EstadoCita estado,

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
                    estado.getId() == null;

            estadoCitaService.guardar(
                    estado,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    nuevo
                            ? "Estado de cita creado correctamente."
                            : "Estado de cita actualizado correctamente."
            );

            return "redirect:/admin/estados-cita";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/estados-cita/formulario";
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

            estadoCitaService.desactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Estado de cita desactivado correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/estados-cita";
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

            estadoCitaService.reactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Estado de cita reactivado correctamente."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/estados-cita";
    }
}