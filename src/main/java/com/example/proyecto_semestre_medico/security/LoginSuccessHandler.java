package com.example.proyecto_semestre_medico.security;

import com.example.proyecto_semestre_medico.service.AutenticacionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AutenticacionService autenticacionService;

    public LoginSuccessHandler(
            AutenticacionService autenticacionService
    ) {
        this.autenticacionService = autenticacionService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        autenticacionService.registrarLoginExitoso(
                authentication.getName()
        );

        response.sendRedirect("/");
    }
}