package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.PagoOnlineDTO;
import com.example.proyecto_semestre_medico.model.Pago;

public interface PagoOnlineService {

    Pago procesar(
            String nombreUsuarioPaciente,
            PagoOnlineDTO dto
    );
}