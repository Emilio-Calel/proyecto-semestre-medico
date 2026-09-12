package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.EventoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EventoAgendaRepository
        extends JpaRepository<EventoAgenda, Long> {

    @Query("""
        SELECT COUNT(e) > 0
        FROM EventoAgenda e
        WHERE e.medico.id = :medicoId
          AND e.activo = true
          AND e.fechaInicio < :fechaFin
          AND e.fechaFin > :fechaInicio
    """)
    boolean existeBloqueoEnRango(
            @Param("medicoId") Long medicoId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}