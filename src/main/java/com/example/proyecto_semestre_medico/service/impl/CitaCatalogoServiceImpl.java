package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO;
import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.repository.MedicoRepository;
import com.example.proyecto_semestre_medico.repository.SucursalEspecialidadRepository;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.service.CitaCatalogoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Service
public class CitaCatalogoServiceImpl
        implements CitaCatalogoService {

    private final SucursalRepository sucursalRepository;
    private final SucursalEspecialidadRepository sucursalEspecialidadRepository;
    private final MedicoRepository medicoRepository;

    public CitaCatalogoServiceImpl(
            SucursalRepository sucursalRepository,
            SucursalEspecialidadRepository sucursalEspecialidadRepository,
            MedicoRepository medicoRepository
    ) {
        this.sucursalRepository = sucursalRepository;
        this.sucursalEspecialidadRepository =
                sucursalEspecialidadRepository;
        this.medicoRepository = medicoRepository;
    }

    @Override
    @Cacheable("citaSucursalesActivas")
    @Transactional(readOnly = true)
    public List<Sucursal> listarSucursales() {

        return sucursalRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Cacheable(
            value = "citaEspecialidadesPorSucursal",
            key = "#sucursalId"
    )
    @Transactional(readOnly = true)
    public List<Especialidad> listarEspecialidades(
            Long sucursalId
    ) {

        if (sucursalId == null) {

            throw new IllegalArgumentException(
                    "Sucursal requerida"
            );
        }

        return sucursalEspecialidadRepository
                .listarEspecialidadesActivasPorSucursal(
                        sucursalId
                );
    }

    @Override
    @Cacheable(
            value = "citaMedicosPorSucursalEspecialidad",
            key = "#sucursalId + '-' + #especialidadId"
    )
    @Transactional(readOnly = true)
    public List<MedicoOpcionDTO> listarMedicos(
            Long sucursalId,
            Long especialidadId
    ) {

        if (sucursalId == null
                || especialidadId == null) {

            throw new IllegalArgumentException(
                    "Sucursal y especialidad son requeridas"
            );
        }

        boolean relacionValida =
                sucursalEspecialidadRepository
                        .listarEspecialidadesActivasPorSucursal(sucursalId)
                        .stream().anyMatch(e -> e.getId().equals(especialidadId));

        if (!relacionValida) {

            throw new IllegalArgumentException(
                    "La especialidad no está disponible en la sucursal"
            );
        }

        return medicoRepository
                .listarDisponiblesParaCita(
                        sucursalId,
                        especialidadId
                );
    }
}
