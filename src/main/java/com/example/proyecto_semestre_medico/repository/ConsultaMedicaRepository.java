package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.ConsultaMedica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultaMedicaRepository
        extends JpaRepository<ConsultaMedica, Long> {

    Optional<ConsultaMedica> findByCitaId(Long citaId);

    boolean existsByCitaId(Long citaId);
}