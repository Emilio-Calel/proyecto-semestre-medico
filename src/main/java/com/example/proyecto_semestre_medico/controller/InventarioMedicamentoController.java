package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.service.InventarioMedicamentoService;
import com.example.proyecto_semestre_medico.service.MedicamentoService;
import com.example.proyecto_semestre_medico.service.SucursalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/inventario-medicamentos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class InventarioMedicamentoController {

    private final InventarioMedicamentoService
            inventarioMedicamentoService;

    private final MedicamentoService
            medicamentoService;

    private final SucursalService
            sucursalService;

    public InventarioMedicamentoController(
            InventarioMedicamentoService inventarioMedicamentoService,
            MedicamentoService medicamentoService,
            SucursalService sucursalService
    ) {
        this.inventarioMedicamentoService =
                inventarioMedicamentoService;

        this.medicamentoService =
                medicamentoService;

        this.sucursalService =
                sucursalService;
    }

    @GetMapping
    public String listar(
            Model model
    ) {

        model.addAttribute(
                "inventarios",
                inventarioMedicamentoService
                        .listarTodos()
        );

        return "admin/inventario-medicamentos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(
            Model model
    ) {

        cargarCatalogos(model);

        return "admin/inventario-medicamentos/nuevo";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(
                    name = "medicamentoId",
                    required = false
            )
            Long medicamentoId,

            @RequestParam(
                    name = "sucursalId",
                    required = false
            )
            Long sucursalId,

            @RequestParam(
                    name = "stockActual",
                    required = false
            )
            Integer stockActual,

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

            inventarioMedicamentoService
                    .guardar(
                            medicamentoId,
                            sucursalId,
                            stockActual,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Inventario registrado correctamente."
                    );

            return "redirect:/admin/inventario-medicamentos";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "medicamentoSeleccionado",
                    medicamentoId
            );

            model.addAttribute(
                    "sucursalSeleccionada",
                    sucursalId
            );

            model.addAttribute(
                    "stockIngresado",
                    stockActual
            );

            cargarCatalogos(model);

            return "admin/inventario-medicamentos/nuevo";
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

            inventarioMedicamentoService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Inventario desactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/inventario-medicamentos";
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

            inventarioMedicamentoService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Inventario reactivado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/admin/inventario-medicamentos";
    }

    private void cargarCatalogos(
            Model model
    ) {

        model.addAttribute(
                "medicamentos",
                medicamentoService
                        .listarActivos()
        );

        model.addAttribute(
                "sucursales",
                sucursalService
                        .listarActivas()
        );
    }
}