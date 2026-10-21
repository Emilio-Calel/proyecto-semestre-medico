package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.AgendarCitaDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.service.CitaCatalogoService;
import com.example.proyecto_semestre_medico.service.CitaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CitaController {

    private final CitaCatalogoService citaCatalogoService;
    private final CitaService citaService;

    public CitaController(
            CitaCatalogoService citaCatalogoService,
            CitaService citaService
    ) {
        this.citaCatalogoService =
                citaCatalogoService;
        this.citaService =
                citaService;
    }

    @GetMapping("/citas/nueva")
    @PreAuthorize("hasRole('PACIENTE')")
    public String nueva(
            @RequestParam(
                    required = false
            )
            Long sucursalId,

            @RequestParam(
                    required = false
            )
            Long especialidadId,

            @RequestParam(
                    required = false
            )
            Long medicoId,

            @RequestParam(
                    required = false
            )
            Integer paso,

            Model model
    ) {

        AgendarCitaDTO dto =
                new AgendarCitaDTO();

        dto.setSucursalId(
                sucursalId
        );

        dto.setEspecialidadId(
                especialidadId
        );

        dto.setMedicoId(
                medicoId
        );

        model.addAttribute(
                "citaForm",
                dto
        );

        model.addAttribute(
                "sucursales",
                citaCatalogoService
                        .listarSucursales()
        );

        model.addAttribute(
                "pasoInicial",
                paso == null
                        ? 1
                        : paso
        );

        return "citas/nueva";
    }

    @PostMapping("/citas/guardar")
    @PreAuthorize("hasRole('PACIENTE')")
    public String guardar(
            @ModelAttribute("citaForm")
            AgendarCitaDTO dto,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Cita cita =
                    citaService.agendar(
                            authentication
                                    .getName(),
                            dto
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Su cita ha sido registrada exitosamente. "
                            + "Será redirigido al proceso de pago para confirmar la reserva."
            );

            return "redirect:/pagos/online/"
                    + cita.getId();

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "sucursales",
                    citaCatalogoService
                            .listarSucursales()
            );

            return "citas/nueva";
        }
    }

    @GetMapping("/citas/confirmacion")
    @PreAuthorize("hasRole('PACIENTE')")
    public String confirmacion() {
        return "citas/confirmacion";
    }

    @GetMapping("/citas/reagendar/{citaId}")
    @PreAuthorize("hasRole('PACIENTE')")
    public String reagendar(
            @PathVariable Long citaId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        Cita cita =
                citaService.obtenerParaReagendar(
                        citaId,
                        authentication.getName()
                );

        redirectAttributes.addFlashAttribute(
                "mensajeExpiracion",
                "El tiempo para confirmar su cita ha expirado. "
                        + "El horario seleccionado ha sido liberado. "
                        + "Por favor, seleccione un nuevo horario."
        );

        return "redirect:/citas/nueva"
                + "?sucursalId="
                + cita.getSucursal().getId()
                + "&especialidadId="
                + cita.getEspecialidad().getId()
                + "&medicoId="
                + cita.getMedico().getId()
                + "&paso=4";
    }


}