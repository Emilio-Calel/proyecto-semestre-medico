package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.RegistroPacienteDTO;
import com.example.proyecto_semestre_medico.service.PacienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            Model model
    ) {

        if (!model.containsAttribute(
                "pacienteForm"
        )) {

            RegistroPacienteDTO dto =
                    new RegistroPacienteDTO();

            if (model.containsAttribute(
                    "dpiVerificado"
            )) {

                dto.setDpi(
                        model.getAttribute(
                                "dpiVerificado"
                        ).toString()
                );
            }

            model.addAttribute(
                    "pacienteForm",
                    dto
            );
        }

        return "pacientes/registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @ModelAttribute("pacienteForm")
            RegistroPacienteDTO dto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            pacienteService.registrar(dto);

            redirectAttributes.addFlashAttribute(
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
             * Nunca devolvemos la contraseña al HTML
             * después de un error.
             */
            dto.setPassword(null);

            return "pacientes/registro";
        }
    }
}