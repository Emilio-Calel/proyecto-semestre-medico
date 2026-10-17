package com.example.proyecto_semestre_medico.security;

import com.example.proyecto_semestre_medico.service.AutenticacionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        boolean esPaciente =
                authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                "ROLE_PACIENTE"
                                        .equals(
                                                authority.getAuthority()
                                        )
                        );

        String tipoAcceso =
                request.getParameter(
                        "tipoAcceso"
                );

        boolean accesoInterno =
                "interno".equalsIgnoreCase(
                        tipoAcceso
                );

        /*
         * FA09:
         * Un usuario interno intentó utilizar
         * el acceso exclusivo para pacientes.
         */
        if (!accesoInterno && !esPaciente) {

            SecurityContextHolder
                    .clearContext();

            HttpSession session =
                    request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(
                    "/login?rolNoAutorizado=true"
            );

            return;
        }

        /*
         * El panel administrativo es para personal interno.
         * Si un paciente intenta utilizarlo, lo enviamos
         * nuevamente al acceso normal.
         */
        if (accesoInterno && esPaciente) {

            SecurityContextHolder
                    .clearContext();

            HttpSession session =
                    request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(
                    "/login"
            );

            return;
        }

        /*
         * Solo registramos login exitoso cuando el tipo de
         * acceso corresponde correctamente con el rol.
         */
        autenticacionService
                .registrarLoginExitoso(
                        authentication.getName()
                );

        response.sendRedirect(
                "/dashboard"
        );
    }
}