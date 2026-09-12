package com.example.proyecto_semestre_medico.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio(
            Authentication authentication,
            Model model
    ) {

        model.addAttribute(
                "usuario",
                authentication.getName()
        );

        model.addAttribute(
                "permisos",
                authentication.getAuthorities()
        );

        return "index";
    }
}