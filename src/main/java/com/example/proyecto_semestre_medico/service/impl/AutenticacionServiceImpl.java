package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.AutenticacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;

    public AutenticacionServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void registrarIntentoFallido(String nombreUsuario) {

        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return;
        }

        Optional<Usuario> usuarioOptional =
                usuarioRepository.findByNombreUsuarioIgnoreCase(
                        nombreUsuario.trim()
                );

        // No revelamos si el usuario existe o no.
        if (usuarioOptional.isEmpty()) {
            return;
        }

        Usuario usuario = usuarioOptional.get();

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();

        // Si tenía un bloqueo anterior pero ya terminó,
        // comenzamos nuevamente el contador.
        if (usuario.getBloqueadoHasta() != null
                && !usuario.getBloqueadoHasta().isAfter(ahora)) {

            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }

        // Si todavía está bloqueado no seguimos incrementando.
        if (usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(ahora)) {
            return;
        }

        int intentos = usuario.getIntentosFallidos() == null
                ? 0
                : usuario.getIntentosFallidos();

        intentos++;

        usuario.setIntentosFallidos(intentos);

        if (intentos >= MAX_INTENTOS) {
            usuario.setBloqueadoHasta(
                    ahora.plusMinutes(MINUTOS_BLOQUEO)
            );
        }

        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void registrarLoginExitoso(String nombreUsuario) {

        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return;
        }

        usuarioRepository
                .findByNombreUsuarioIgnoreCase(nombreUsuario.trim())
                .ifPresent(usuario -> {

                    usuario.setIntentosFallidos(0);
                    usuario.setBloqueadoHasta(null);

                    usuarioRepository.save(usuario);
                });
    }
}