package com.example.proyecto_semestre_medico.config;

import com.example.proyecto_semestre_medico.model.Personal;
import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.PersonalRepository;
import com.example.proyecto_semestre_medico.repository.RolRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PersonalRepository personalRepository;
    private final SucursalRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.nombre:Administrador General}")
    private String nombreCompleto;

    @Value("${app.bootstrap-admin.sucursal:Hospital Central}")
    private String nombreSucursal;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PersonalRepository personalRepository,
            SucursalRepository sucursalRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.personalRepository = personalRepository;
        this.sucursalRepository = sucursalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return;
        }

        String usernameNormalizado =
                username.trim().toLowerCase();

        if (usernameNormalizado.length() < 8
                || usernameNormalizado.length() > 9
                || !usernameNormalizado.matches("^[A-Za-z0-9]+$")) {

            throw new IllegalStateException(
                    "El usuario administrador bootstrap debe tener "
                            + "8-9 caracteres alfanuméricos"
            );
        }

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "La contraseña bootstrap debe tener mínimo 12 caracteres"
            );
        }

        Rol rolAdministrador = rolRepository
                .findByCodigo("ADMINISTRADOR")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el rol ADMINISTRADOR"
                        )
                );

        Usuario usuario = usuarioRepository
                .findByNombreUsuarioIgnoreCase(usernameNormalizado)
                .orElseGet(() -> {

                    Usuario nuevoUsuario = new Usuario();

                    nuevoUsuario.setNombreUsuario(usernameNormalizado);
                    nuevoUsuario.setPasswordHash(
                            passwordEncoder.encode(password)
                    );
                    nuevoUsuario.setRol(rolAdministrador);
                    nuevoUsuario.setActivo(true);
                    nuevoUsuario.setIntentosFallidos(0);

                    Usuario guardado =
                            usuarioRepository.save(nuevoUsuario);

                    System.out.println(
                            ">>> USUARIO ADMINISTRADOR INICIAL CREADO"
                    );

                    return guardado;
                });

        if (personalRepository
                .findByUsuarioId(usuario.getId())
                .isEmpty()) {

            Sucursal sucursal = sucursalRepository
                    .findByNombreIgnoreCase(nombreSucursal)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "No existe la sucursal bootstrap: "
                                            + nombreSucursal
                            )
                    );

            Personal personal = new Personal();

            personal.setUsuario(usuario);
            personal.setSucursal(sucursal);
            personal.setNombreCompleto(nombreCompleto);
            personal.setActivo(true);

            personalRepository.save(personal);

            System.out.println(
                    ">>> PERSONAL DEL ADMINISTRADOR INICIAL CREADO"
            );
        }
    }
}