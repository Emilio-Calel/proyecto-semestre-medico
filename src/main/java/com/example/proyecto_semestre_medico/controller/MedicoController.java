package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.ConsultaMedicaFormDTO;
import com.example.proyecto_semestre_medico.service.ConsultaMedicaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.OrdenLaboratorio;
import com.example.proyecto_semestre_medico.service.OrdenLaboratorioService;
import com.example.proyecto_semestre_medico.dto.RecetaFormDTO;
import com.example.proyecto_semestre_medico.model.Receta;
import com.example.proyecto_semestre_medico.service.RecetaService;

@Controller
@RequestMapping("/medico")
@PreAuthorize("hasRole('MEDICO')")
public class MedicoController {

    private final ConsultaMedicaService consultaMedicaService;
    private final OrdenLaboratorioService ordenLaboratorioService;
    private final RecetaService recetaService;

    public MedicoController(
            ConsultaMedicaService consultaMedicaService,
            OrdenLaboratorioService ordenLaboratorioService,
            RecetaService recetaService
    ) {
        this.consultaMedicaService =
                consultaMedicaService;

        this.ordenLaboratorioService =
                ordenLaboratorioService;

        this.recetaService =
                recetaService;
    }

    /*
     * =========================================================
     * PANEL PRINCIPAL DEL MÉDICO
     * =========================================================
     */

    @GetMapping
    public String panel(
            Authentication authentication,
            Model model
    ) {

        String nombreUsuario =
                authentication.getName();

        model.addAttribute(
                "enEspera",
                consultaMedicaService
                        .listarEnEspera(nombreUsuario)
        );

        model.addAttribute(
                "enConsulta",
                consultaMedicaService
                        .listarEnConsulta(nombreUsuario)
        );

        model.addAttribute(
                "evaluados",
                consultaMedicaService
                        .listarPendientesCierre(nombreUsuario)
        );

        return "medico/panel";
    }

    /*
     * =========================================================
     * INICIAR CONSULTA
     * =========================================================
     */

    @PostMapping("/consulta/iniciar/{citaId}")
    public String iniciarConsulta(
            @PathVariable Long citaId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            consultaMedicaService.iniciarConsulta(
                    citaId,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Consulta médica iniciada correctamente."
            );

            /*
             * Estos datos servirán para que el HTML
             * realice posteriormente el anuncio TTS.
             */
            redirectAttributes.addFlashAttribute(
                    "ttsCitaId",
                    citaId
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/medico";
    }

    /*
     * =========================================================
     * ABRIR / COMPLETAR CONSULTA
     * =========================================================
     */

    @GetMapping("/consulta/{citaId}")
    public String verConsulta(
            @PathVariable Long citaId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            ConsultaMedicaFormDTO consulta =
                    consultaMedicaService
                            .obtenerConsulta(
                                    citaId,
                                    authentication.getName()
                            );

            model.addAttribute(
                    "consultaForm",
                    consulta
            );

            return "medico/consulta";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/medico";
        }
    }

    /*
     * =========================================================
     * GUARDAR CONSULTA
     * =========================================================
     */

    @PostMapping("/consulta/guardar")
    public String guardarConsulta(
            @ModelAttribute("consultaForm")
            ConsultaMedicaFormDTO dto,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            consultaMedicaService.guardarConsulta(
                    dto,
                    authentication.getName()
            );

            if ("FINALIZADA".equalsIgnoreCase(
                    dto.getEstado()
            )) {

                redirectAttributes.addFlashAttribute(
                        "mensaje",
                        "La consulta ha sido finalizada exitosamente. "
                                + "El paciente puede proceder a las siguientes "
                                + "indicaciones médicas."
                );

                return "redirect:/medico";
            }

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Consulta guardada correctamente."
            );

            return "redirect:/medico/consulta/"
                    + dto.getCitaId();

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "consultaForm",
                    dto
            );

            return "medico/consulta";
        }
    }

    /*
     * =========================================================
     * PACIENTE NO ASISTIÓ
     * =========================================================
     */

    @PostMapping("/no-asistio/{citaId}")
    public String marcarNoAsistio(
            @PathVariable Long citaId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            consultaMedicaService.marcarNoAsistio(
                    citaId,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Cita marcada como No Asistió."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/medico";
    }

    /*
     * =========================================================
     * FINALIZAR ATENCIÓN
     * =========================================================
     */

    @PostMapping("/finalizar/{citaId}")
    public String finalizarAtencion(
            @PathVariable Long citaId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            consultaMedicaService.finalizarAtencion(
                    citaId,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Atención finalizada correctamente para la cita."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/medico";
    }
    @GetMapping("/orden-laboratorio/{citaId}")
    public String nuevaOrdenLaboratorio(
            @PathVariable Long citaId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            /*
             * Reutilizamos obtenerConsulta porque además
             * valida que la cita pertenezca al médico.
             */
            ConsultaMedicaFormDTO consulta =
                    consultaMedicaService
                            .obtenerConsulta(
                                    citaId,
                                    authentication.getName()
                            );

            if (!"FINALIZADA".equals(
                    consulta.getEstado()
            )) {

                throw new IllegalArgumentException(
                        "La consulta debe estar finalizada antes de generar una orden."
                );
            }

            OrdenLaboratorioFormDTO ordenForm =
                    new OrdenLaboratorioFormDTO();

            ordenForm.setCitaId(
                    citaId
            );

            model.addAttribute(
                    "ordenForm",
                    ordenForm
            );

            model.addAttribute(
                    "consulta",
                    consulta
            );

            model.addAttribute(
                    "examenes",
                    ordenLaboratorioService
                            .listarExamenesActivos()
            );

            return "medico/orden-laboratorio";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/medico";
        }
    }

    @PostMapping("/orden-laboratorio/guardar")
    public String guardarOrdenLaboratorio(
            @ModelAttribute("ordenForm")
            OrdenLaboratorioFormDTO dto,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            OrdenLaboratorio orden =
                    ordenLaboratorioService
                            .crearOrden(
                                    dto,
                                    authentication.getName()
                            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Orden de laboratorio generada exitosamente. "
                            + "Número de orden: "
                            + orden.getNumeroOrden()
                            + "."
            );

            return "redirect:/medico";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "ordenForm",
                    dto
            );

            model.addAttribute(
                    "examenes",
                    ordenLaboratorioService
                            .listarExamenesActivos()
            );

            /*
             * Volvemos a cargar el contexto de la cita.
             */
            try {

                model.addAttribute(
                        "consulta",
                        consultaMedicaService
                                .obtenerConsulta(
                                        dto.getCitaId(),
                                        authentication.getName()
                                )
                );

            } catch (Exception ignored) {
            }

            return "medico/orden-laboratorio";
        }
    }

    @GetMapping("/receta/{citaId}")
    public String nuevaReceta(
            @PathVariable Long citaId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            ConsultaMedicaFormDTO consulta =
                    consultaMedicaService
                            .obtenerConsulta(
                                    citaId,
                                    authentication.getName()
                            );

            if (!"FINALIZADA".equalsIgnoreCase(
                    consulta.getEstado()
            )) {

                throw new IllegalArgumentException(
                        "La consulta debe estar finalizada antes de generar una receta."
                );
            }

            RecetaFormDTO recetaForm =
                    new RecetaFormDTO();

            recetaForm.setCitaId(
                    citaId
            );

            model.addAttribute(
                    "recetaForm",
                    recetaForm
            );

            model.addAttribute(
                    "consulta",
                    consulta
            );

            model.addAttribute(
                    "medicamentos",
                    recetaService
                            .listarMedicamentosActivos()
            );

            return "medico/receta";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/medico";
        }
    }

    @PostMapping("/receta/guardar")
    public String guardarReceta(
            @ModelAttribute("recetaForm")
            RecetaFormDTO dto,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Receta receta =
                    recetaService
                            .crearReceta(
                                    dto,
                                    authentication.getName()
                            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Receta generada correctamente. "
                            + "Número de receta: "
                            + receta.getId()
                            + ". Vigencia: 7 días."
            );

            return "redirect:/medico";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "recetaForm",
                    dto
            );

            model.addAttribute(
                    "medicamentos",
                    recetaService
                            .listarMedicamentosActivos()
            );

            try {

                model.addAttribute(
                        "consulta",
                        consultaMedicaService
                                .obtenerConsulta(
                                        dto.getCitaId(),
                                        authentication.getName()
                                )
                );

            } catch (Exception ignored) {
            }

            return "medico/receta";
        }
    }
}