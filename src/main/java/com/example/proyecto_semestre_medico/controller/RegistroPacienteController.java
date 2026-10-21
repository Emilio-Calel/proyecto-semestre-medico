package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.RegistroPacienteDTO;
import com.example.proyecto_semestre_medico.service.PacienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistroPacienteController {

    private final PacienteService pacienteService;

    public RegistroPacienteController(
            PacienteService pacienteService
    ) {
        this.pacienteService = pacienteService;
    }

    @GetMapping("/registro")
    public String mostrarRegistro(
            @RequestParam(
                    required = false
            )
            String origen,

            @RequestParam(
                    required = false
            )
            String dpi,

            Model model
    ) {

        if (!model.containsAttribute(
                "pacienteForm"
        )) {

            RegistroPacienteDTO dto =
                    new RegistroPacienteDTO();

            /*
             * DPI proveniente del flujo normal
             * de verificación del portal.
             */
            if (model.containsAttribute(
                    "dpiVerificado"
            )) {

                dto.setDpi(
                        model.getAttribute(
                                "dpiVerificado"
                        ).toString()
                );

                /*
                 * DPI proveniente de Recepción - FA03.
                 */
            } else if (
                    "recepcion".equalsIgnoreCase(
                            origen
                    )
                            && dpi != null
                            && dpi.matches(
                            "^[0-9]{13}$"
                    )
            ) {

                dto.setDpi(
                        dpi
                );
            }

            model.addAttribute(
                    "pacienteForm",
                    dto
            );
        }

        model.addAttribute(
                "origenRegistro",
                origen
        );

        model.addAttribute(
                "dpiOrigen",
                dpi
        );

        return "pacientes/registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @ModelAttribute("pacienteForm")
            RegistroPacienteDTO dto,

            @RequestParam(
                    required = false
            )
            String origen,

            @RequestParam(
                    required = false
            )
            String dpiOrigen,

            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            pacienteService.registrar(
                    dto
            );

            /*
             * =====================================================
             * FA03 - REGISTRO INICIADO DESDE RECEPCIÓN
             * =====================================================
             */
            if ("recepcion".equalsIgnoreCase(
                    origen
            )) {

                redirectAttributes
                        .addFlashAttribute(
                                "mensaje",
                                "Paciente registrado correctamente."
                        );

                return "redirect:/recepcion"
                        + "?tipoBusqueda=DPI"
                        + "&criterio="
                        + dto.getDpi().trim();
            }

            /*
             * =====================================================
             * CU-02 - REGISTRO NORMAL DEL PACIENTE
             * =====================================================
             */
            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "¡Registro exitoso! Su cuenta ha sido creada. "
                                    + "Ahora puede iniciar sesión con sus credenciales."
                    );

            return "redirect:/login";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            /*
             * Conservamos el origen para que un error
             * de validación no saque al recepcionista
             * del flujo FA03.
             */
            model.addAttribute(
                    "origenRegistro",
                    origen
            );

            model.addAttribute(
                    "dpiOrigen",
                    dpiOrigen
            );

            /*
             * Nunca devolvemos la contraseña al HTML
             * después de un error.
             */
            dto.setPassword(
                    null
            );

            return "pacientes/registro";
        }
    }
}