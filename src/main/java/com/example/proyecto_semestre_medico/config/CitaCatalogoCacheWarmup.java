package com.example.proyecto_semestre_medico.config;

import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.service.CitaCatalogoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CitaCatalogoCacheWarmup
        implements ApplicationRunner {

    private final CitaCatalogoService
            citaCatalogoService;

    public CitaCatalogoCacheWarmup(
            CitaCatalogoService citaCatalogoService
    ) {

        this.citaCatalogoService =
                citaCatalogoService;
    }

    @Override
    public void run(
            ApplicationArguments args
    ) {

        // ============================================
        // PRECARGAR SUCURSALES ACTIVAS
        // ============================================

        List<Sucursal> sucursales =
                citaCatalogoService
                        .listarSucursales();

        // ============================================
        // PRECARGAR ESPECIALIDADES POR SUCURSAL
        // ============================================

        for (Sucursal sucursal : sucursales) {

            List<Especialidad> especialidades =
                    citaCatalogoService
                            .listarEspecialidades(
                                    sucursal.getId()
                            );

            // ========================================
            // PRECARGAR MÉDICOS POR COMBINACIÓN
            // ========================================

            for (Especialidad especialidad
                    : especialidades) {

                citaCatalogoService
                        .listarMedicos(
                                sucursal.getId(),
                                especialidad.getId()
                        );
            }
        }
    }
}