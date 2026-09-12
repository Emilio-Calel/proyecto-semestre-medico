package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.HorarioDisponibleDTO;
import com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO;
import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.service.CitaCatalogoService;
import com.example.proyecto_semestre_medico.service.CitaDisponibilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaApiController {

    private final CitaCatalogoService citaCatalogoService;
    private final CitaDisponibilidadService citaDisponibilidadService;

    public CitaApiController(
            CitaCatalogoService citaCatalogoService,
            CitaDisponibilidadService citaDisponibilidadService
    ) {
        this.citaCatalogoService = citaCatalogoService;
        this.citaDisponibilidadService = citaDisponibilidadService;
    }

    @GetMapping("/especialidades")
    public List<Especialidad> listarEspecialidades(
            @RequestParam Long sucursalId
    ) {
        return citaCatalogoService
                .listarEspecialidades(sucursalId);
    }

    @GetMapping("/medicos")
    public List<MedicoOpcionDTO> listarMedicos(
            @RequestParam Long sucursalId,
            @RequestParam Long especialidadId
    ) {
        return citaCatalogoService
                .listarMedicos(
                        sucursalId,
                        especialidadId
                );
    }

    @GetMapping("/horarios")
    public List<HorarioDisponibleDTO> listarHorarios(
            @RequestParam Long medicoId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha
    ) {
        return citaDisponibilidadService
                .obtenerHorarios(
                        medicoId,
                        fecha
                );
    }
}