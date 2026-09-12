package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.RegistroPacienteDTO;
import com.example.proyecto_semestre_medico.model.Paciente;
import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.PacienteRepository;
import com.example.proyecto_semestre_medico.repository.PersonalRepository;
import com.example.proyecto_semestre_medico.repository.RolRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import com.example.proyecto_semestre_medico.service.PacienteService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatosSensiblesService datosSensiblesService;

    public PacienteServiceImpl(
            PacienteRepository pacienteRepository,
            PersonalRepository personalRepository,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            DatosSensiblesService datosSensiblesService
    ) {
        this.pacienteRepository = pacienteRepository;
        this.personalRepository = personalRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.datosSensiblesService = datosSensiblesService;
    }

    @Override
    @Transactional
    public void registrar(RegistroPacienteDTO dto) {

        validar(dto);

        // ============================================
        // NORMALIZAR DATOS
        // ============================================

        String nombre =
                dto.getNombreCompleto().trim();

        String dpi =
                dto.getDpi().trim();

        String nit =
                dto.getNit()
                        .trim()
                        .toUpperCase();

        String telefono =
                dto.getTelefono().trim();

        String correo =
                dto.getCorreo()
                        .trim()
                        .toLowerCase();

        String username =
                dto.getNombreUsuario()
                        .trim()
                        .toLowerCase();

        String numeroSeguro =
                normalizarOpcional(
                        dto.getNumeroSeguro()
                );

        // ============================================
        // USERNAME ÚNICO
        // ============================================

        if (usuarioRepository
                .existsByNombreUsuarioIgnoreCase(username)) {

            throw new IllegalArgumentException(
                    "El nombre de usuario ya está registrado"
            );
        }

        // ============================================
        // CORREO ÚNICO
        // ============================================

        if (pacienteRepository
                .existsByCorreoIgnoreCase(correo)) {

            throw new IllegalArgumentException(
                    "El correo electrónico ya está registrado"
            );
        }

        // ============================================
        // DPI ÚNICO GLOBAL
        // ============================================

        String dpiHash =
                datosSensiblesService
                        .generarHashBusqueda(dpi);

        boolean dpiEnPaciente =
                pacienteRepository
                        .existsByDpiHash(dpiHash);

        boolean dpiEnPersonal =
                personalRepository
                        .existsByDpiHash(dpiHash);

        if (dpiEnPaciente || dpiEnPersonal) {

            throw new IllegalArgumentException(
                    "El DPI ya se encuentra registrado"
            );
        }

        // ============================================
        // ROL PACIENTE
        // ============================================

        Rol rolPaciente =
                rolRepository
                        .findByCodigo("PACIENTE")
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el rol PACIENTE"
                                )
                        );

        if (!Boolean.TRUE.equals(
                rolPaciente.getActivo()
        )) {

            throw new IllegalStateException(
                    "El rol PACIENTE se encuentra inactivo"
            );
        }

        // ============================================
        // CREAR USUARIO
        // ============================================

        Usuario usuario = new Usuario();

        usuario.setNombreUsuario(username);

        usuario.setPasswordHash(
                passwordEncoder.encode(
                        dto.getPassword()
                )
        );

        usuario.setRol(rolPaciente);
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);

        usuario =
                usuarioRepository.save(usuario);

        // ============================================
        // CIFRAR DATOS
        // ============================================

        String dpiCifrado =
                datosSensiblesService.cifrar(dpi);

        String nitCifrado =
                datosSensiblesService.cifrar(nit);

        // ============================================
        // CREAR PACIENTE
        // ============================================

        Paciente paciente =
                new Paciente();

        paciente.setUsuario(usuario);
        paciente.setNombreCompleto(nombre);

        paciente.setDpiHash(dpiHash);
        paciente.setDpiCifrado(dpiCifrado);

        paciente.setNitCifrado(nitCifrado);

        paciente.setTelefono(telefono);
        paciente.setCorreo(correo);

        paciente.setNumeroSeguro(
                numeroSeguro
        );

        pacienteRepository.save(paciente);
    }

    // ================================================
    // VALIDACIONES
    // ================================================

    private void validar(RegistroPacienteDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Los datos de registro son obligatorios"
            );
        }

        // NOMBRE 10 - 100
        if (dto.getNombreCompleto() == null
                || dto.getNombreCompleto()
                .trim()
                .length() < 10
                || dto.getNombreCompleto()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "El nombre completo debe tener entre 10 y 100 caracteres"
            );
        }

        // DPI EXACTAMENTE 13
        if (dto.getDpi() == null
                || !dto.getDpi()
                .trim()
                .matches("^[0-9]{13}$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener exactamente 13 dígitos"
            );
        }

        // NIT 8 - 9 ALFANUMÉRICO
        if (dto.getNit() == null
                || !dto.getNit()
                .trim()
                .matches("^[A-Za-z0-9]{8,9}$")) {

            throw new IllegalArgumentException(
                    "El NIT debe contener entre 8 y 9 caracteres alfanuméricos"
            );
        }

        // TELÉFONO 8 DÍGITOS
        if (dto.getTelefono() == null
                || !dto.getTelefono()
                .trim()
                .matches("^[0-9]{8}$")) {

            throw new IllegalArgumentException(
                    "El teléfono debe contener exactamente 8 dígitos"
            );
        }

        // SEGURO OPCIONAL 5 - 50
        if (dto.getNumeroSeguro() != null
                && !dto.getNumeroSeguro().isBlank()) {

            int longitud =
                    dto.getNumeroSeguro()
                            .trim()
                            .length();

            if (longitud < 5
                    || longitud > 50) {

                throw new IllegalArgumentException(
                        "El número de seguro debe tener entre 5 y 50 caracteres"
                );
            }
        }

        // CORREO
        if (dto.getCorreo() == null
                || !dto.getCorreo()
                .trim()
                .matches(
                        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
                )) {

            throw new IllegalArgumentException(
                    "Debe ingresar un correo electrónico válido"
            );
        }

        // USERNAME 8 - 9
        if (dto.getNombreUsuario() == null
                || !dto.getNombreUsuario()
                .trim()
                .matches("^[A-Za-z0-9]{8,9}$")) {

            throw new IllegalArgumentException(
                    "El usuario debe contener entre 8 y 9 caracteres alfanuméricos"
            );
        }

        // PASSWORD >= 12
        if (dto.getPassword() == null
                || dto.getPassword()
                .length() < 12) {

            throw new IllegalArgumentException(
                    "La contraseña debe contener mínimo 12 caracteres"
            );
        }
    }

    private String normalizarOpcional(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        return valor.trim();
    }
}