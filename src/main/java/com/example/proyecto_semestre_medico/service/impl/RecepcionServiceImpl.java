package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.CitaRecepcionDTO;
import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import com.example.proyecto_semestre_medico.service.RecepcionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.proyecto_semestre_medico.model.Pago;
import com.example.proyecto_semestre_medico.repository.PagoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecepcionServiceImpl
        implements RecepcionService {

    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final DatosSensiblesService datosSensiblesService;
    private final PagoRepository pagoRepository;

    public RecepcionServiceImpl(
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            DatosSensiblesService datosSensiblesService,
            PagoRepository pagoRepository
    ) {
        this.citaRepository = citaRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.datosSensiblesService = datosSensiblesService;
        this.pagoRepository = pagoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaRecepcionDTO> buscar(
            String criterio
    ) {

        if (criterio == null
                || criterio.trim().isEmpty()) {

            return List.of();
        }

        String valor = criterio.trim();

        List<Cita> citas =
                new ArrayList<>();

        /*
         * Si tiene 13 dígitos,
         * asumimos que es DPI.
         */
        if (valor.matches("^[0-9]{13}$")) {

            String dpiHash =
                    datosSensiblesService
                            .generarHashBusqueda(valor);

            citas =
                    citaRepository
                            .buscarPorDpiHash(dpiHash);

        } else {

            citaRepository
                    .findByNumeroCitaIgnoreCase(valor)
                    .ifPresent(citas::add);
        }

        return citas.stream()
                .map(this::convertir)
                .toList();
    }

    @Override
    @Transactional
    public void registrarLlegada(
            Long citaId,
            String usuarioRecepcionista
    ) {

        if (citaId == null) {
            throw new IllegalArgumentException(
                    "Cita requerida"
            );
        }

        Cita cita =
                citaRepository
                        .findById(citaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada"
                                )
                        );

        if (!"CONFIRMADA".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "Solo se puede registrar llegada de una cita confirmada"
            );
        }

        boolean pagoAprobado =
                pagoRepository
                        .findFirstByCitaIdAndEstadoOrderByFechaPagoDesc(
                                cita.getId(),
                                "APROBADO"
                        )
                        .isPresent();

        if (!pagoAprobado) {

            throw new IllegalArgumentException(
                    "La cita no tiene un pago aprobado registrado"
            );
        }

        /*
         * Evitamos registrar llegada
         * absurdamente antes de la cita.
         *
         * Dejamos 2 horas de tolerancia previa
         * por ahora.
         */
        LocalDateTime ahora =
                LocalDateTime.now();

        if (ahora.isBefore(
                cita.getFechaHoraInicio()
                        .minusHours(2)
        )) {

            throw new IllegalArgumentException(
                    "Todavía es demasiado temprano para registrar la llegada"
            );
        }

        EstadoCita presente =
                estadoCitaRepository
                        .findByCodigo(
                                "PACIENTE_PRESENTE"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el estado PACIENTE_PRESENTE"
                                )
                        );

        cita.setEstadoCita(presente);

        citaRepository.save(cita);

        registrarAuditoria(
                cita,
                usuarioRecepcionista
        );
    }

    private CitaRecepcionDTO convertir(
            Cita cita
    ) {

        Pago pagoAprobado =
                pagoRepository
                        .findFirstByCitaIdAndEstadoOrderByFechaPagoDesc(
                                cita.getId(),
                                "APROBADO"
                        )
                        .orElse(null);

        String estadoPago;
        String metodoPago;

        if (pagoAprobado != null) {

            estadoPago = "PAGADO";
            metodoPago =
                    pagoAprobado.getMetodoPago();

        } else {

            estadoPago = "PENDIENTE";
            metodoPago = "-";
        }

        return new CitaRecepcionDTO(
                cita.getId(),
                cita.getNumeroCita(),

                cita.getPaciente()
                        .getNombreCompleto(),

                cita.getPaciente()
                        .getTelefono(),

                cita.getPaciente()
                        .getCorreo(),

                cita.getSucursal()
                        .getNombre(),

                cita.getEspecialidad()
                        .getNombre(),

                cita.getMedico()
                        .getPersonal()
                        .getNombreCompleto(),

                cita.getFechaHoraInicio(),

                cita.getEstadoCita()
                        .getCodigo(),

                estadoPago,
                metodoPago
        );
    }

    private void registrarAuditoria(
            Cita cita,
            String nombreUsuario
    ) {

        Usuario usuario =
                usuarioRepository
                        .findByNombreUsuarioIgnoreCase(
                                nombreUsuario
                        )
                        .orElse(null);

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuario);
        auditoria.setAccion(
                "REGISTRAR_LLEGADA"
        );
        auditoria.setTipoEntidad(
                "CITA"
        );
        auditoria.setEntidadId(
                String.valueOf(cita.getId())
        );
        auditoria.setDetalle(
                "Check-in de cita "
                        + cita.getNumeroCita()
                        + ". Nuevo estado: PACIENTE_PRESENTE"
        );

        auditoriaRepository.save(
                auditoria
        );
    }
}