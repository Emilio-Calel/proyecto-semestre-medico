package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.PagoPresencialDTO;
import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.model.Pago;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.repository.PagoRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.PagoPresencialService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PagoPresencialServiceImpl
        implements PagoPresencialService {

    private final PagoRepository pagoRepository;
    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    private final BigDecimal precioBase;

    public PagoPresencialServiceImpl(
            PagoRepository pagoRepository,
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            @Value("${app.citas.precio-base:250.00}")
            BigDecimal precioBase
    ) {
        this.pagoRepository = pagoRepository;
        this.citaRepository = citaRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.precioBase = precioBase;
    }

    @Override
    @Transactional
    public Pago procesar(
            String usuarioCajero,
            PagoPresencialDTO dto
    ) {

        validarBasico(dto);

        Cita cita = citaRepository
                .findById(dto.getCitaId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cita no encontrada"
                        )
                );

        if (!"PENDIENTE_PAGO".equals(
                cita.getEstadoCita().getCodigo()
        )) {
            throw new IllegalArgumentException(
                    "La cita no se encuentra pendiente de pago"
            );
        }

        boolean yaPagada =
                pagoRepository
                        .findFirstByCitaIdAndEstadoOrderByFechaPagoDesc(
                                cita.getId(),
                                "APROBADO"
                        )
                        .isPresent();

        if (yaPagada) {
            throw new IllegalArgumentException(
                    "La cita ya tiene un pago aprobado"
            );
        }

        Pago pago = new Pago();

        pago.setCita(cita);
        pago.setNumeroTransaccion(
                generarNumeroTransaccion()
        );
        pago.setMonto(precioBase);
        pago.setCanal("PRESENCIAL");

        pago.setClaveIdempotencia(
                "PRES-"
                        + UUID.randomUUID()
                        .toString()
        );

        String metodo =
                dto.getMetodoPago()
                        .trim()
                        .toUpperCase();

        pago.setMetodoPago(metodo);

        if ("EFECTIVO".equals(metodo)) {

            if (dto.getMontoRecibido() == null) {
                throw new IllegalArgumentException(
                        "Debe ingresar el monto recibido."
                );
            }

            if (dto.getMontoRecibido()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "El monto recibido debe ser mayor a Q0.00."
                );
            }

            if (dto.getMontoRecibido()
                    .compareTo(precioBase) < 0) {

                throw new IllegalArgumentException(
                        "El monto recibido (Q"
                                + dto.getMontoRecibido()
                                + ") es menor al monto a cobrar (Q"
                                + precioBase
                                + ")"
                );
            }

            pago.setEstado("APROBADO");
            pago.setUltimos4(null);
            pago.setReferenciaPasarela(null);

        } else {

            String ultimos4 =
                    dto.getUltimos4() == null
                            ? ""
                            : dto.getUltimos4().trim();

            if (!ultimos4.matches("^[0-9]{4}$")) {

                throw new IllegalArgumentException(
                        "Debe ingresar los últimos 4 dígitos de la tarjeta."
                );
            }

            pago.setUltimos4(
                    ultimos4
            );

            /*
             * En CU-06 el pago es presencial.
             * Caja solo registra la referencia
             * de los últimos cuatro dígitos.
             */
            pago.setReferenciaPasarela(
                    "PRESENCIAL-"
                            + metodo
                            + "-"
                            + ultimos4
            );

            if ("0000".equals(ultimos4)) {

                pago.setEstado(
                        "RECHAZADO"
                );

            } else {

                pago.setEstado(
                        "APROBADO"
                );
            }
        }

        Pago pagoGuardado =
                pagoRepository.save(pago);

        if ("APROBADO".equals(
                pagoGuardado.getEstado()
        )) {

            EstadoCita confirmada =
                    estadoCitaRepository
                            .findByCodigo("CONFIRMADA")
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "No existe el estado CONFIRMADA"
                                    )
                            );

            cita.setEstadoCita(confirmada);
            cita.setReservaExpiraEn(null);

            citaRepository.save(cita);

            registrarAuditoria(
                    cita,
                    pagoGuardado,
                    usuarioCajero
            );
        }

        return pagoGuardado;
    }

    private void validarBasico(
            PagoPresencialDTO dto
    ) {

        if (dto == null
                || dto.getCitaId() == null) {

            throw new IllegalArgumentException(
                    "Cita requerida"
            );
        }

        if (dto.getMetodoPago() == null
                || dto.getMetodoPago().isBlank()) {

            throw new IllegalArgumentException(
                    "Método de pago requerido"
            );
        }

        String metodo =
                dto.getMetodoPago()
                        .trim()
                        .toUpperCase();

        if (!metodo.equals("EFECTIVO")
                && !metodo.equals("VISA")
                && !metodo.equals("MASTERCARD")
                && !metodo.equals("DEBITO")) {

            throw new IllegalArgumentException(
                    "Método de pago no válido"
            );
        }
    }

    private String generarNumeroTransaccion() {

        String numero;

        do {

            numero =
                    "PAY-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 10)
                            .toUpperCase();

        } while (
                pagoRepository
                        .existsByNumeroTransaccion(numero)
        );

        return numero;
    }

    private void registrarAuditoria(
            Cita cita,
            Pago pago,
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
                "PAGO_PRESENCIAL"
        );
        auditoria.setTipoEntidad(
                "PAGO"
        );

        auditoria.setEntidadId(
                String.valueOf(pago.getId())
        );

        auditoria.setDetalle(
                "Pago presencial de cita "
                        + cita.getNumeroCita()
                        + ". Método: "
                        + pago.getMetodoPago()
                        + ". Estado: "
                        + pago.getEstado()
        );

        auditoriaRepository.save(
                auditoria
        );
    }
}