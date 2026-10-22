package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.EventoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventoAgendaRepository
        extends JpaRepository<EventoAgenda, Long> {

    @Query("""
        SELECT COUNT(e) > 0
        FROM EventoAgenda e
        WHERE e.medico.id = :medicoId
          AND e.activo = true
          AND e.tipoEvento IN ('BLOQUEO', 'VACACIONES', 'CAPACITACION', 'REUNION', 'DESCANSO', 'OTRO')
          AND e.fechaInicio < :fechaFin
          AND e.fechaFin > :fechaInicio
    """)
    boolean existeBloqueoEnRango(
            @Param("medicoId") Long medicoId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("""
    SELECT e
    FROM EventoAgenda e
    JOIN FETCH e.medico m
    JOIN FETCH m.personal
    WHERE e.medico.id = :medicoId
      AND e.activo = true
      AND e.fechaInicio < :hasta
      AND e.fechaFin > :desde
    ORDER BY e.fechaInicio ASC
""")
    List<EventoAgenda> listarEnRango(
            @Param("medicoId") Long medicoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    Optional<EventoAgenda>
    findByIdAndMedicoIdAndActivoTrue(
            Long id,
            Long medicoId
    );

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventoAgenda e WHERE e.id = :id AND e.medico.id = :medicoId AND e.activo = true")
    Optional<EventoAgenda> buscarParaModificar(@Param("id") Long id, @Param("medicoId") Long medicoId);
}
