package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.VerificacionDpiDTO;
import com.example.proyecto_semestre_medico.service.PortalPacienteService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PortalController {

    private final PortalPacienteService portalPacienteService;

    public PortalController(
            PortalPacienteService portalPacienteService
    ) {
        this.portalPacienteService =
                portalPacienteService;
    }

    @GetMapping({"/", "/portal"})
    public String portal(
            Model model,
            HttpServletRequest request
    ) {

        /*
         * El portal es público, pero contiene un formulario POST
         * protegido por CSRF.
         *
         * Creamos la sesión antes de renderizar la vista para que
         * Spring Security pueda generar el token CSRF sin intentar
         * crear la sesión después de que la respuesta haya comenzado.
         */
        request.getSession(true);

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

                    return "redirect:/login";
                }

                case NO_REGISTRADO -> {

                    redirectAttributes
                            .addFlashAttribute(
                                    "dpiVerificado",
                                    dto.getDpi().trim()
                            );

                    redirectAttributes
                            .addFlashAttribute(
                                    "mensaje",
                                    "No se encontró un registro asociado a este DPI. Será redirigido al formulario de registro."
                            );

                    return "redirect:/registro";
                }

                case USUARIO_INTERNO -> {

                    model.addAttribute(
                            "error",
                            "Este DPI pertenece a un usuario del sistema interno. Por favor, contacte a recepción."
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

        } catch (DataAccessException e) {

            model.addAttribute(
                    "error",
                    "No se pudo conectar con el servidor. Intente de nuevo más tarde."
            );

            return "portal/index";
        }
    }
}