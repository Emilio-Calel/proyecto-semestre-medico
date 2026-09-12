package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    List<Especialidad> findByActivoTrueOrderByNombreAsc();
}