package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.DocumentoCita;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoCitaRepository
        extends JpaRepository<DocumentoCita, Long> {
}