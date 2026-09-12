package com.example.proyecto_semestre_medico.config;

import com.example.proyecto_semestre_medico.service.CitaExpiracionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CitaExpiracionScheduler {

    private final CitaExpiracionService citaExpiracionService;

    public CitaExpiracionScheduler(
            CitaExpiracionService citaExpiracionService
    ) {
        this.citaExpiracionService =
                citaExpiracionService;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.citas.expiracion-check-ms:60000}"
    )
    public void expirarReservas() {

        int cantidad =
                citaExpiracionService
                        .expirarReservasVencidas();

        if (cantidad > 0) {

            System.out.println(
                    ">>> CITAS EXPIRADAS AUTOMÁTICAMENTE: "
                            + cantidad
            );
        }
    }
}