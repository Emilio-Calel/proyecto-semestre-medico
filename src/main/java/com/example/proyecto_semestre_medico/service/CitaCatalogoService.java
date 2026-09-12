package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO;
import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.Sucursal;

import java.util.List;

public interface CitaCatalogoService {

    List<Sucursal> listarSucursales();

    List<Especialidad> listarEspecialidades(
            Long sucursalId
    );

    List<MedicoOpcionDTO> listarMedicos(
            Long sucursalId,
            Long especialidadId
    );
}