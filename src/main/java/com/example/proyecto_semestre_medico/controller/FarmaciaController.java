package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.DespachoDetalleDTO;
import com.example.proyecto_semestre_medico.dto.DespachoMedicamentoDTO;
import com.example.proyecto_semestre_medico.dto.MedicamentoRecetaFarmaciaDTO;
import com.example.proyecto_semestre_medico.model.DespachoMedicamento;
import com.example.proyecto_semestre_medico.model.EstadoReceta;
import com.example.proyecto_semestre_medico.model.Receta;
import com.example.proyecto_semestre_medico.repository.MedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.repository.InventarioMedicamentoRepository;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.dao.DataAccessException;
import com.example.proyecto_semestre_medico.service.DespachoMedicamentoService;
import com.example.proyecto_semestre_medico.service.RecetaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/farmacia")
@PreAuthorize(
        "hasRole('FARMACEUTICO') or hasRole('ADMINISTRADOR')"
)
public class FarmaciaController {

    private final RecetaService recetaService;

    private final DespachoMedicamentoService
            despachoMedicamentoService;

    private final SucursalRepository
            sucursalRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final InventarioMedicamentoRepository inventarioRepository;

    public FarmaciaController(
            RecetaService recetaService,
            DespachoMedicamentoService despachoMedicamentoService,
            SucursalRepository sucursalRepository,
            MedicamentoRepository medicamentoRepository,
            InventarioMedicamentoRepository inventarioRepository
    ) {

        this.recetaService =
                recetaService;

        this.despachoMedicamentoService =
                despachoMedicamentoService;

        this.sucursalRepository =
                sucursalRepository;

        this.medicamentoRepository =
                medicamentoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    /*
     * =========================================================
     * LISTADO DE RECETAS
     * =========================================================
     */

    @InitBinder("despachoForm")
    public void camposDespacho(WebDataBinder binder) {
        binder.setAllowedFields("recetaId", "sucursalId", "detalles[*].recetaDetalleId",
                "detalles[*].cantidad", "detalles[*].cantidadDespachadaPrevia",
                "detalles[*].medicamentoDespachadoId", "detalles[*].motivoSustitucion", "detalles[*].sustituir");
    }

    @GetMapping({"", "/recetas/buscar"})
    public String listarRecetas(
            @RequestParam(
                    name = "estado",
                    required = false
            )
            String estado,

            @RequestParam(required = false) Long recetaId,
            @RequestParam(required = false) Long consultaId,

            Model model
    ) {

        List<Receta> recetas;
        try {
            recetas = recetaService.buscarActivasFarmacia(recetaId, consultaId);
        } catch (IllegalArgumentException e) {
            recetas = List.of();
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("recetaId", recetaId);
        model.addAttribute("consultaId", consultaId);

        if (estado != null
                && !estado.isBlank()
                && !"TODOS".equalsIgnoreCase(
                estado
        )) {

            try {

                EstadoReceta estadoReceta =
                        EstadoReceta.valueOf(
                                estado.toUpperCase()
                        );

                recetas =
                        recetas.stream()
                                .filter(r ->
                                        r.getEstado()
                                                == estadoReceta
                                )
                                .toList();

            } catch (IllegalArgumentException ignored) {
            }
        }

        model.addAttribute(
                "recetas",
                recetas
        );

        model.addAttribute(
                "estadoSeleccionado",
                estado
        );

        model.addAttribute(
                "estados",
                EstadoReceta.values()
        );

        return "farmacia/recetas/lista";
    }

    /*
     * =========================================================
     * DETALLE DE RECETA
     * =========================================================
     */

    @GetMapping("/recetas/{id}")
    public String verReceta(
            @PathVariable Long id,
            Model model
    ) {

        Receta receta =
                recetaService
                        .buscarPorId(id);

        model.addAttribute(
                "receta",
                receta
        );

        return "farmacia/recetas/detalle";
    }

    /*
     * =========================================================
     * ABRIR DESPACHO
     * =========================================================
     */

    @GetMapping("/recetas/{id}/despachar")
    public String abrirDespacho(
            @PathVariable Long id,
            @RequestParam(
                    name = "sucursalId",
                    required = false
            )
            Long sucursalId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Receta receta =
                    recetaService
                            .buscarPorId(id);

            if (receta.isVencidaParaDespacho()) {

                throw new IllegalArgumentException(
                        receta.getMensajeVencida()
                );
            }

            if (!Boolean.TRUE.equals(receta.getActivo())) {
                throw new IllegalArgumentException("La receta se encuentra inactiva.");
            }

            if (receta.getEstado()
                    == EstadoReceta.DESPACHADA) {

                throw new IllegalArgumentException(
                        "La receta ya fue despachada completamente."
                );
            }

            if (receta.getEstado()
                    == EstadoReceta.NO_ADQUIRIDA) {

                throw new IllegalArgumentException(
                        "La receta fue marcada como no adquirida."
                );
            }

            model.addAttribute(
                    "receta",
                    receta
            );

            model.addAttribute(
                    "sucursales",
                    sucursalRepository
                            .findByActivoTrueOrderByNombreAsc()
            );

            model.addAttribute(
                    "medicamentos",
                    medicamentoRepository
                            .findByActivoTrueOrderByNombreAsc()
            );

            model.addAttribute(
                    "sucursalId",
                    sucursalId
            );

            if (sucursalId != null) {

                List<MedicamentoRecetaFarmaciaDTO>
                        detalles =
                        despachoMedicamentoService
                                .obtenerDetalleParaDespacho(
                                        id,
                                        sucursalId
                                );

                model.addAttribute(
                        "detalles",
                        detalles
                );

                model.addAttribute("inventarios", inventarioRepository
                        .findBySucursalIdAndActivoTrueOrderByMedicamentoNombreAsc(sucursalId).stream()
                        .collect(java.util.stream.Collectors.toMap(i -> i.getMedicamento().getId(), i -> i)));

                DespachoMedicamentoDTO form =
                        new DespachoMedicamentoDTO();

                form.setRecetaId(
                        id
                );

                form.setSucursalId(
                        sucursalId
                );

                List<DespachoDetalleDTO>
                        detallesForm =
                        new ArrayList<>();

                for (MedicamentoRecetaFarmaciaDTO detalle
                        : detalles) {

                    DespachoDetalleDTO dto =
                            new DespachoDetalleDTO();

                    dto.setRecetaDetalleId(
                            detalle
                                    .getRecetaDetalleId()
                    );

                    dto.setMedicamentoDespachadoId(
                            detalle
                                    .getMedicamentoId()
                    );

                    dto.setCantidad(
                            0
                    );
                    dto.setCantidadDespachadaPrevia(detalle.getCantidadDespachada());

                    detallesForm.add(
                            dto
                    );
                }

                form.setDetalles(
                        detallesForm
                );

                model.addAttribute(
                        "despachoForm",
                        form
                );
            }

            return "farmacia/recetas/despacho";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/farmacia/recetas/"
                    + id;
        }
    }

    /*
     * =========================================================
     * REGISTRAR DESPACHO
     * =========================================================
     */

    @PostMapping("/recetas/despachar")
    public String registrarDespacho(
            @ModelAttribute("despachoForm")
            DespachoMedicamentoDTO dto,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            if (bindingResult.hasErrors()) {
                throw new IllegalArgumentException("Las cantidades e identificadores del despacho deben ser números enteros válidos.");
            }

            DespachoMedicamento despacho =
                    despachoMedicamentoService
                            .registrarDespacho(
                                    dto,
                                    authentication.getName()
                            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Despacho registrado exitosamente. "
                            + despacho.getDetalles().size()
                            + " medicamento(s) despachado(s). Total: Q"
                            + despacho.getMontoTotal().setScale(2).toPlainString()
                            + "."
            );

            redirectAttributes.addFlashAttribute("avisos", despacho.getAvisos());

            return "redirect:/farmacia/recetas/"
                    + dto.getRecetaId() + "/historial";

        } catch (IllegalArgumentException | DataAccessException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e instanceof DataAccessException ? "No fue posible registrar el despacho. Verifique la receta y el inventario antes de reintentar." : e.getMessage()
            );

            if (dto.getRecetaId() == null || dto.getSucursalId() == null) return "redirect:/farmacia";

            return "redirect:/farmacia/recetas/"
                    + dto.getRecetaId()
                    + "/despachar?sucursalId="
                    + dto.getSucursalId();
        }
    }

    /*
     * =========================================================
     * MARCAR RECETA COMO NO ADQUIRIDA
     * =========================================================
     */

    @PostMapping("/recetas/{id}/no-adquirida")
    public String marcarComoNoAdquirida(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Receta receta = recetaService
                    .marcarComoNoAdquirida(
                            id,
                            authentication.getName()
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Se ha registrado que el paciente " + receta.getConsultaMedica().getCita().getPaciente().getNombreCompleto()
                            + " no adquirió los medicamentos recetados en farmacia interna. Receta: " + receta.getId() + "."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/farmacia/recetas/"
                + id;
    }

    /*
     * =========================================================
     * HISTORIAL DE DESPACHOS DE UNA RECETA
     * =========================================================
     */

    @GetMapping("/recetas/{id}/historial")
    public String verHistorialDespachos(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Receta receta =
                    recetaService
                            .buscarPorId(id);

            List<DespachoMedicamento> despachos =
                    despachoMedicamentoService
                            .listarDespachosPorReceta(id);

            model.addAttribute(
                    "receta",
                    receta
            );

            model.addAttribute(
                    "despachos",
                    despachos
            );

            return "farmacia/recetas/historial";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/farmacia";
        }
    }
}
