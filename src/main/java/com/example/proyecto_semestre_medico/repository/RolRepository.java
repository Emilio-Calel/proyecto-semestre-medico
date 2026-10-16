package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository
        extends JpaRepository<Rol, Long> {

    List<Rol> findByActivoTrueOrderByNombreAsc();

    List<Rol> findByActivoTrueAndCodigoNotOrderByNombreAsc(
            String codigo
    );

    List<Rol> findAllByOrderByNombreAsc();

    Optional<Rol> findByNombreIgnoreCase(
            String nombre
    );

    Optional<Rol> findByCodigo(
            String codigo
    );

    Optional<Rol> findByCodigoIgnoreCase(
            String codigo
    );
}