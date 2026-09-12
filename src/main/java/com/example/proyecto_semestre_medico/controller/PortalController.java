package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.VerificacionDpiDTO;
import com.example.proyecto_semestre_medico.service.PortalPacienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PortalController {

    private final PortalPacienteService portalPacienteService;

    public PortalController(
            PortalPacienteService portalPacienteService
    ) {
        this.portalPacienteService =
                portalPacienteService;
    }

    @GetMapping("/portal")
    public String portal(
            Model model
    ) {

        if (!model.containsAttribute(
                "verificacionDpi"
        )) {

            model.addAttribute(
                    "verificacionDpi",
                    new VerificacionDpiDTO()
            );
        }

        return "portal/index";
    }

    @PostMapping("/portal/verificar-dpi")
    public String verificarDpi(
            @ModelAttribute("verificacionDpi")
            VerificacionDpiDTO dto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            PortalPacienteService
                    .ResultadoVerificacionDpi resultado =
                    portalPacienteService
                            .verificarDpi(
                                    dto.getDpi()
                            );

            switch (resultado) {

                case PACIENTE_REGISTRADO -> {

                    redirectAttributes
                            .addFlashAttribute(
                                    "mensaje",
                                    "Paciente registrado. "
                                            + "Inicie sesión para continuar."
                            );

                    return "redirect:/login";
                }

                case NO_REGISTRADO -> {

                    redirectAttributes
                            .addFlashAttribute(
                                    "dpiVerificado",
                                    dto.getDpi().trim()
                            );

                    return "redirect:/registro";
                }

                case USUARIO_INTERNO -> {

                    model.addAttribute(
                            "error",
                            "El DPI ingresado corresponde "
                                    + "a un usuario interno y "
                                    + "no puede utilizar el portal "
                                    + "de pacientes."
                    );

                    return "portal/index";
                }

                default -> throw new IllegalStateException(
                        "Resultado de verificación no reconocido"
                );
            }

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "portal/index";
        }
    }
}