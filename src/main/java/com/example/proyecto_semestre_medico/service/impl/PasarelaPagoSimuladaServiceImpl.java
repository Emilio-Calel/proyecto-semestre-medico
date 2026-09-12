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

        /*
         * Simulación para proyecto académico.
         *
         * Regla:
         * si la tarjeta termina en 0000,
         * se rechaza.
         *
         * Cualquier otra tarjeta válida
         * se aprueba.
         */

        boolean aprobado =
                !numeroTarjeta.endsWith("0000");

        String referencia =
                "SIM-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        return new ResultadoPago(
                aprobado,
                referencia
        );
    }
}