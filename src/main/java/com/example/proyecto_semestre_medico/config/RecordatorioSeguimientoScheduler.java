package com.example.proyecto_semestre_medico.config;

import com.example.proyecto_semestre_medico.service.RecordatorioSeguimientoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecordatorioSeguimientoScheduler {

    private final RecordatorioSeguimientoService
            recordatorioSeguimientoService;

    public RecordatorioSeguimientoScheduler(
            RecordatorioSeguimientoService
                    recordatorioSeguimientoService
    ) {

        this.recordatorioSeguimientoService =
                recordatorioSeguimientoService;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.seguimiento.recordatorio-check-ms:3600000}"
    )
    public void procesar() {

        int cantidad =
                recordatorioSeguimientoService
                        .procesarRecordatorios();

        if (cantidad > 0) {

            System.out.println(
                    ">>> RECORDATORIOS DE SEGUIMIENTO REGISTRADOS: "
                            + cantidad
            );
        }
    }
}