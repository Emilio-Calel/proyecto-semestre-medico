package com.example.proyecto_semestre_medico.service;

import java.math.BigDecimal;

public interface PasarelaPagoService {

    ResultadoPago procesar(
            String numeroTarjeta,
            String titular,
            String expiracion,
            String cvv,
            BigDecimal monto
    );

    record ResultadoPago(
            boolean aprobado,
            String referencia
    ) {
    }
}