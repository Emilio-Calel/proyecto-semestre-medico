package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.Medicamento;
import com.example.proyecto_semestre_medico.service.MedicamentoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/medicamentos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;

    public MedicamentoController(
            MedicamentoService medicamentoService
    ) {
        this.medicamentoService =
                medicamentoService;
    }

    @GetMapping
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "medicamentos",
                medicamentoService
                        .listarTodos()
        );

        return "admin/medicamentos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(
            Model model
    ) {

        model.addAttribute(
                "medicamento",
                new Medicamento()
        );

        return "admin/medicamentos/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "medicamento",
                medicamentoService
                        .buscarPorId(id)
        );

        return "admin/medicamentos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("medicamento")
            Medicamento medicamento,

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

            boolean esNuevo =
                    medicamento.getId() == null;

            medicamentoService.guardar(
                    medicamento,
                    nombreUsuario
            );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            esNuevo
                                    ? "Medicamento creado correctamente."
                                    : "Medicamento actualizado correctamente."
                    );

            return "redirect:/admin/medicamentos";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/medicamentos/formulario";
        }
    }

    @PostMapping("/desactivar/{id}")
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

            medicamentoService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Medicamento desactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/medicamentos";
    }

    @PostMapping("/reactivar/{id}")
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

            medicamentoService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Medicamento reactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/medicamentos";
    }
}
