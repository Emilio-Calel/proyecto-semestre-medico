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

public class EstadoCitaController {

    @InitBinder("estado")
    public void camposEditables(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "codigo", "nombre", "descripcion");
    }

    private final EstadoCitaService estadoCitaService;

    public EstadoCitaController(
            EstadoCitaService estadoCitaService
    ) {
        this.estadoCitaService = estadoCitaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int pagina,
            Model model
    ) {

        CatalogoListado.cargar(model, "estados", estadoCitaService.listarTodos(), q, pagina,
                r -> r.getId() + " " + r.getNombre());

        return "admin/estados-cita/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_CREATE')")
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
    @PreAuthorize(
            "hasRole('ADMINISTRADOR') and ((#estado.id == null and hasAuthority('CATALOG_CREATE')) or (#estado.id != null and hasAuthority('CATALOG_UPDATE')))"
    )
    public String guardar(
            @ModelAttribute("estado")
            EstadoCita estado,
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
                    estado.getId() == null;

            estado = estadoCitaService.guardar(
                    estado,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    nuevo
                            ? "El registro " + estado.getNombre() + " ha sido creado exitosamente."
                                    : "El registro " + estado.getNombre() + " ha sido actualizado correctamente."
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

            estadoCitaService.desactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El registro " + estadoCitaService.buscarPorId(id).getNombre() + " ha sido eliminado correctamente."
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

            estadoCitaService.reactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El registro " + estadoCitaService.buscarPorId(id).getNombre() + " ha sido actualizado correctamente."
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
