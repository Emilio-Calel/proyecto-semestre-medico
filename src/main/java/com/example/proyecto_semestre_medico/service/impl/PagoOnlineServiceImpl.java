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
    private final String nombreHospital;
    private final String telefonoHospital;

    private final BigDecimal precioBase;

    public PagoOnlineServiceImpl(
            PagoRepository pagoRepository,
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository,
            PasarelaPagoService pasarelaPagoService,
            CorreoPendienteService correoPendienteService,
            @Value("${app.citas.precio-base:250.00}")
            BigDecimal precioBase,
            @Value("${app.hospital.nombre:Hospital}")
            String nombreHospital,
            @Value("${app.hospital.telefono:}")
            String telefonoHospital
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
        this.nombreHospital =
                nombreHospital;
        this.telefonoHospital =
                telefonoHospital;
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

        // =====================================================
// IDEMPOTENCIA
// =====================================================

        Pago existente =
                pagoRepository
                        .findByClaveIdempotencia(
                                dto.getClaveIdempotencia()
                        )
                        .orElse(null);

        if (existente != null) {

            /*
             * La misma clave nunca debe poder utilizarse
             * para una cita diferente.
             */
            if (!existente.getCita()
                    .getId()
                    .equals(
                            cita.getId()
                    )) {

                throw new IllegalArgumentException(
                        "La clave de operación ya fue utilizada."
                );
            }

            /*
             * Si esta misma operación ya fue procesada,
             * devolvemos exactamente el pago existente.
             *
             * No volvemos a llamar a la pasarela.
             * No generamos otra transacción.
             */
            return existente;
        }

// =====================================================
// ESTADO DE LA CITA
// =====================================================

        if (!"PENDIENTE_PAGO".equals(
                cita.getEstadoCita()
                        .getCodigo()
        )) {

            throw new IllegalArgumentException(
                    "La cita ya no se encuentra pendiente de pago"
            );
        }

// =====================================================
// VIGENCIA DE LA RESERVA
// =====================================================

        if (cita.getReservaExpiraEn() == null
                || !cita.getReservaExpiraEn()
                .isAfter(
                        LocalDateTime.now()
                )) {

            throw new IllegalArgumentException(
                    "Su sesión de pago ha expirado. Por favor, inicie el proceso de pago nuevamente."
            );
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

            pago.setEstado(
                    "APROBADO"
            );

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

            cita.setEstadoCita(
                    confirmada
            );

            cita.setReservaExpiraEn(
                    null
            );

            citaRepository.save(
                    cita
            );

        } else {

            pago.setEstado(
                    "RECHAZADO"
            );

            pago.setMensajePasarela(
                    obtenerMensajeRechazo(
                            resultado.codigoResultado()
                    )
            );
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

        // =====================================================
        // RN-CU04-01 NÚMERO DE TARJETA
        // =====================================================

        String tarjeta =
                dto.getNumeroTarjeta() == null
                        ? ""
                        : dto.getNumeroTarjeta()
                        .replaceAll("\\s+", "");

        if (!tarjeta.matches(
                "^[0-9]{13,19}$"
        )
                || !luhnValido(tarjeta)) {

            throw new IllegalArgumentException(
                    "El número de tarjeta debe contener entre 13 y 19 dígitos y ser válido."
            );
        }

        // =====================================================
        // RN-CU04-02 NOMBRE TITULAR
        // =====================================================

        String titular =
                dto.getNombreTitular() == null
                        ? ""
                        : dto.getNombreTitular()
                        .trim();

        if (!titular.matches(
                "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{5,100}$"
        )) {

            throw new IllegalArgumentException(
                    "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales."
            );
        }

        // =====================================================
        // RN-CU04-03 FECHA VENCIMIENTO
        // =====================================================

        validarExpiracion(
                dto.getExpiracion()
        );

        // =====================================================
        // RN-CU04-04 CVV
        // =====================================================

        if (dto.getCvv() == null
                || !dto.getCvv()
                .matches(
                        "^[0-9]{3,4}$"
                )) {

            throw new IllegalArgumentException(
                    "El CVV debe contener 3 ó 4 dígitos numéricos."
            );
        }

        // =====================================================
        // IDEMPOTENCIA
        // =====================================================

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
                "Comprobante de Pago - Cita Médica - Hospital "
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
                        color:#1f2933;
                ">
    
                <div style="
                        width:100%%;
                        padding:32px 0;
                        background:#f4f8f9;
                ">
    
                    <div style="
                            max-width:650px;
                            margin:0 auto;
                            background:#ffffff;
                            border-radius:18px;
                            overflow:hidden;
                            box-shadow:0 8px 24px rgba(30,70,80,0.10);
                    ">
    
                        <div style="
                                padding:28px 32px;
                                background:linear-gradient(
                                        135deg,
                                        #0f6f7a,
                                        #1596a3
                                );
                                color:#ffffff;
                        ">
    
                            <div style="
                                    font-size:22px;
                                    font-weight:bold;
                                    margin-bottom:5px;
                            ">
                                ✚ %s
                            </div>
    
                            <div style="
                                    font-size:13px;
                                    color:rgba(255,255,255,0.85);
                            ">
                                Sistema Informático Hospitalario
                            </div>
    
                        </div>
    
                        <div style="
                                padding:34px 32px;
                        ">
    
                            <div style="
                                    width:58px;
                                    height:58px;
                                    line-height:58px;
                                    text-align:center;
                                    border-radius:50%%;
                                    background:#e2f6ea;
                                    color:#247447;
                                    font-size:28px;
                                    font-weight:bold;
                                    margin:0 auto 18px;
                            ">
                                ✓
                            </div>
    
                            <h2 style="
                                    margin:0 0 10px;
                                    text-align:center;
                                    color:#1f4f35;
                                    font-size:25px;
                            ">
                                ¡Pago realizado exitosamente!
                            </h2>
    
                            <p style="
                                    margin:0 0 28px;
                                    text-align:center;
                                    color:#697b81;
                                    font-size:14px;
                                    line-height:1.6;
                            ">
                                Su cita médica ha sido confirmada.
                                A continuación encontrará el
                                comprobante de su transacción.
                            </p>
    
                            <div style="
                                    background:#f7fafb;
                                    border:1px solid #e1eaec;
                                    border-radius:14px;
                                    padding:22px;
                            ">
    
                                <div style="
                                        font-size:13px;
                                        color:#7b898f;
                                        margin-bottom:5px;
                                ">
                                    Número de transacción
                                </div>
    
                                <div style="
                                        font-size:18px;
                                        font-weight:bold;
                                        color:#0f6f7a;
                                        margin-bottom:20px;
                                ">
                                    %s
                                </div>
    
                                <div style="
                                        border-top:1px solid #e4ecee;
                                        padding-top:16px;
                                        line-height:1.9;
                                        font-size:14px;
                                ">
    
                                    <strong>Número de cita:</strong>
                                    %s
                                    <br>
    
                                    <strong>Paciente:</strong>
                                    %s
                                    <br>
    
                                    <strong>Médico:</strong>
                                    %s
                                    <br>
    
                                    <strong>Especialidad:</strong>
                                    %s
                                    <br>
    
                                    <strong>Sucursal:</strong>
                                    %s
                                    <br>
    
                                    <strong>Fecha y hora de cita:</strong>
                                    %s
                                    <br>
    
                                    <strong>Método de pago:</strong>
                                    %s
                                    <br>
    
                                    <strong>Tarjeta terminada en:</strong>
                                    **** %s
                                    <br>
    
                                    <strong>Fecha y hora de pago:</strong>
                                    %s
    
                                </div>
    
                            </div>
    
                            <div style="
                                    margin-top:22px;
                                    padding:18px 20px;
                                    border-radius:12px;
                                    background:#eaf7f8;
                                    border:1px solid #cfe6e9;
                                    text-align:center;
                            ">
    
                                <span style="
                                        display:block;
                                        color:#68787e;
                                        font-size:13px;
                                        margin-bottom:5px;
                                ">
                                    Total pagado
                                </span>
    
                                <strong style="
                                        color:#0f6f7a;
                                        font-size:27px;
                                ">
                                    Q%s
                                </strong>
    
                            </div>
    
                            <p style="
                                    margin:24px 0 0;
                                    color:#718087;
                                    font-size:13px;
                                    line-height:1.6;
                                    text-align:center;
                            ">
                                Conserve este correo como comprobante
                                de su pago y confirmación de cita.
                            </p>
    
                        </div>
    
                        <div style="
                                padding:22px 30px;
                                background:#f3f7f8;
                                border-top:1px solid #e1eaec;
                                text-align:center;
                                color:#849298;
                                font-size:11px;
                                line-height:1.6;
                        ">
    
                            Este es un correo automático del Sistema
                            Informático Hospitalario. No responda a este mensaje.
                            Para consultas, comuníquese al teléfono %s.
    
                        </div>
    
                    </div>
    
                </div>
    
                </body>
                </html>
                """
                        .formatted(
                                nombreHospital,
                                pago.getNumeroTransaccion(),
                                cita.getNumeroCita(),
                                cita.getPaciente()
                                        .getNombreCompleto(),
                                cita.getMedico()
                                        .getPersonal()
                                        .getNombreCompleto(),
                                cita.getEspecialidad()
                                        .getNombre(),
                                cita.getSucursal()
                                        .getNombre(),
                                cita.getFechaHoraInicio(),
                                pago.getMetodoPago(),
                                pago.getUltimos4(),
                                pago.getFechaPago(),
                                pago.getMonto(),
                                telefonoHospital
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

        String mensaje =
                "La fecha de vencimiento debe estar en formato MM/AA y la tarjeta no debe estar vencida.";

        if (expiracion == null
                || !expiracion.matches(
                "^(0[1-9]|1[0-2])/[0-9]{2}$"
        )) {

            throw new IllegalArgumentException(
                    mensaje
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
                        mensaje
                );
            }

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    mensaje
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

    private String obtenerMensajeRechazo(
            String codigo
    ) {

        if (codigo == null) {

            return "El pago no pudo ser procesado. Por favor, intente nuevamente o utilice otra tarjeta.";
        }

        return switch (codigo) {

            case "FONDOS_INSUFICIENTES" ->
                    "Su tarjeta fue rechazada por fondos insuficientes. Verifique su saldo e intente nuevamente.";

            case "TARJETA_INVALIDA" ->
                    "Su tarjeta fue rechazada. El número de tarjeta es inválido. Verifique los datos e intente nuevamente.";

            case "TARJETA_VENCIDA" ->
                    "Su tarjeta fue rechazada. La tarjeta está vencida. Utilice otra tarjeta de crédito o débito.";

            case "ERROR_COMUNICACION" ->
                    "Error al procesar el pago. Por favor, intente nuevamente o contacte a su banco.";

            default ->
                    "El pago no pudo ser procesado. Por favor, intente nuevamente o utilice otra tarjeta.";
        };
    }
}