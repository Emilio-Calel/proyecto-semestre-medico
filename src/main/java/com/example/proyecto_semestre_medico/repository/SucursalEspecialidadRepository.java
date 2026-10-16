package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.model.SucursalEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SucursalEspecialidadRepository
        extends JpaRepository<SucursalEspecialidad, Long> {

    boolean existsBySucursalIdAndEspecialidadIdAndActivoTrue(
            Long sucursalId,
            Long especialidadId
    );

    Optional<SucursalEspecialidad>
    findBySucursalIdAndEspecialidadId(
            Long sucursalId,
            Long especialidadId
    );

    List<SucursalEspecialidad>
    findAllByOrderByIdDesc();

    @Query("""
        SELECT se
        FROM SucursalEspecialidad se
        JOIN FETCH se.sucursal s
        JOIN FETCH se.especialidad e
        WHERE (:id IS NULL OR se.id = :id)
        ORDER BY se.id DESC
    """)
    List<SucursalEspecialidad> buscar(
            @Param("id") Long id
    );

    @Query("""
        SELECT se.especialidad
        FROM SucursalEspecialidad se
        WHERE se.sucursal.id = :sucursalId
          AND se.activo = true
          AND se.sucursal.activo = true
          AND se.especialidad.activo = true
        ORDER BY se.especialidad.nombre ASC
    """)
    List<Especialidad>
    listarEspecialidadesActivasPorSucursal(
            @Param("sucursalId") Long sucursalId
    );
}