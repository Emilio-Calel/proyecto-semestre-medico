package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.PagoPresencialDTO;
import com.example.proyecto_semestre_medico.model.Pago;

public interface PagoPresencialService {

    Pago procesar(
            String usuarioCajero,
            PagoPresencialDTO dto
    );
}