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
import com.example.proyecto_semestre_medico.service.CorreoPendienteService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatosSensiblesService datosSensiblesService;
    private final CorreoPendienteService correoPendienteService;

    @Value("${app.hospital.nombre}")
    private String nombreHospital;

    @Value("${app.hospital.telefono}")
    private String telefonoHospital;

    public PacienteServiceImpl(
            PacienteRepository pacienteRepository,
            PersonalRepository personalRepository,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            DatosSensiblesService datosSensiblesService,
            CorreoPendienteService correoPendienteService
    ) {
        this.pacienteRepository = pacienteRepository;
        this.personalRepository = personalRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.datosSensiblesService = datosSensiblesService;
        this.correoPendienteService =
                correoPendienteService;
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
                    "El nombre de usuario ya se encuentra registrado."
            );
        }

        // ============================================
        // CORREO ÚNICO
        // ============================================

        if (pacienteRepository
                .existsByCorreoIgnoreCase(correo)) {

            throw new IllegalArgumentException(
                    "Ya existe una cuenta registrada con este correo electrónico."
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
                    "Ya existe una cuenta registrada con este número de DPI. Si ya tiene cuenta, inicie sesión."
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

        // ============================================
// CORREO DE BIENVENIDA
// ============================================

        String asunto =
                "Bienvenido al Sistema de Citas - Hospital "
                        + nombreHospital;

        String contenido =
                """
                <!DOCTYPE html>
                <html lang="es">
        
                <body style="
                    margin:0;
                    padding:0;
                    background:#f4f8f9;
                    font-family:Arial,Helvetica,sans-serif;
                    color:#263238;
                ">
        
                <table
                    width="100%%"
                    cellpadding="0"
                    cellspacing="0"
                    border="0"
                    style="
                        background:#f4f8f9;
                        padding:30px 15px;
                    "
                >
        
                    <tr>
        
                        <td align="center">
        
                            <table
                                width="100%%"
                                cellpadding="0"
                                cellspacing="0"
                                border="0"
                                style="
                                    max-width:620px;
                                    background:#ffffff;
                                    border-radius:16px;
                                    overflow:hidden;
                                    box-shadow:0 6px 18px rgba(30,70,80,0.10);
                                "
                            >
        
                                <!-- ENCABEZADO -->
        
                                <tr>
        
                                    <td
                                        style="
                                            padding:28px 32px;
                                            background:linear-gradient(
                                                135deg,
                                                #0f6f7a,
                                                #1596a3
                                            );
                                            color:#ffffff;
                                            text-align:center;
                                        "
                                    >
        
                                        <div
                                            style="
                                                font-size:30px;
                                                margin-bottom:10px;
                                            "
                                        >
                                            ✚
                                        </div>
        
                                        <div
                                            style="
                                                font-size:21px;
                                                font-weight:bold;
                                            "
                                        >
                                            Hospital %s
                                        </div>
        
                                        <div
                                            style="
                                                font-size:13px;
                                                margin-top:6px;
                                                opacity:0.90;
                                            "
                                        >
                                            Sistema Informático Hospitalario
                                        </div>
        
                                    </td>
        
                                </tr>
        
                                <!-- CONTENIDO -->
        
                                <tr>
        
                                    <td
                                        style="
                                            padding:34px 36px;
                                        "
                                    >
        
                                        <h2
                                            style="
                                                margin:0 0 18px 0;
                                                color:#0f6f7a;
                                                font-size:23px;
                                            "
                                        >
                                            ¡Bienvenido(a)!
                                        </h2>
        
                                        <p
                                            style="
                                                margin:0 0 16px 0;
                                                font-size:15px;
                                                line-height:1.7;
                                            "
                                        >
                                            Estimado(a)
                                            <strong>%s</strong>,
                                            su registro ha sido completado exitosamente.
                                        </p>
        
                                        <p
                                            style="
                                                margin:0 0 24px 0;
                                                font-size:15px;
                                                line-height:1.7;
                                            "
                                        >
                                            Ya puede agendar sus citas médicas
                                            a través de nuestro portal.
                                        </p>
        
                                        <div
                                            style="
                                                background:#eef8f9;
                                                border-left:4px solid #1596a3;
                                                border-radius:8px;
                                                padding:16px 18px;
                                                margin:22px 0;
                                                color:#42565c;
                                                font-size:14px;
                                                line-height:1.6;
                                            "
                                        >
                                            Su cuenta se encuentra activa y
                                            puede iniciar sesión utilizando
                                            las credenciales registradas.
                                        </div>
        
                                        <p
                                            style="
                                                margin:25px 0 0 0;
                                                font-size:14px;
                                                color:#6d7c82;
                                                line-height:1.6;
                                            "
                                        >
                                            Gracias por utilizar nuestro
                                            Sistema de Citas Médicas.
                                        </p>
        
                                    </td>
        
                                </tr>
        
                                <!-- PIE -->
        
                                <tr>
        
                                    <td
                                        style="
                                            padding:22px 30px;
                                            background:#f5f9fa;
                                            border-top:1px solid #e2ebed;
                                            text-align:center;
                                        "
                                    >
        
                                        <p
                                            style="
                                                margin:0;
                                                font-size:12px;
                                                color:#718087;
                                                line-height:1.6;
                                            "
                                        >
                                            Este es un correo automático del
                                            Sistema Informático Hospitalario.
                                            No responda a este mensaje.
                                            Para consultas, comuníquese al teléfono
                                            <strong>%s</strong>.
                                        </p>
        
                                    </td>
        
                                </tr>
        
                            </table>
        
                        </td>
        
                    </tr>
        
                </table>
        
                </body>
                </html>
                """.formatted(
                        nombreHospital,
                        nombre,
                        telefonoHospital
                );

        correoPendienteService.registrar(
                correo,
                asunto,
                contenido
        );
    }

    // ================================================
    // VALIDACIONES
    // ================================================

    private void validar(
            RegistroPacienteDTO dto
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Los datos de registro son obligatorios"
            );
        }

        // =====================================================
        // NOMBRE
        // =====================================================

        if (dto.getNombreCompleto() == null
                || dto.getNombreCompleto()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El campo Nombre es obligatorio."
            );
        }

        int longitudNombre =
                dto.getNombreCompleto()
                        .trim()
                        .length();

        if (longitudNombre < 10
                || longitudNombre > 100) {

            throw new IllegalArgumentException(
                    "El nombre debe contener entre 10 y 100 caracteres. Usted ingresó "
                            + longitudNombre
                            + " caracteres."
            );
        }

        // =====================================================
        // DPI
        // =====================================================

        if (dto.getDpi() == null
                || dto.getDpi()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El campo DPI es obligatorio. Por favor, ingrese su número de DPI."
            );
        }

        String dpi =
                dto.getDpi().trim();

        if (!dpi.matches("^[0-9]+$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales."
            );
        }

        if (dpi.length() != 13) {

            throw new IllegalArgumentException(
                    "El DPI debe contener exactamente 13 dígitos. Usted ingresó "
                            + dpi.length()
                            + " dígitos."
            );
        }

        // =====================================================
        // NIT
        // =====================================================

        if (dto.getNit() == null
                || dto.getNit()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El campo NIT es obligatorio."
            );
        }

        String nit =
                dto.getNit()
                        .trim()
                        .toUpperCase();

        if (nit.length() < 8
                || nit.length() > 9) {

            throw new IllegalArgumentException(
                    "El NIT debe contener entre 8 y 9 caracteres. Usted ingresó "
                            + nit.length()
                            + " caracteres."
            );
        }

        if (!nit.matches("^[A-Za-z0-9]+$")) {

            throw new IllegalArgumentException(
                    "El NIT debe contener únicamente caracteres alfanuméricos."
            );
        }

        // =====================================================
        // TELÉFONO
        // =====================================================

        if (dto.getTelefono() == null
                || dto.getTelefono()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El número de teléfono debe contener exactamente 8 dígitos numéricos."
            );
        }

        if (!dto.getTelefono()
                .trim()
                .matches("^[0-9]{8}$")) {

            throw new IllegalArgumentException(
                    "El número de teléfono debe contener exactamente 8 dígitos numéricos."
            );
        }

        // =====================================================
        // SEGURO MÉDICO
        // =====================================================

        if (dto.getNumeroSeguro() != null
                && !dto.getNumeroSeguro().isBlank()) {

            int longitudSeguro =
                    dto.getNumeroSeguro()
                            .trim()
                            .length();

            if (longitudSeguro < 5
                    || longitudSeguro > 50) {

                throw new IllegalArgumentException(
                        "El número de seguro debe contener entre 5 y 50 caracteres."
                );
            }
        }

        // =====================================================
        // CORREO
        // =====================================================

        if (dto.getCorreo() == null
                || dto.getCorreo()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com"
            );
        }

        if (!dto.getCorreo()
                .trim()
                .matches(
                        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
                )) {

            throw new IllegalArgumentException(
                    "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com"
            );
        }

        // =====================================================
        // NOMBRE DE USUARIO
        // =====================================================

        if (dto.getNombreUsuario() == null
                || dto.getNombreUsuario()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "El usuario debe contener al menos 8 caracteres."
            );
        }

        String usuario =
                dto.getNombreUsuario().trim();

        if (usuario.length() < 8) {

            throw new IllegalArgumentException(
                    "El usuario debe contener al menos 8 caracteres."
            );
        }

        if (usuario.length() > 9) {

            throw new IllegalArgumentException(
                    "El usuario no puede exceder los 9 caracteres."
            );
        }

        if (!usuario.matches("^[A-Za-z0-9]+$")) {

            throw new IllegalArgumentException(
                    "El usuario debe contener únicamente caracteres alfanuméricos."
            );
        }

        // =====================================================
        // CONTRASEÑA
        // =====================================================

        if (dto.getPassword() == null
                || dto.getPassword()
                .length() < 12) {

            throw new IllegalArgumentException(
                    "La contraseña debe contener al menos 12 caracteres."
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