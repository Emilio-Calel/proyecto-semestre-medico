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
import com.example.proyecto_semestre_medico.service.PasarelaPagoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
public class PagoPresencialServiceImpl
        implements PagoPresencialService {

    private final PagoRepository pagoRepository;
    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final PasarelaPagoService pasarelaPagoService;

    private final BigDecimal precioBase;

    public PagoPresencialServiceImpl(
            PagoRepository pagoRepository,
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            PasarelaPagoService pasarelaPagoService,
            @Value("${app.citas.precio-base:250.00}")
            BigDecimal precioBase
    ) {
        this.pagoRepository = pagoRepository;
        this.citaRepository = citaRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.pasarelaPagoService = pasarelaPagoService;
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

            pago.setEstado("APROBADO");
            pago.setUltimos4(null);
            pago.setReferenciaPasarela(null);

        } else {

            validarTarjeta(dto);

            String tarjeta =
                    dto.getNumeroTarjeta()
                            .replaceAll("\\s+", "");

            var resultado =
                    pasarelaPagoService.procesar(
                            tarjeta,
                            dto.getNombreTitular().trim(),
                            dto.getExpiracion(),
                            dto.getCvv(),
                            precioBase
                    );

            pago.setUltimos4(
                    tarjeta.substring(
                            tarjeta.length() - 4
                    )
            );

            pago.setReferenciaPasarela(
                    resultado.referencia()
            );

            if (resultado.aprobado()) {
                pago.setEstado("APROBADO");
            } else {
                pago.setEstado("RECHAZADO");
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

    private void validarTarjeta(
            PagoPresencialDTO dto
    ) {

        String tarjeta =
                dto.getNumeroTarjeta() == null
                        ? ""
                        : dto.getNumeroTarjeta()
                        .replaceAll("\\s+", "");

        if (!tarjeta.matches(
                "^[0-9]{13,19}$"
        )) {
            throw new IllegalArgumentException(
                    "El número de tarjeta debe contener entre 13 y 19 dígitos"
            );
        }

        if (!luhnValido(tarjeta)) {
            throw new IllegalArgumentException(
                    "El número de tarjeta no es válido"
            );
        }

        if (dto.getNombreTitular() == null
                || dto.getNombreTitular()
                .trim()
                .length() < 5) {

            throw new IllegalArgumentException(
                    "Nombre del titular no válido"
            );
        }

        validarExpiracion(
                dto.getExpiracion()
        );

        if (dto.getCvv() == null
                || !dto.getCvv()
                .matches("^[0-9]{3,4}$")) {

            throw new IllegalArgumentException(
                    "El CVV debe contener 3 o 4 dígitos"
            );
        }
    }

    private void validarExpiracion(
            String expiracion
    ) {

        if (expiracion == null
                || !expiracion.matches(
                "^(0[1-9]|1[0-2])/[0-9]{2}$"
        )) {

            throw new IllegalArgumentException(
                    "La expiración debe tener formato MM/YY"
            );
        }

        try {

            YearMonth fecha =
                    YearMonth.parse(
                            expiracion,
                            DateTimeFormatter.ofPattern(
                                    "MM/yy"
                            )
                    );

            if (fecha.isBefore(
                    YearMonth.now()
            )) {

                throw new IllegalArgumentException(
                        "La tarjeta está vencida"
                );
            }

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "La fecha de expiración no es válida"
            );
        }
    }

    private boolean luhnValido(
            String numero
    ) {

        int suma = 0;
        boolean duplicar = false;

        for (int i = numero.length() - 1;
             i >= 0;
             i--) {

            int digito =
                    numero.charAt(i) - '0';

            if (duplicar) {

                digito *= 2;

                if (digito > 9) {
                    digito -= 9;
                }
            }

            suma += digito;
            duplicar = !duplicar;
        }

        return suma % 10 == 0;
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