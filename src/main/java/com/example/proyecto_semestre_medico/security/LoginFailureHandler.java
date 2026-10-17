package com.example.proyecto_semestre_medico.security;

import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.AutenticacionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final int MAX_INTENTOS = 5;

    private final AutenticacionService autenticacionService;
    private final UsuarioRepository usuarioRepository;

    public LoginFailureHandler(
            AutenticacionService autenticacionService,
            UsuarioRepository usuarioRepository
    ) {
        this.autenticacionService = autenticacionService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String nombreUsuario =
                request.getParameter("username");

        /*
         * FA08 - Error de conexión durante el inicio de sesión.
         */
        if (exception instanceof InternalAuthenticationServiceException) {

            response.sendRedirect(
                    "/login?conexionError=true"
            );

            return;
        }

        /*
         * Si Spring Security detectó que la cuenta ya está
         * bloqueada, no incrementamos nuevamente los intentos.
         */
        if (exception instanceof LockedException) {

            response.sendRedirect(
                    "/login?bloqueado=true"
            );

            return;
        }

        autenticacionService.registrarIntentoFallido(
                nombreUsuario
        );

        Optional<Usuario> usuarioOptional =
                usuarioRepository
                        .findByNombreUsuarioIgnoreCase(
                                nombreUsuario == null
                                        ? ""
                                        : nombreUsuario.trim()
                        );

        if (usuarioOptional.isPresent()) {

            Usuario usuario =
                    usuarioOptional.get();

            boolean bloqueado =
                    usuario.getBloqueadoHasta() != null
                            && usuario.getBloqueadoHasta()
                            .isAfter(LocalDateTime.now());

            if (bloqueado) {

                response.sendRedirect(
                        "/login?bloqueado=true"
                );

                return;
            }

            int intentosFallidos =
                    usuario.getIntentosFallidos() == null
                            ? 0
                            : usuario.getIntentosFallidos();

            int intentosRestantes =
                    Math.max(
                            0,
                            MAX_INTENTOS - intentosFallidos
                    );

            response.sendRedirect(
                    "/login?error=true&restantes="
                            + intentosRestantes
            );

            return;
        }

        /*
         * Si el usuario no existe, mantenemos un mensaje genérico.
         * No revelamos si existe o no.
         */
        response.sendRedirect(
                "/login?error=true&restantes=4"
        );
    }
}