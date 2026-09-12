package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.UsuarioInternoEditarDTO;
import com.example.proyecto_semestre_medico.dto.UsuarioInternoFormDTO;
import com.example.proyecto_semestre_medico.dto.UsuarioInternoListadoDTO;
import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.Medico;
import com.example.proyecto_semestre_medico.model.Personal;
import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.EspecialidadRepository;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import com.example.proyecto_semestre_medico.repository.PacienteRepository;
import com.example.proyecto_semestre_medico.repository.PersonalRepository;
import com.example.proyecto_semestre_medico.repository.RolRepository;
import com.example.proyecto_semestre_medico.repository.SucursalEspecialidadRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import com.example.proyecto_semestre_medico.service.UsuarioInternoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.proyecto_semestre_medico.dto.UsuarioInternoFiltroDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioInternoServiceImpl
        implements UsuarioInternoService {

    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final SucursalEspecialidadRepository sucursalEspecialidadRepository;

    private final PasswordEncoder passwordEncoder;
    private final DatosSensiblesService datosSensiblesService;

    public UsuarioInternoServiceImpl(
            PersonalRepository personalRepository,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            SucursalRepository sucursalRepository,
            EspecialidadRepository especialidadRepository,
            MedicoRepository medicoRepository,
            PacienteRepository pacienteRepository,
            AuditoriaRepository auditoriaRepository,
            SucursalEspecialidadRepository sucursalEspecialidadRepository,
            PasswordEncoder passwordEncoder,
            DatosSensiblesService datosSensiblesService
    ) {
        this.personalRepository = personalRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.sucursalRepository = sucursalRepository;
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.sucursalEspecialidadRepository = sucursalEspecialidadRepository;
        this.passwordEncoder = passwordEncoder;
        this.datosSensiblesService = datosSensiblesService;
    }

    // =========================================================
    // LISTAR USUARIOS INTERNOS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioInternoListadoDTO> listar() {
        return personalRepository.listarUsuariosInternos();
    }
    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioInternoListadoDTO> buscar(
            UsuarioInternoFiltroDTO filtro,
            int pagina
    ) {

        if (pagina < 0) {
            pagina = 0;
        }

        String usuario =
                normalizarFiltro(
                        filtro.getUsuario()
                );

        String nombre =
                normalizarFiltro(
                        filtro.getNombre()
                );

        String nitHash = null;

        if (filtro.getNit() != null
                && !filtro.getNit().isBlank()) {

            String nit =
                    filtro.getNit()
                            .trim()
                            .toUpperCase();

            if (!nit.matches(
                    "^[A-Za-z0-9]{8,9}$"
            )) {

                throw new IllegalArgumentException(
                        "El NIT debe contener entre 8 y 9 caracteres alfanuméricos"
                );
            }

            nitHash =
                    datosSensiblesService
                            .generarHashBusqueda(nit);
        }

        Pageable pageable =
                PageRequest.of(
                        pagina,
                        20
                );

        return personalRepository
                .buscarUsuariosInternos(
                        usuario,
                        nombre,
                        nitHash,
                        filtro.getRolId(),
                        filtro.getSucursalId(),
                        pageable
                );
    }

    // =========================================================
    // CREAR USUARIO INTERNO
    // =========================================================

    @Override
    @Transactional
    public void crear(UsuarioInternoFormDTO dto) {

        validar(dto);

        String username =
                dto.getNombreUsuario()
                        .trim()
                        .toLowerCase();

        if (usuarioRepository
                .existsByNombreUsuarioIgnoreCase(username)) {

            throw new IllegalArgumentException(
                    "El nombre de usuario ya existe"
            );
        }

        // -----------------------------------------------------
        // ROL
        // -----------------------------------------------------

        Rol rol = rolRepository
                .findById(dto.getRolId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rol no válido"
                        )
                );

        if (!Boolean.TRUE.equals(rol.getActivo())
                || "PACIENTE".equals(rol.getCodigo())) {

            throw new IllegalArgumentException(
                    "El rol seleccionado no es válido"
            );
        }

        if ("MEDICO".equals(rol.getCodigo())
                && dto.getEspecialidadId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una especialidad para el médico"
            );
        }

        // -----------------------------------------------------
        // SUCURSAL
        // -----------------------------------------------------

        Sucursal sucursal = sucursalRepository
                .findById(dto.getSucursalId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sucursal no válida"
                        )
                );

        if (!Boolean.TRUE.equals(sucursal.getActivo())) {

            throw new IllegalArgumentException(
                    "La sucursal está inactiva"
            );
        }

        // -----------------------------------------------------
        // DPI
        // -----------------------------------------------------

        String dpiHash = null;
        String dpiCifrado = null;

        if (dto.getDpi() != null
                && !dto.getDpi().isBlank()) {

            String dpi = dto.getDpi().trim();

            dpiHash =
                    datosSensiblesService
                            .generarHashBusqueda(dpi);

            boolean existeEnPersonal =
                    personalRepository
                            .existsByDpiHash(dpiHash);

            boolean existeEnPaciente =
                    pacienteRepository
                            .existsByDpiHash(dpiHash);

            if (existeEnPersonal || existeEnPaciente) {

                throw new IllegalArgumentException(
                        "El DPI ya se encuentra registrado"
                );
            }

            dpiCifrado =
                    datosSensiblesService.cifrar(dpi);
        }

        // -----------------------------------------------------
        // NIT
        // -----------------------------------------------------

        String nitCifrado = null;
        String nitHash = null;

        if (dto.getNit() != null
                && !dto.getNit().isBlank()) {

            String nit =
                    dto.getNit()
                            .trim()
                            .toUpperCase();

            nitCifrado =
                    datosSensiblesService.cifrar(nit);

            nitHash =
                    datosSensiblesService
                            .generarHashBusqueda(nit);
        }

        // -----------------------------------------------------
        // CREAR USUARIO
        // -----------------------------------------------------

        Usuario usuario = new Usuario();

        usuario.setNombreUsuario(username);

        usuario.setPasswordHash(
                passwordEncoder.encode(
                        dto.getPasswordTemporal()
                )
        );

        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);

        usuario = usuarioRepository.save(usuario);

        // -----------------------------------------------------
        // CREAR PERSONAL
        // -----------------------------------------------------

        Personal personal = new Personal();

        personal.setUsuario(usuario);
        personal.setSucursal(sucursal);

        personal.setNombreCompleto(
                dto.getNombreCompleto().trim()
        );

        personal.setDpiHash(dpiHash);
        personal.setDpiCifrado(dpiCifrado);
        personal.setNitCifrado(nitCifrado);
        personal.setNitHash(nitHash);

        personal.setTelefono(
                normalizarOpcional(
                        dto.getTelefono()
                )
        );

        personal.setNumeroSeguro(
                normalizarOpcional(
                        dto.getNumeroSeguro()
                )
        );

        personal.setActivo(true);

        personal =
                personalRepository.save(personal);

        // -----------------------------------------------------
        // SI EL ROL ES MEDICO
        // -----------------------------------------------------

        if ("MEDICO".equals(rol.getCodigo())) {

            Especialidad especialidad =
                    especialidadRepository
                            .findById(
                                    dto.getEspecialidadId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Especialidad no válida"
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    especialidad.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "La especialidad está inactiva"
                );
            }

            boolean disponibleEnSucursal =
                    sucursalEspecialidadRepository
                            .existsBySucursalIdAndEspecialidadIdAndActivoTrue(
                                    sucursal.getId(),
                                    especialidad.getId()
                            );

            if (!disponibleEnSucursal) {

                throw new IllegalArgumentException(
                        "La especialidad seleccionada no está "
                                + "disponible en la sucursal seleccionada"
                );
            }

            Medico medico = new Medico();

            medico.setPersonal(personal);
            medico.setEspecialidad(especialidad);
            medico.setActivo(true);

            medicoRepository.save(medico);
        }

        // -----------------------------------------------------
        // AUDITORIA
        // -----------------------------------------------------

        registrarAuditoriaCreacion(usuario);
    }

    // =========================================================
    // BUSCAR USUARIO PARA EDITAR
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public UsuarioInternoEditarDTO buscarParaEditar(
            Long usuarioId
    ) {

        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Usuario no encontrado"
                                )
                        );

        Personal personal =
                personalRepository
                        .findByUsuarioId(usuarioId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El usuario no tiene "
                                                + "información de personal"
                                )
                        );

        UsuarioInternoEditarDTO dto =
                new UsuarioInternoEditarDTO();

        dto.setUsuarioId(usuario.getId());
        dto.setPersonalId(personal.getId());

        dto.setNombreUsuario(
                usuario.getNombreUsuario()
        );

        dto.setNombreCompleto(
                personal.getNombreCompleto()
        );

        dto.setRolId(
                usuario.getRol().getId()
        );

        dto.setSucursalId(
                personal.getSucursal().getId()
        );

        dto.setTelefono(
                personal.getTelefono()
        );

        dto.setNumeroSeguro(
                personal.getNumeroSeguro()
        );

        dto.setTieneDpi(
                personal.getDpiHash() != null
        );

        dto.setTieneNit(
                personal.getNitCifrado() != null
        );

        Optional<Medico> medico =
                medicoRepository
                        .findByPersonalId(
                                personal.getId()
                        );

        if (medico.isPresent()
                && Boolean.TRUE.equals(
                medico.get().getActivo()
        )) {

            dto.setEspecialidadId(
                    medico.get()
                            .getEspecialidad()
                            .getId()
            );
        }

        return dto;
    }

    @Override
    @Transactional
    public void desactivar(Long usuarioId) {

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario no encontrado"
                        )
                );
        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null
                && usuario.getNombreUsuario()
                .equalsIgnoreCase(auth.getName())) {

            throw new IllegalArgumentException(
                    "No puede desactivar su propio usuario"
            );
        }

        Personal personal = personalRepository
                .findByUsuarioId(usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Personal no encontrado"
                        )
                );

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException(
                    "El usuario ya se encuentra inactivo"
            );
        }

        usuario.setActivo(false);

        /*
         * Limpiamos cualquier bloqueo temporal.
         * Ya no tiene sentido mantenerlo si la cuenta
         * fue desactivada administrativamente.
         */
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        usuarioRepository.save(usuario);

        personal.setActivo(false);

        personalRepository.save(personal);

        medicoRepository
                .findByPersonalId(personal.getId())
                .ifPresent(medico -> {

                    medico.setActivo(false);

                    medicoRepository.save(medico);
                });

        registrarAuditoriaEstado(
                usuario,
                "DESACTIVAR_USUARIO",
                "Se desactivó el usuario interno: "
                        + usuario.getNombreUsuario()
        );

        if ("ADMINISTRADOR".equals(
                usuario.getRol().getCodigo()
        )) {

            long administradoresActivos =
                    usuarioRepository
                            .countByRolCodigoAndActivoTrue(
                                    "ADMINISTRADOR"
                            );

            if (administradoresActivos <= 1) {
                throw new IllegalArgumentException(
                        "No se puede desactivar el último administrador activo"
                );
            }
        }
    }

    @Override
    @Transactional
    public void reactivar(Long usuarioId) {

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario no encontrado"
                        )
                );

        Personal personal = personalRepository
                .findByUsuarioId(usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Personal no encontrado"
                        )
                );

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException(
                    "El usuario ya se encuentra activo"
            );
        }

        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        usuarioRepository.save(usuario);

        personal.setActivo(true);

        personalRepository.save(personal);

        /*
         * OJO:
         *
         * Si tiene registro de médico, solamente se reactiva
         * si actualmente su rol sigue siendo MEDICO.
         */
        medicoRepository
                .findByPersonalId(personal.getId())
                .ifPresent(medico -> {

                    boolean esMedico =
                            "MEDICO".equals(
                                    usuario.getRol().getCodigo()
                            );

                    medico.setActivo(esMedico);

                    medicoRepository.save(medico);
                });

        registrarAuditoriaEstado(
                usuario,
                "REACTIVAR_USUARIO",
                "Se reactivó el usuario interno: "
                        + usuario.getNombreUsuario()
        );
    }

    private void registrarAuditoriaEstado(
            Usuario usuarioModificado,
            String accion,
            String detalle
    ) {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        Usuario usuarioActor = null;

        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null) {

            usuarioActor =
                    usuarioRepository
                            .findByNombreUsuarioIgnoreCase(
                                    auth.getName()
                            )
                            .orElse(null);
        }

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuarioActor);
        auditoria.setAccion(accion);
        auditoria.setTipoEntidad("USUARIO");

        auditoria.setEntidadId(
                usuarioModificado
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(detalle);

        auditoriaRepository.save(auditoria);
    }

    // =========================================================
    // ACTUALIZAR USUARIO
    // =========================================================

    @Override
    @Transactional
    public void actualizar(
            UsuarioInternoEditarDTO dto
    ) {

        validarEdicion(dto);

        Usuario usuario =
                usuarioRepository
                        .findById(dto.getUsuarioId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Usuario no encontrado"
                                )
                        );

        Personal personal =
                personalRepository
                        .findByUsuarioId(
                                usuario.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Personal no encontrado"
                                )
                        );

        String username =
                dto.getNombreUsuario()
                        .trim()
                        .toLowerCase();

        // -----------------------------------------------------
        // COMPROBAR USERNAME DUPLICADO
        // -----------------------------------------------------

        if (!usuario.getNombreUsuario()
                .equalsIgnoreCase(username)) {

            if (usuarioRepository
                    .existsByNombreUsuarioIgnoreCase(
                            username
                    )) {

                throw new IllegalArgumentException(
                        "El nombre de usuario ya existe"
                );
            }
        }

        // -----------------------------------------------------
        // ROL
        // -----------------------------------------------------

        Rol rol =
                rolRepository
                        .findById(dto.getRolId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Rol no válido"
                                )
                        );

        if (!Boolean.TRUE.equals(rol.getActivo())
                || "PACIENTE".equals(
                rol.getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "El rol seleccionado no es válido"
            );
        }

        // -----------------------------------------------------
        // SUCURSAL
        // -----------------------------------------------------

        Sucursal sucursal =
                sucursalRepository
                        .findById(
                                dto.getSucursalId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sucursal no válida"
                                )
                        );

        if (!Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal seleccionada está inactiva"
            );
        }

        // -----------------------------------------------------
        // ACTUALIZAR USUARIO
        // -----------------------------------------------------

        usuario.setNombreUsuario(username);
        usuario.setRol(rol);

        /*
         * La contraseña solo cambia si el administrador
         * escribió una nueva.
         */
        if (dto.getNuevaPassword() != null
                && !dto.getNuevaPassword().isBlank()) {

            usuario.setPasswordHash(
                    passwordEncoder.encode(
                            dto.getNuevaPassword()
                    )
            );
        }

        usuarioRepository.save(usuario);

        // -----------------------------------------------------
        // ACTUALIZAR PERSONAL
        // -----------------------------------------------------

        personal.setNombreCompleto(
                dto.getNombreCompleto().trim()
        );

        personal.setSucursal(sucursal);

        personal.setTelefono(
                normalizarOpcional(
                        dto.getTelefono()
                )
        );

        personal.setNumeroSeguro(
                normalizarOpcional(
                        dto.getNumeroSeguro()
                )
        );

        // -----------------------------------------------------
        // CAMBIAR DPI SOLO SI ESCRIBIO UNO NUEVO
        // -----------------------------------------------------

        if (dto.getNuevoDpi() != null
                && !dto.getNuevoDpi().isBlank()) {

            String nuevoDpi =
                    dto.getNuevoDpi().trim();

            String nuevoHash =
                    datosSensiblesService
                            .generarHashBusqueda(
                                    nuevoDpi
                            );

            /*
             * Si escribió exactamente el mismo DPI
             * que ya posee, no lo consideramos duplicado.
             */
            if (!nuevoHash.equals(
                    personal.getDpiHash()
            )) {

                boolean existeEnPersonal =
                        personalRepository
                                .existsByDpiHash(
                                        nuevoHash
                                );

                boolean existeEnPaciente =
                        pacienteRepository
                                .existsByDpiHash(
                                        nuevoHash
                                );

                if (existeEnPersonal
                        || existeEnPaciente) {

                    throw new IllegalArgumentException(
                            "El DPI ya se encuentra registrado"
                    );
                }
            }

            personal.setDpiHash(nuevoHash);

            personal.setDpiCifrado(
                    datosSensiblesService.cifrar(
                            nuevoDpi
                    )
            );
        }

        // -----------------------------------------------------
        // CAMBIAR NIT SOLO SI ESCRIBIO UNO NUEVO
        // -----------------------------------------------------

        if (dto.getNuevoNit() != null
                && !dto.getNuevoNit().isBlank()) {

            String nuevoNit =
                    dto.getNuevoNit()
                            .trim()
                            .toUpperCase();

            personal.setNitCifrado(
                    datosSensiblesService.cifrar(
                            nuevoNit
                    )
            );

            personal.setNitHash(
                    datosSensiblesService
                            .generarHashBusqueda(
                                    nuevoNit
                            )
            );
        }

        personalRepository.save(personal);

        // -----------------------------------------------------
        // CONTROLAR MEDICO
        // -----------------------------------------------------

        Optional<Medico> medicoExistente =
                medicoRepository
                        .findByPersonalId(
                                personal.getId()
                        );

        /*
         * CASO 1:
         * El usuario ahora es MEDICO.
         */
        if ("MEDICO".equals(rol.getCodigo())) {

            if (dto.getEspecialidadId() == null) {

                throw new IllegalArgumentException(
                        "Debe seleccionar una especialidad"
                );
            }

            Especialidad especialidad =
                    especialidadRepository
                            .findById(
                                    dto.getEspecialidadId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Especialidad no válida"
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    especialidad.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "La especialidad está inactiva"
                );
            }

            boolean disponible =
                    sucursalEspecialidadRepository
                            .existsBySucursalIdAndEspecialidadIdAndActivoTrue(
                                    sucursal.getId(),
                                    especialidad.getId()
                            );

            if (!disponible) {

                throw new IllegalArgumentException(
                        "La especialidad seleccionada no está "
                                + "disponible en la sucursal"
                );
            }

            /*
             * Si ya había sido médico alguna vez,
             * reutilizamos el mismo registro.
             */
            if (medicoExistente.isPresent()) {

                Medico medico =
                        medicoExistente.get();

                medico.setEspecialidad(
                        especialidad
                );

                medico.setActivo(true);

                medicoRepository.save(medico);

            } else {

                /*
                 * Nunca fue médico.
                 * Creamos su registro.
                 */
                Medico medico =
                        new Medico();

                medico.setPersonal(personal);

                medico.setEspecialidad(
                        especialidad
                );

                medico.setActivo(true);

                medicoRepository.save(medico);
            }

        } else {

            /*
             * CASO 2:
             * Ya no tiene rol MEDICO.
             *
             * Si tenía registro médico, no lo borramos.
             * Simplemente lo desactivamos.
             */
            if (medicoExistente.isPresent()) {

                Medico medico =
                        medicoExistente.get();

                medico.setActivo(false);

                medicoRepository.save(medico);
            }
        }

        // -----------------------------------------------------
        // AUDITORIA
        // -----------------------------------------------------

        registrarAuditoriaActualizacion(usuario);
    }



    // =========================================================
    // VALIDACIONES DE CREACION
    // =========================================================

    private void validar(
            UsuarioInternoFormDTO dto
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Datos requeridos"
            );
        }

        // -----------------------------------------------------
        // NOMBRE
        // -----------------------------------------------------

        if (dto.getNombreCompleto() == null
                || dto.getNombreCompleto()
                .trim()
                .length() < 10
                || dto.getNombreCompleto()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "El nombre debe tener entre "
                            + "10 y 100 caracteres"
            );
        }

        // -----------------------------------------------------
        // USERNAME
        // -----------------------------------------------------

        if (dto.getNombreUsuario() == null
                || !dto.getNombreUsuario()
                .trim()
                .matches(
                        "^[A-Za-z0-9]{8,9}$"
                )) {

            throw new IllegalArgumentException(
                    "El usuario debe contener "
                            + "8 a 9 caracteres alfanuméricos"
            );
        }

        // -----------------------------------------------------
        // PASSWORD
        // -----------------------------------------------------

        if (dto.getPasswordTemporal() == null
                || dto.getPasswordTemporal()
                .length() < 12) {

            throw new IllegalArgumentException(
                    "La contraseña debe tener "
                            + "mínimo 12 caracteres"
            );
        }

        // -----------------------------------------------------
        // ROL
        // -----------------------------------------------------

        if (dto.getRolId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un rol"
            );
        }

        // -----------------------------------------------------
        // SUCURSAL
        // -----------------------------------------------------

        if (dto.getSucursalId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal"
            );
        }

        // -----------------------------------------------------
        // DPI
        // -----------------------------------------------------

        if (dto.getDpi() != null
                && !dto.getDpi().isBlank()
                && !dto.getDpi()
                .trim()
                .matches("^[0-9]{13}$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener "
                            + "exactamente 13 dígitos"
            );
        }

        // -----------------------------------------------------
        // NIT
        // -----------------------------------------------------

        if (dto.getNit() != null
                && !dto.getNit().isBlank()
                && !dto.getNit()
                .trim()
                .matches(
                        "^[A-Za-z0-9]{8,9}$"
                )) {

            throw new IllegalArgumentException(
                    "El NIT debe contener "
                            + "8 a 9 caracteres alfanuméricos"
            );
        }

        // -----------------------------------------------------
        // TELEFONO
        // -----------------------------------------------------

        if (dto.getTelefono() != null
                && !dto.getTelefono().isBlank()
                && !dto.getTelefono()
                .trim()
                .matches("^[0-9]{8}$")) {

            throw new IllegalArgumentException(
                    "El teléfono debe contener 8 dígitos"
            );
        }

        // -----------------------------------------------------
        // NUMERO DE SEGURO
        // -----------------------------------------------------

        if (dto.getNumeroSeguro() != null
                && !dto.getNumeroSeguro().isBlank()) {

            int longitud =
                    dto.getNumeroSeguro()
                            .trim()
                            .length();

            if (longitud < 5
                    || longitud > 50) {

                throw new IllegalArgumentException(
                        "El número de seguro debe tener "
                                + "entre 5 y 50 caracteres"
                );
            }
        }
    }

    // =========================================================
    // VALIDACIONES DE EDICION
    // =========================================================

    private void validarEdicion(
            UsuarioInternoEditarDTO dto
    ) {

        if (dto == null
                || dto.getUsuarioId() == null) {

            throw new IllegalArgumentException(
                    "Usuario requerido"
            );
        }

        // -----------------------------------------------------
        // NOMBRE
        // -----------------------------------------------------

        if (dto.getNombreCompleto() == null
                || dto.getNombreCompleto()
                .trim()
                .length() < 10
                || dto.getNombreCompleto()
                .trim()
                .length() > 100) {

            throw new IllegalArgumentException(
                    "El nombre debe tener entre "
                            + "10 y 100 caracteres"
            );
        }

        // -----------------------------------------------------
        // USERNAME
        // -----------------------------------------------------

        if (dto.getNombreUsuario() == null
                || !dto.getNombreUsuario()
                .trim()
                .matches(
                        "^[A-Za-z0-9]{8,9}$"
                )) {

            throw new IllegalArgumentException(
                    "El usuario debe contener "
                            + "8 a 9 caracteres alfanuméricos"
            );
        }

        // -----------------------------------------------------
        // NUEVA PASSWORD OPCIONAL
        // -----------------------------------------------------

        if (dto.getNuevaPassword() != null
                && !dto.getNuevaPassword().isBlank()
                && dto.getNuevaPassword()
                .length() < 12) {

            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener "
                            + "mínimo 12 caracteres"
            );
        }

        // -----------------------------------------------------
        // ROL
        // -----------------------------------------------------

        if (dto.getRolId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un rol"
            );
        }

        // -----------------------------------------------------
        // SUCURSAL
        // -----------------------------------------------------

        if (dto.getSucursalId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una sucursal"
            );
        }

        // -----------------------------------------------------
        // NUEVO DPI
        // -----------------------------------------------------

        if (dto.getNuevoDpi() != null
                && !dto.getNuevoDpi().isBlank()
                && !dto.getNuevoDpi()
                .trim()
                .matches("^[0-9]{13}$")) {

            throw new IllegalArgumentException(
                    "El DPI debe contener "
                            + "exactamente 13 dígitos"
            );
        }

        // -----------------------------------------------------
        // NUEVO NIT
        // -----------------------------------------------------

        if (dto.getNuevoNit() != null
                && !dto.getNuevoNit().isBlank()
                && !dto.getNuevoNit()
                .trim()
                .matches(
                        "^[A-Za-z0-9]{8,9}$"
                )) {

            throw new IllegalArgumentException(
                    "El NIT debe contener "
                            + "8 a 9 caracteres alfanuméricos"
            );
        }

        // -----------------------------------------------------
        // TELEFONO
        // -----------------------------------------------------

        if (dto.getTelefono() != null
                && !dto.getTelefono().isBlank()
                && !dto.getTelefono()
                .trim()
                .matches("^[0-9]{8}$")) {

            throw new IllegalArgumentException(
                    "El teléfono debe contener 8 dígitos"
            );
        }

        // -----------------------------------------------------
        // NUMERO DE SEGURO
        // -----------------------------------------------------

        if (dto.getNumeroSeguro() != null
                && !dto.getNumeroSeguro().isBlank()) {

            int longitud =
                    dto.getNumeroSeguro()
                            .trim()
                            .length();

            if (longitud < 5
                    || longitud > 50) {

                throw new IllegalArgumentException(
                        "El número de seguro debe tener "
                                + "entre 5 y 50 caracteres"
                );
            }
        }
    }

    // =========================================================
    // NORMALIZAR CAMPO OPCIONAL
    // =========================================================

    private String normalizarOpcional(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        return valor.trim();
    }

    // =========================================================
    // AUDITORIA - CREACION
    // =========================================================

    private void registrarAuditoriaCreacion(
            Usuario usuarioCreado
    ) {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        Usuario usuarioActor = null;

        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null) {

            usuarioActor =
                    usuarioRepository
                            .findByNombreUsuarioIgnoreCase(
                                    auth.getName()
                            )
                            .orElse(null);
        }

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuarioActor);

        auditoria.setAccion(
                "CREAR_USUARIO"
        );

        auditoria.setTipoEntidad(
                "USUARIO"
        );

        auditoria.setEntidadId(
                usuarioCreado
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(
                "Se creó el usuario interno: "
                        + usuarioCreado
                        .getNombreUsuario()
        );

        auditoriaRepository.save(auditoria);
    }

    // =========================================================
    // AUDITORIA - ACTUALIZACION
    // =========================================================

    private void registrarAuditoriaActualizacion(
            Usuario usuarioModificado
    ) {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        Usuario usuarioActor = null;

        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null) {

            usuarioActor =
                    usuarioRepository
                            .findByNombreUsuarioIgnoreCase(
                                    auth.getName()
                            )
                            .orElse(null);
        }

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuarioActor);

        auditoria.setAccion(
                "ACTUALIZAR_USUARIO"
        );

        auditoria.setTipoEntidad(
                "USUARIO"
        );

        auditoria.setEntidadId(
                usuarioModificado
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(
                "Se actualizó el usuario interno: "
                        + usuarioModificado
                        .getNombreUsuario()
        );

        auditoriaRepository.save(auditoria);
    }
    private String normalizarFiltro(String valor) {

        if (valor == null || valor.isBlank()) {
            return "";
        }

        return valor.trim();
    }
}