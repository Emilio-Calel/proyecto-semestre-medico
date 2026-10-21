package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.service.RecepcionService;
import com.example.proyecto_semestre_medico.dto.ResultadoLlegadaRecepcionDTO;
import com.example.proyecto_semestre_medico.dto.CitaRecepcionDTO;
import com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO;
import com.example.proyecto_semestre_medico.dto.EmergenciaRecepcionDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;

@Controller
@RequestMapping("/recepcion")
@PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
public class RecepcionController {

    private final RecepcionService recepcionService;

    public RecepcionController(
            RecepcionService recepcionService
    ) {
        this.recepcionService =
                recepcionService;
    }

    @GetMapping
    public String inicio(
            @RequestParam(
                    required = false
            )
            String criterio,

            @RequestParam(
                    required = false,
                    defaultValue = "CITA"
            )
            String tipoBusqueda,

            Model model
    ) {

        model.addAttribute(
                "criterio",
                criterio
        );

        model.addAttribute(
                "tipoBusqueda",
                tipoBusqueda
        );

        if (criterio != null) {

            if (criterio.isBlank()) {

                model.addAttribute(
                        "error",
                        "Debe ingresar un número de cita o DPI para buscar."
                );

                return "recepcion/index";
            }

            if ("DPI".equalsIgnoreCase(
                    tipoBusqueda
            )) {

                String dpi =
                        criterio.trim();

                if (!dpi.matches("^[0-9]+$")) {

                    model.addAttribute(
                            "error",
                            "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales."
                    );

                    return "recepcion/index";
                }

                if (dpi.length() != 13) {

                    model.addAttribute(
                            "error",
                            "El DPI debe contener exactamente 13 dígitos. Usted ingresó "
                                    + dpi.length()
                                    + " dígitos."
                    );

                    return "recepcion/index";
                }
            }

            var resultados =
                    recepcionService.buscar(
                            criterio,
                            tipoBusqueda
                    );

            model.addAttribute(
                    "resultados",
                    resultados
            );

            if (resultados.isEmpty()) {

                /*
                 * Si la búsqueda fue por DPI,
                 * distinguimos FA03 y FA04.
                 */
                if ("DPI".equalsIgnoreCase(
                        tipoBusqueda
                )) {

                    var paciente =
                            recepcionService
                                    .buscarNombrePacientePorDpi(
                                            criterio
                                    );

                    if (paciente.isEmpty()) {

                        model.addAttribute(
                                "fa03PacienteNoRegistrado",
                                true
                        );

                        model.addAttribute(
                                "dpiBuscado",
                                criterio.trim()
                        );

                        model.addAttribute(
                                "error",
                                "No se encontró ningún paciente con ese DPI."
                        );

                        model.addAttribute(
                                "subtexto",
                                "Es necesario registrar al paciente antes de continuar."
                        );

                    } else {

                        model.addAttribute(
                                "fa04PacienteSinCitas",
                                true
                        );

                        model.addAttribute(
                                "nombrePaciente",
                                paciente.get()
                        );

                        recepcionService
                                .buscarIdPacientePorDpi(
                                        criterio
                                )
                                .ifPresent(
                                        pacienteId ->
                                                model.addAttribute(
                                                        "pacienteId",
                                                        pacienteId
                                                )
                                );

                        model.addAttribute(
                                "error",
                                "El paciente "
                                        + paciente.get()
                                        + " está registrado pero no tiene citas activas."
                        );

                        model.addAttribute(
                                "subtexto",
                                "Puede crear una nueva cita para este paciente."
                        );
                    }

                } else {

                    model.addAttribute(
                            "error",
                            "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."
                    );
                }
            }
        }

        return "recepcion/index";
    }

    @PostMapping("/citas/{id}/llegada")
    @PreAuthorize("hasAuthority('APPOINTMENT_CHECK_IN')")
    public String registrarLlegada(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            ResultadoLlegadaRecepcionDTO resultado =
                    recepcionService.registrarLlegada(
                            id,
                            authentication.getName()
                    );

            if (resultado.isEmergencia()) {

                redirectAttributes.addFlashAttribute(
                        "mensaje",
                        "Paciente "
                                + resultado.getNombrePaciente()
                                + " registrado con prioridad de EMERGENCIA. "
                                + "El paciente debe pasar directamente a toma de signos vitales."
                );

            } else {

                redirectAttributes.addFlashAttribute(
                        "mensaje",
                        "La llegada del paciente "
                                + resultado.getNombrePaciente()
                                + " ha sido registrada exitosamente. "
                                + "El paciente debe pasar a la sala de espera."
                );
            }

            redirectAttributes.addAttribute(
                    "tipoBusqueda",
                    "CITA"
            );

            redirectAttributes.addAttribute(
                    "criterio",
                    resultado.getNumeroCita()
            );

            return "redirect:/recepcion";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {


            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/recepcion";


        } catch (ObjectOptimisticLockingFailureException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                "Error al registrar la llegada"
        );

        return "redirect:/recepcion";
    }


    }

    @GetMapping("/citas/{id}/signos-urgente")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public String signosUrgente(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        redirectAttributes.addFlashAttribute(
                "mensaje",
                "Paciente de EMERGENCIA enviado a toma de signos vitales."
        );

        return "redirect:/recepcion";
    }

    @GetMapping("/citas/{id}/reasignar")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public String mostrarReasignacion(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            CitaRecepcionDTO cita =
                    recepcionService
                            .obtenerParaReasignacion(
                                    id
                            );

            List<MedicoOpcionDTO> medicos =
                    recepcionService
                            .listarMedicosDisponiblesParaReasignacion(
                                    id
                            );

            model.addAttribute(
                    "cita",
                    cita
            );

            model.addAttribute(
                    "medicos",
                    medicos
            );

            return "recepcion/reasignar-medico";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            return "redirect:/recepcion";
        }
    }

    @PostMapping("/citas/{id}/reasignar")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public String confirmarReasignacion(
            @PathVariable Long id,

            @RequestParam Long medicoId,

            @RequestParam(
                    required = false
            )
            String motivo,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            recepcionService
                    .reasignarMedico(
                            id,
                            medicoId,
                            motivo,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Médico reasignado correctamente"
                    );

            CitaRecepcionDTO cita =
                    recepcionService
                            .obtenerParaReasignacion(
                                    id
                            );

            redirectAttributes
                    .addAttribute(
                            "tipoBusqueda",
                            "CITA"
                    );

            redirectAttributes
                    .addAttribute(
                            "criterio",
                            cita.getNumeroCita()
                    );

            return "redirect:/recepcion";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            return "redirect:/recepcion/citas/"
                    + id
                    + "/reasignar";
        }
    }

    @GetMapping("/emergencia")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public String mostrarEmergencia(
            Model model
    ) {

        if (!model.containsAttribute(
                "emergenciaForm"
        )) {

            model.addAttribute(
                    "emergenciaForm",
                    new EmergenciaRecepcionDTO()
            );
        }

        return "recepcion/emergencia";
    }

    @PostMapping("/emergencia")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public String registrarEmergencia(
            @ModelAttribute("emergenciaForm")
            EmergenciaRecepcionDTO dto,

            Authentication authentication,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        try {

            CitaRecepcionDTO cita =
                    recepcionService
                            .registrarEmergencia(
                                    dto,
                                    authentication.getName()
                            );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Paciente "
                                    + cita.getPaciente()
                                    + " registrado con prioridad de EMERGENCIA. "
                                    + "El paciente debe pasar directamente a toma de signos vitales."
                    );

            redirectAttributes
                    .addAttribute(
                            "tipoBusqueda",
                            "CITA"
                    );

            redirectAttributes
                    .addAttribute(
                            "criterio",
                            cita.getNumeroCita()
                    );

            return "redirect:/recepcion";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "emergenciaForm",
                    dto
            );

            return "recepcion/emergencia";
        }
    }
}