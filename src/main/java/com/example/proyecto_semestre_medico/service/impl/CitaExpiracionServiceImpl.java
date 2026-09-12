package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Cita;
import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.repository.CitaRepository;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.service.CitaExpiracionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CitaExpiracionServiceImpl
        implements CitaExpiracionService {

    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;

    public CitaExpiracionServiceImpl(
            CitaRepository citaRepository,
            EstadoCitaRepository estadoCitaRepository
    ) {
        this.citaRepository = citaRepository;
        this.estadoCitaRepository = estadoCitaRepository;
    }

    @Override
    @Transactional
    public int expirarReservasVencidas() {

        LocalDateTime ahora =
                LocalDateTime.now();

        List<Cita> vencidas =
                citaRepository
                        .buscarReservasExpiradas(ahora);

        if (vencidas.isEmpty()) {
            return 0;
        }

        EstadoCita expirada =
                estadoCitaRepository
                        .findByCodigo("EXPIRADA")
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el estado EXPIRADA"
                                )
                        );

        for (Cita cita : vencidas) {

            cita.setEstadoCita(expirada);
            cita.setReservaExpiraEn(null);
        }

        citaRepository.saveAll(vencidas);

        return vencidas.size();
    }
}