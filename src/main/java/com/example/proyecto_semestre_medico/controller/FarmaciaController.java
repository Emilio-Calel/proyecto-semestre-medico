package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.DespachoDetalleDTO;
import com.example.proyecto_semestre_medico.dto.DespachoMedicamentoDTO;
import com.example.proyecto_semestre_medico.dto.MedicamentoRecetaFarmaciaDTO;
import com.example.proyecto_semestre_medico.model.DespachoMedicamento;
import com.example.proyecto_semestre_medico.model.EstadoReceta;
import com.example.proyecto_semestre_medico.model.Receta;
import com.example.proyecto_semestre_medico.repository.MedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
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

    public FarmaciaController(
            RecetaService recetaService,
            DespachoMedicamentoService despachoMedicamentoService,
            SucursalRepository sucursalRepository,
            MedicamentoRepository medicamentoRepository
    ) {

        this.recetaService =
                recetaService;

        this.despachoMedicamentoService =
                despachoMedicamentoService;

        this.sucursalRepository =
                sucursalRepository;

        this.medicamentoRepository =
                medicamentoRepository;
    }

    /*
     * =========================================================
     * LISTADO DE RECETAS
     * =========================================================
     */

    @GetMapping
    public String listarRecetas(
            @RequestParam(
                    name = "estado",
                    required = false
            )
            String estado,

            Model model
    ) {

        List<Receta> recetas =
                recetaService
                        .listarTodas();

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

            if (receta.getEstado()
                    == EstadoReceta.VENCIDA) {

                throw new IllegalArgumentException(
                        "La receta está vencida y no puede ser despachada."
                );
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
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            DespachoMedicamento despacho =
                    despachoMedicamentoService
                            .registrarDespacho(
                                    dto,
                                    authentication.getName()
                            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Despacho registrado correctamente. "
                            + "Número de despacho: "
                            + despacho.getId()
                            + ". Monto total: Q "
                            + despacho.getMontoTotal()
                            + "."
            );

            return "redirect:/farmacia/recetas/"
                    + dto.getRecetaId();

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/farmacia/recetas/"
                    + dto.getRecetaId()
                    + "/despachar?sucursalId="
                    + dto.getSucursalId();
        }
    }
}