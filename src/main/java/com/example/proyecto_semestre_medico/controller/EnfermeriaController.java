package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.SignosVitalesDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.service.SignosVitalesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/enfermeria")
@PreAuthorize("hasAuthority('VITAL_SIGNS_VIEW')")
public class EnfermeriaController {

    private final SignosVitalesService signosVitalesService;

    public EnfermeriaController(
            SignosVitalesService signosVitalesService
    ) {
        this.signosVitalesService = signosVitalesService;
    }

    @GetMapping
    public String inicio(
            Model model
    ) {

        var pacientes =
                signosVitalesService
                        .listarPacientesPresentes();

        var pacientesPresentes =
                pacientes.stream()
                        .filter(cita ->
                                "PACIENTE_PRESENTE"
                                        .equals(
                                                cita.getEstadoCita()
                                                        .getCodigo()
                                        )
                        )
                        .toList();

        var pacientesEnSignos =
                pacientes.stream()
                        .filter(cita ->
                                "SIGNOS_VITALES"
                                        .equals(
                                                cita.getEstadoCita()
                                                        .getCodigo()
                                        )
                        )
                        .toList();

        model.addAttribute(
                "pacientesPresentes",
                pacientesPresentes
        );

        model.addAttribute(
                "pacientesEnSignos",
                pacientesEnSignos
        );

        return "enfermeria/index";
    }

    @PostMapping("/citas/{citaId}/iniciar")
    @PreAuthorize("hasAuthority('VITAL_SIGNS_CREATE')")
    public String iniciarToma(
            @PathVariable Long citaId,
            RedirectAttributes redirectAttributes
    ) {

        try {

            signosVitalesService
                    .iniciarToma(citaId);

            return "redirect:/enfermeria/citas/"
                    + citaId
                    + "/signos";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            return "redirect:/enfermeria";
        }
    }

    @GetMapping("/citas/{citaId}/signos")
    @PreAuthorize("hasAuthority('VITAL_SIGNS_CREATE')")
    public String formulario(
            @PathVariable Long citaId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Cita cita =
                    signosVitalesService
                            .buscarCitaParaSignos(
                                    citaId
                            );

            SignosVitalesDTO dto =
                    new SignosVitalesDTO();

            dto.setCitaId(citaId);

            dto.setEmergencia(
                    Boolean.TRUE.equals(
                            cita.getPrioridadEmergencia()
                    )
            );

            model.addAttribute(
                    "cita",
                    cita
            );

            model.addAttribute(
                    "signosForm",
                    dto
            );

            return "enfermeria/signos";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            return "redirect:/enfermeria";
        }
    }

    @PostMapping("/signos/guardar")
    @PreAuthorize("hasAuthority('VITAL_SIGNS_CREATE')")
    public String guardar(
            @ModelAttribute("signosForm")
            SignosVitalesDTO dto,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            // Los errores de conversión también deben mostrar el mensaje del CU.
            if (bindingResult.hasErrors()) {
                String mensaje = "Datos de signos vitales inválidos";
                for (var error : bindingResult.getFieldErrors()) {
                    mensaje = switch (error.getField()) {
                        case "presionSistolica", "presionDiastolica" -> "La presión arterial debe ingresarse en formato sistólica/diastólica (ej: 120/80) dentro de rangos válidos.";
                        case "temperatura" -> "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.";
                        case "peso" -> "El peso debe estar entre 0.5 y 300 kg con dos decimales.";
                        case "talla" -> "La talla debe estar entre 30 y 250 cm con dos decimales.";
                        case "pulso" -> "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.";
                        default -> mensaje;
                    };
                }
                throw new IllegalArgumentException(mensaje);
            }

            var signos =
                    signosVitalesService
                            .registrar(
                                    authentication.getName(),
                                    dto
                            );

            String nombrePaciente =
                    signos.getCita()
                            .getPaciente()
                            .getNombreCompleto();

            if (Boolean.TRUE.equals(
                    signos.getEmergencia()
            )) {

                redirectAttributes
                        .addFlashAttribute(
                                "mensaje",
                                "Signos vitales de emergencia registrados para paciente "
                                        + nombrePaciente
                                        + ". El paciente debe pasar directamente a consulta médica."
                        );

            } else {

                redirectAttributes
                        .addFlashAttribute(
                                "mensaje",
                                "Signos vitales del paciente "
                                        + nombrePaciente
                                        + " registrados correctamente. El paciente puede regresar a la sala de espera."
                        );
            }

            return "redirect:/enfermeria";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            try {
                Cita cita = signosVitalesService.buscarCitaParaSignos(dto.getCitaId());
                model.addAttribute("cita", cita);
                return "enfermeria/signos";
            } catch (IllegalArgumentException | IllegalStateException citaInvalida) {
                redirectAttributes.addFlashAttribute("error", citaInvalida.getMessage());
                return "redirect:/enfermeria";
            }
        }
    }
}
