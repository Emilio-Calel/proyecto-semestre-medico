package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.SignosVitales;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignosVitalesRepository
        extends JpaRepository<SignosVitales, Long> {

    boolean existsByCitaId(
            Long citaId
    );

    Optional<SignosVitales> findByCitaId(
            Long citaId
    );
}