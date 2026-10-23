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

public class MedicamentoController {

    @InitBinder("medicamento")
    public void camposEditables(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "descripcion", "precio", "unidad", "esControlado", "stockMinimo");
    }

    private final MedicamentoService medicamentoService;

    public MedicamentoController(
            MedicamentoService medicamentoService
    ) {
        this.medicamentoService =
                medicamentoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int pagina,
            Model model
    ) {

        CatalogoListado.cargar(model, "medicamentos", medicamentoService.listarTodos(), q, pagina,
                r -> r.getId() + " " + r.getNombre());

        return "admin/medicamentos/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_CREATE')")
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
    @PreAuthorize(
            "hasRole('ADMINISTRADOR') and ((#medicamento.id == null and hasAuthority('CATALOG_CREATE')) or (#medicamento.id != null and hasAuthority('CATALOG_UPDATE')))"
    )
    public String guardar(
            @ModelAttribute("medicamento")
            Medicamento medicamento,
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

            boolean esNuevo =
                    medicamento.getId() == null;

            medicamento = medicamentoService.guardar(
                    medicamento,
                    nombreUsuario
            );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            esNuevo
                                    ? "El registro " + medicamento.getNombre() + " ha sido creado exitosamente."
                                    : "El registro " + medicamento.getNombre() + " ha sido actualizado correctamente."
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

            medicamentoService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "El registro " + medicamentoService.buscarPorId(id).getNombre() + " ha sido eliminado correctamente."
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

            medicamentoService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "El registro " + medicamentoService.buscarPorId(id).getNombre() + " ha sido actualizado correctamente."
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
