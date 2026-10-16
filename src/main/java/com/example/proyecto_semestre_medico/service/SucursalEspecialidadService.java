package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.model.SucursalEspecialidad;

import java.util.List;

public interface SucursalEspecialidadService {

    List<SucursalEspecialidad> listar(
            Long id
    );

    List<Sucursal> listarSucursalesActivas();

    List<Especialidad> listarEspecialidadesActivas();

    SucursalEspecialidad asignar(
            Long sucursalId,
            Long especialidadId,
            String nombreUsuario
    );

    void desactivar(
            Long id,
            String nombreUsuario
    );
}