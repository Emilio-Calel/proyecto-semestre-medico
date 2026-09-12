package com.example.proyecto_semestre_medico.config;

import com.example.proyecto_semestre_medico.service.CorreoPendienteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CorreoScheduler {

    private final CorreoPendienteService correoPendienteService;

    public CorreoScheduler(
            CorreoPendienteService correoPendienteService
    ) {
        this.correoPendienteService =
                correoPendienteService;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.correo.check-ms:60000}"
    )
    public void procesarCorreos() {

        int enviados =
                correoPendienteService
                        .procesarPendientes();

        if (enviados > 0) {

            System.out.println(
                    ">>> CORREOS PROCESADOS: "
                            + enviados
            );
        }
    }
}
