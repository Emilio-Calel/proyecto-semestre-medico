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

public class SucursalController {

    @InitBinder("sucursal")
    public void camposEditables(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "descripcion", "telefono", "direccion");
    }

    private final SucursalService sucursalService;

    public SucursalController(
            SucursalService sucursalService
    ) {
        this.sucursalService = sucursalService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int pagina,
            Model model
    ) {

        CatalogoListado.cargar(model, "sucursales", sucursalService.listarTodas(), q, pagina,
                r -> r.getId() + " " + r.getNombre());

        return "admin/sucursales/lista";
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_CREATE')")
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
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE')")
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
    @PreAuthorize(
            "hasRole('ADMINISTRADOR') and ((#sucursal.id == null and hasAuthority('CATALOG_CREATE')) or (#sucursal.id != null and hasAuthority('CATALOG_UPDATE')))"
    )
    public String guardar(
            @ModelAttribute("sucursal")
            Sucursal sucursal,
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

            boolean nueva =
                    sucursal.getId() == null;

            sucursal = sucursalService.guardar(
                    sucursal,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    nueva
                            ? "El registro " + sucursal.getNombre() + " ha sido creado exitosamente."
                                    : "El registro " + sucursal.getNombre() + " ha sido actualizado correctamente."
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

            sucursalService.desactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El registro " + sucursalService.buscarPorId(id).getNombre() + " ha sido eliminado correctamente."
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

            sucursalService.reactivar(
                    id,
                    nombreUsuario
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El registro " + sucursalService.buscarPorId(id).getNombre() + " ha sido actualizado correctamente."
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