package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.service.PasarelaPagoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PasarelaPagoSimuladaServiceImpl
        implements PasarelaPagoService {

    @Override
    public ResultadoPago procesar(
            String numeroTarjeta,
            String titular,
            String expiracion,
            String cvv,
            BigDecimal monto
    ) {

        String referencia =
                "SIM-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        /*
         * Tarjetas especiales de prueba
         * para simular respuestas de la pasarela.
         */

        if ("4000000000009995".equals(
                numeroTarjeta
        )) {

            return new ResultadoPago(
                    false,
                    referencia,
                    "FONDOS_INSUFICIENTES"
            );
        }

        if ("4000000000000002".equals(
                numeroTarjeta
        )) {

            return new ResultadoPago(
                    false,
                    referencia,
                    "TARJETA_INVALIDA"
            );
        }

        if ("4000000000000069".equals(
                numeroTarjeta
        )) {

            return new ResultadoPago(
                    false,
                    referencia,
                    "TARJETA_VENCIDA"
            );
        }

        if ("4000000000000119".equals(
                numeroTarjeta
        )) {

            return new ResultadoPago(
                    false,
                    referencia,
                    "ERROR_COMUNICACION"
            );
        }

        /*
         * Cualquier otra tarjeta que haya
         * superado las validaciones del sistema
         * se considera aprobada.
         */

        return new ResultadoPago(
                true,
                referencia,
                "APROBADO"
        );
    }
}