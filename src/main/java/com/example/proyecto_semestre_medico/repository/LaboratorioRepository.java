package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaboratorioRepository
        extends JpaRepository<Laboratorio, Long> {

    List<Laboratorio>
    findAllByOrderByNombreAsc();

    List<Laboratorio>
    findByActivoTrueOrderByNombreAsc();

    Optional<Laboratorio>
    findByNombreIgnoreCase(
            String nombre
    );
}