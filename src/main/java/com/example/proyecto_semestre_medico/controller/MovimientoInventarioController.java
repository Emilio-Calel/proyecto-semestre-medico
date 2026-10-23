package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.MovimientoInventarioDTO;
import com.example.proyecto_semestre_medico.model.InventarioMedicamento;
import com.example.proyecto_semestre_medico.model.TipoMovimientoInventario;
import com.example.proyecto_semestre_medico.service.InventarioMedicamentoService;
import com.example.proyecto_semestre_medico.service.MovimientoInventarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/farmacia/inventario")
@PreAuthorize(
        "hasRole('FARMACEUTICO') or (hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW'))"
)
public class MovimientoInventarioController {

    private final MovimientoInventarioService
            movimientoInventarioService;

    private final InventarioMedicamentoService
            inventarioMedicamentoService;

    public MovimientoInventarioController(
            MovimientoInventarioService movimientoInventarioService,
            InventarioMedicamentoService inventarioMedicamentoService
    ) {
        this.movimientoInventarioService =
                movimientoInventarioService;

        this.inventarioMedicamentoService =
                inventarioMedicamentoService;
    }

    @GetMapping
    public String listar(
            @RequestParam(
                    name = "inventarioId",
                    required = false
            )
            Long inventarioId,

            @RequestParam(
                    name = "tipo",
                    required = false
            )
            String tipo,

            @RequestParam(
                    name = "estado",
                    required = false
            )
            String estado,

            Model model
    ) {

        model.addAttribute(
                "movimientos",
                movimientoInventarioService
                        .listarFiltrados(
                                inventarioId,
                                tipo,
                                estado
                        )
        );

        model.addAttribute(
                "inventarios",
                inventarioMedicamentoService
                        .listarTodos()
        );

        model.addAttribute(
                "tiposMovimiento",
                TipoMovimientoInventario.values()
        );

        model.addAttribute(
                "inventarioSeleccionado",
                inventarioId
        );

        model.addAttribute(
                "tipoSeleccionado",
                tipo
        );

        model.addAttribute(
                "estadoSeleccionado",
                estado
        );

        return "farmacia/inventario/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('FARMACEUTICO') or (hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE'))")
    public String nuevo(
            @RequestParam(
                    name = "inventarioId",
                    required = false
            )
            Long inventarioId,
            Model model
    ) {

        MovimientoInventarioDTO dto =
                new MovimientoInventarioDTO();

        if (inventarioId != null) {
            dto.setInventarioId(
                    inventarioId
            );
        }

        cargarFormulario(
                model,
                dto
        );

        return "farmacia/inventario/nuevo";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('FARMACEUTICO') or (hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE'))")
    public String guardar(
            @ModelAttribute("movimiento")
            MovimientoInventarioDTO dto,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            movimientoInventarioService
                    .registrarMovimiento(
                            dto,
                            nombreUsuario
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Movimiento registrado correctamente."
            );

            return "redirect:/farmacia/inventario";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            cargarFormulario(
                    model,
                    dto
            );

            return "farmacia/inventario/nuevo";
        }
    }

    @PostMapping("/desactivar/{id}")
    @PreAuthorize("hasRole('FARMACEUTICO') or (hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_DISABLE'))")
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

            movimientoInventarioService
                    .desactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Movimiento desactivado correctamente. El stock no fue modificado."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/farmacia/inventario";
    }

    @PostMapping("/reactivar/{id}")
    @PreAuthorize("hasRole('FARMACEUTICO') or (hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_UPDATE'))")
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

            movimientoInventarioService
                    .reactivar(
                            id,
                            nombreUsuario
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Movimiento reactivado correctamente. El stock no fue modificado."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/farmacia/inventario";
    }

    @GetMapping("/historial/{inventarioId}")
    public String historial(
            @PathVariable Long inventarioId,
            Model model
    ) {

        InventarioMedicamento inventario =
                inventarioMedicamentoService
                        .buscarPorId(inventarioId);

        model.addAttribute(
                "inventario",
                inventario
        );

        model.addAttribute(
                "movimientos",
                movimientoInventarioService
                        .listarPorInventario(
                                inventarioId
                        )
        );

        return "farmacia/inventario/historial";
    }

    @GetMapping("/resumen")
    public String resumen(
            @RequestParam(
                    name = "anio",
                    required = false
            )
            Integer anio,

            @RequestParam(
                    name = "mes",
                    required = false
            )
            Integer mes,

            Model model
    ) {

        LocalDate hoy =
                LocalDate.now();

        int anioSeleccionado =
                anio != null
                        ? anio
                        : hoy.getYear();

        int mesSeleccionado =
                mes != null
                        ? mes
                        : hoy.getMonthValue();

        try {

            model.addAttribute(
                    "resumen",
                    movimientoInventarioService
                            .obtenerResumenMensual(
                                    anioSeleccionado,
                                    mesSeleccionado
                            )
            );

            model.addAttribute(
                    "movimientos",
                    movimientoInventarioService
                            .listarPorMes(
                                    anioSeleccionado,
                                    mesSeleccionado
                            )
            );

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );
        }

        model.addAttribute(
                "anioSeleccionado",
                anioSeleccionado
        );

        model.addAttribute(
                "mesSeleccionado",
                mesSeleccionado
        );

        return "farmacia/inventario/resumen";
    }

    private void cargarFormulario(
            Model model,
            MovimientoInventarioDTO dto
    ) {

        model.addAttribute(
                "movimiento",
                dto
        );

        model.addAttribute(
                "inventarios",
                inventarioMedicamentoService
                        .listarTodos()
                        .stream()
                        .filter(i ->
                                Boolean.TRUE.equals(
                                        i.getActivo()
                                )
                        )
                        .toList()
        );

        model.addAttribute(
                "tiposMovimiento",
                new TipoMovimientoInventario[]{
                        TipoMovimientoInventario.COMPRA,
                        TipoMovimientoInventario.DEVOLUCION,
                        TipoMovimientoInventario.VENTA,
                        TipoMovimientoInventario.RECLAMO,
                        TipoMovimientoInventario.AJUSTE_POSITIVO,
                        TipoMovimientoInventario.AJUSTE_NEGATIVO
                }
        );
    }
}
