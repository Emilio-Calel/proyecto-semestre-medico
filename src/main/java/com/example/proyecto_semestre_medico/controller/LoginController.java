package com.example.proyecto_semestre_medico.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(
            HttpServletRequest request
    ) {

        /*
         * La vista de login contiene un formulario POST
         * protegido por CSRF.
         *
         * Creamos la sesión antes de renderizar la vista
         * para evitar que Thymeleaf intente crearla cuando
         * la respuesta ya comenzó a enviarse.
         */
        request.getSession(true);

        return "login";
    }
}