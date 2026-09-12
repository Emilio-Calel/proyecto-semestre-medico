package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoCitaRepository
        extends JpaRepository<EstadoCita, Long> {

    Optional<EstadoCita> findByCodigo(String codigo);

    List<EstadoCita> findByActivoTrueOrderByNombreAsc();
}