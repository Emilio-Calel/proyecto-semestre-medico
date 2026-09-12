package com.example.proyecto_semestre_medico.security;

import com.example.proyecto_semestre_medico.service.AutenticacionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final AutenticacionService autenticacionService;

    public LoginFailureHandler(
            AutenticacionService autenticacionService
    ) {
        this.autenticacionService = autenticacionService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String nombreUsuario =
                request.getParameter("username");

        autenticacionService.registrarIntentoFallido(
                nombreUsuario
        );

        response.sendRedirect("/login?error");
    }
}