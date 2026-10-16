package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.EstadoReceta;
import com.example.proyecto_semestre_medico.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecetaRepository
        extends JpaRepository<Receta, Long> {

    List<Receta>
    findAllByOrderByFechaEmisionDesc();

    List<Receta>
    findByEstadoOrderByFechaEmisionDesc(
            EstadoReceta estado
    );

    Optional<Receta>
    findByIdAndActivoTrue(
            Long id
    );

    List<Receta>
    findByConsultaMedicaIdOrderByFechaEmisionDesc(
            Long consultaMedicaId
    );
}