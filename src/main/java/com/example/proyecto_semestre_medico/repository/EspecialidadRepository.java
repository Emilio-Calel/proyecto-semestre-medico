package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EspecialidadRepository
        extends JpaRepository<Especialidad, Long> {

    List<Especialidad>
    findByActivoTrueOrderByNombreAsc();

    List<Especialidad>
    findAllByOrderByNombreAsc();

    Optional<Especialidad>
    findFirstByNombreIgnoreCaseAndActivoTrue(
            String nombre
    );

    Optional<Especialidad>
    findFirstByNombreIgnoreCaseAndActivoFalse(
            String nombre
    );
}