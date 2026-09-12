package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}