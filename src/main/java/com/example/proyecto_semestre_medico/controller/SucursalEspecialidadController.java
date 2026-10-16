package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.service.SucursalEspecialidadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/sucursal-especialidad")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class SucursalEspecialidadController {

    private final SucursalEspecialidadService
            sucursalEspecialidadService;

    public SucursalEspecialidadController(
            SucursalEspecialidadService
                    sucursalEspecialidadService
    ) {
        this.sucursalEspecialidadService =
                sucursalEspecialidadService;
    }

    @GetMapping
    public String listar(
            @RequestParam(
                    name = "id",
                    required = false
            )
            Long id,
            Model model
    ) {

        model.addAttribute(
                "asignaciones",
                sucursalEspecialidadService
                        .listar(id)
        );

        model.addAttribute(
                "idFiltro",
                id
        );

        return "admin/sucursal-especialidad/lista";
    }

    @GetMapping("/nueva")
    public String nueva(
            Model model
    ) {

        cargarCatalogos(model);

        return "admin/sucursal-especialidad/nueva";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(
                    name = "sucursalId",
                    required = false
            )
            Long sucursalId,

            @RequestParam(
                    name = "especialidadId",
                    required = false
            )
            Long especialidadId,

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

            sucursalEspecialidadService
                    .asignar(
                            sucursalId,
                            especialidadId,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Especialidad asignada a la sede correctamente."
                    );

            return "redirect:/admin/sucursal-especialidad";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "sucursalSeleccionada",
                    sucursalId
            );

            model.addAttribute(
                    "especialidadSeleccionada",
                    especialidadId
            );

            cargarCatalogos(model);

            return "admin/sucursal-especialidad/nueva";
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

            sucursalEspecialidadService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Asignación eliminada correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/sucursal-especialidad";
    }

    private void cargarCatalogos(
            Model model
    ) {

        model.addAttribute(
                "sucursales",
                sucursalEspecialidadService
                        .listarSucursalesActivas()
        );

        model.addAttribute(
                "especialidades",
                sucursalEspecialidadService
                        .listarEspecialidadesActivas()
        );
    }
}