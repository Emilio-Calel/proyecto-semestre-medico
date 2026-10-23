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

    @InitBinder("movimiento")
    public void limitarCampos(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("inventarioId", "tipoMovimiento", "cantidad", "costoUnitario", "referencia", "motivo");
    }

    @ModelAttribute
    public void validarUsuarioActivo() { movimientoInventarioService.validarAcceso(); }

    @GetMapping
    public String listar(@ModelAttribute("filtro") com.example.proyecto_semestre_medico.dto.FiltroMovimientoInventarioDTO filtro,
            org.springframework.validation.BindingResult errores,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        org.springframework.data.domain.Page<com.example.proyecto_semestre_medico.model.MovimientoInventarioMedicamento> resultado;
        try {
            if (errores.hasErrors()) throw new IllegalArgumentException("Revise las fechas y los filtros seleccionados.");
            resultado = movimientoInventarioService.buscar(filtro, pagina);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage()); resultado = org.springframework.data.domain.Page.empty();
        }
        model.addAttribute("pagina", resultado);
        model.addAttribute("movimientos", resultado.getContent());
        model.addAttribute("inventarios", inventarioMedicamentoService.listarTodos());
        model.addAttribute("tiposMovimiento", TipoMovimientoInventario.values());
        model.addAttribute("inventarioSeleccionado", filtro.getInventarioId());
        model.addAttribute("tipoSeleccionado", filtro.getTipo());
        model.addAttribute("estadoSeleccionado", filtro.getEstado());
        return "farmacia/inventario/lista";
    }

    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("movimiento", movimientoInventarioService.buscarPorId(id));
        return "farmacia/inventario/detalle";
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
            org.springframework.validation.BindingResult errores,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (errores.hasErrors()) {
            model.addAttribute("error", errores.hasFieldErrors("cantidad")
                    ? "La cantidad debe ser un número entero positivo."
                    : errores.hasFieldErrors("tipoMovimiento") ? "Debe seleccionar el tipo de movimiento."
                    : "Revise el inventario y el costo unitario ingresados.");
            cargarFormulario(model, dto);
            return "farmacia/inventario/nuevo";
        }
        try {

            String nombreUsuario =
                    principal != null
                            ? principal.getName()
                            : null;

            var registrado = movimientoInventarioService
                    .registrarMovimiento(
                            dto,
                            nombreUsuario
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Movimiento registrado correctamente."
            );

            Integer minimo = registrado.getInventario().getMedicamento().getStockMinimo();
            if (minimo != null && registrado.getStockNuevo() < minimo)
                redirectAttributes.addFlashAttribute("alertaStock", registrado.getInventario().getMedicamento().getNombre()
                        + ": Stock bajo — disponible: " + registrado.getStockNuevo() + " (mínimo: " + minimo + ")");
            return "redirect:/farmacia/inventario";

        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            model.addAttribute("error", "El inventario cambió durante la operación. Revise el stock e intente nuevamente.");
            cargarFormulario(model, dto);
            return "farmacia/inventario/nuevo";
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

            var resumen = movimientoInventarioService.obtenerResumenMensual(anioSeleccionado, mesSeleccionado);
            model.addAttribute("resumen", resumen);
            model.addAttribute("movimientos", resumen.getMovimientos());

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
                                ) && Boolean.TRUE.equals(i.getMedicamento().getActivo())
                                  && Boolean.TRUE.equals(i.getSucursal().getActivo())
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
