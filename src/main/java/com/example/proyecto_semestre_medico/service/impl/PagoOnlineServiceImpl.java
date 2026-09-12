package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.PagoOnlineDTO;
import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.model.Pago;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.repository.PagoRepository;
import com.example.proyecto_semestre_medico.service.PagoOnlineService;
import com.example.proyecto_semestre_medico.service.PasarelaPagoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.proyecto_semestre_medico.service.CorreoPendienteService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
public class PagoOnlineServiceImpl
        implements PagoOnlineService {

    private final PagoRepository pagoRepository;
    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final PasarelaPagoService pasarelaPagoService;
    private final CorreoPendienteService correoPendienteService;

    private final BigDecimal precioBase;

    public PagoOnlineServiceImpl(
            PagoRepository pagoRepository,
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            PasarelaPagoService pasarelaPagoService,
            CorreoPendienteService correoPendienteService,
            @Value("${app.citas.precio-base:250.00}")
            BigDecimal precioBase
    ) {
        this.pagoRepository = pagoRepository;
        this.citaRepository = citaRepository;
        this.estadoCitaRepository =
                estadoCitaRepository;
        this.pasarelaPagoService =
                pasarelaPagoService;
        this.precioBase = precioBase;
        this.correoPendienteService =
                correoPendienteService;
    }

    @Override
    @Transactional
    public Pago procesar(
            String nombreUsuarioPaciente,
            PagoOnlineDTO dto
    ) {

        validar(dto);

        Cita cita =
                citaRepository
                        .findById(dto.getCitaId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cita no encontrada"
                                )
                        );

        if (!cita.getPaciente()
                .getUsuario()
                .getNombreUsuario()
                .equalsIgnoreCase(
                        nombreUsuarioPaciente
                )) {

            throw new IllegalArgumentException(
                    "La cita no pertenece al paciente autenticado"
            );
        }

        if (!"PENDIENTE_PAGO".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "La cita ya no se encuentra pendiente de pago"
            );
        }

        if (cita.getReservaExpiraEn() == null
                || cita.getReservaExpiraEn()
                .isBefore(
                        LocalDateTime.now()
                )) {

            throw new IllegalArgumentException(
                    "La reserva de la cita ha expirado"
            );
        }

        Pago existente =
                pagoRepository
                        .findByClaveIdempotencia(
                                dto.getClaveIdempotencia()
                        )
                        .orElse(null);

        if (existente != null) {
            return existente;
        }

        String tarjeta =
                dto.getNumeroTarjeta()
                        .replaceAll("\\s+", "");

        String metodoPago =
                detectarMetodoPago(tarjeta);

        var resultado =
                pasarelaPagoService.procesar(
                        tarjeta,
                        dto.getNombreTitular().trim(),
                        dto.getExpiracion(),
                        dto.getCvv(),
                        precioBase
                );

        Pago pago = new Pago();

        pago.setCita(cita);
        pago.setNumeroTransaccion(
                generarNumeroTransaccion()
        );
        pago.setMonto(precioBase);
        pago.setMetodoPago(metodoPago);

        pago.setCanal(
                "ONLINE"
        );

        pago.setUltimos4(
                tarjeta.substring(
                        tarjeta.length() - 4
                )
        );

        pago.setReferenciaPasarela(
                resultado.referencia()
        );

        pago.setClaveIdempotencia(
                dto.getClaveIdempotencia()
        );

        if (resultado.aprobado()) {

            pago.setEstado("APROBADO");

            EstadoCita confirmada =
                    estadoCitaRepository
                            .findByCodigo(
                                    "CONFIRMADA"
                            )
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "No existe el estado CONFIRMADA"
                                    )
                            );

            cita.setEstadoCita(confirmada);
            cita.setReservaExpiraEn(null);

            citaRepository.save(cita);

        } else {

            pago.setEstado("RECHAZADO");
        }

        Pago pagoGuardado =
                pagoRepository.save(pago);

        if ("APROBADO".equals(
                pagoGuardado.getEstado()
        )) {

            registrarCorreoConfirmacion(
                    pagoGuardado
            );
        }

        return pagoGuardado;
    }

    private void validar(
            PagoOnlineDTO dto
    ) {

        if (dto == null
                || dto.getCitaId() == null) {

            throw new IllegalArgumentException(
                    "Cita requerida"
            );
        }

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
                || !dto.getNombreTitular()
                .trim()
                .matches(
                        "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{5,100}$"
                )) {

            throw new IllegalArgumentException(
                    "El nombre del titular no es válido"
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

        if (dto.getClaveIdempotencia() == null
                || dto.getClaveIdempotencia()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Clave de operación requerida"
            );
        }
    }

    private void registrarCorreoConfirmacion(
            Pago pago
    ) {

        Cita cita =
                pago.getCita();

        String destinatario =
                cita.getPaciente()
                        .getCorreo();

        String asunto =
                "Confirmación de pago - "
                        + cita.getNumeroCita();

        String contenido =
                """
                Hospital
    
                Su pago fue procesado correctamente.
    
                Número de cita: %s
                Número de transacción: %s
                Paciente: %s
                Monto: Q%s
                Método de pago: %s
                Fecha de cita: %s
                Especialidad: %s
                Sucursal: %s
    
                Gracias por utilizar nuestros servicios.
                """
                        .formatted(
                                cita.getNumeroCita(),
                                pago.getNumeroTransaccion(),
                                cita.getPaciente()
                                        .getNombreCompleto(),
                                pago.getMonto(),
                                pago.getMetodoPago(),
                                cita.getFechaHoraInicio(),
                                cita.getEspecialidad()
                                        .getNombre(),
                                cita.getSucursal()
                                        .getNombre()
                        );

        correoPendienteService.registrar(
                destinatario,
                asunto,
                contenido
        );
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

    private String detectarMetodoPago(
            String numero
    ) {

        if (numero.startsWith("4")) {
            return "VISA";
        }

        if (numero.startsWith("5")) {
            return "MASTERCARD";
        }

        return "DEBITO";
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
                        .existsByNumeroTransaccion(
                                numero
                        )
        );

        return numero;
    }
}