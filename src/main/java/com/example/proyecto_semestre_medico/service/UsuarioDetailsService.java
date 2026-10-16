package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Permiso;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .findByNombreUsuarioIgnoreCase(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado"
                        )
                );

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // Rol principal.
        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_" + usuario.getRol().getCodigo()
                )
        );

        // Permisos granulares activos.
        for (Permiso permiso : usuario.getRol().getPermisos()) {
            if (Boolean.TRUE.equals(permiso.getActivo())) {
                authorities.add(
                        new SimpleGrantedAuthority(
                                permiso.getCodigo()
                        )
                );
            }
        }

        boolean bloqueadoTemporalmente =
                usuario.getBloqueadoHasta() != null
                        && usuario.getBloqueadoHasta()
                        .isAfter(LocalDateTime.now());

        boolean usuarioHabilitado =
                Boolean.TRUE.equals(usuario.getActivo())
                        && usuario.getRol() != null
                        && Boolean.TRUE.equals(
                        usuario.getRol().getActivo()
                );

        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(!usuarioHabilitado)
                .accountLocked(bloqueadoTemporalmente)
                .build();
    }
}