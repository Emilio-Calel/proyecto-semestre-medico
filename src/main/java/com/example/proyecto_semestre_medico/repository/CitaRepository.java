package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import com.example.proyecto_semestre_medico.model.Cita;

public interface CitaRepository
        extends JpaRepository<Cita, Long> {

    boolean existsByNumeroCita(
            String numeroCita
    );

    @Query("""
        SELECT COUNT(c) > 0
        FROM Cita c
        WHERE c.medico.id = :medicoId

          AND c.estadoCita.codigo NOT IN (
              'CANCELADA',
              'EXPIRADA',
              'NO_ASISTIO'
          )

          AND c.fechaHoraInicio < :fechaFin
          AND c.fechaHoraFin > :fechaInicio
    """)
    boolean existeCitaEnRango(
            @Param("medicoId")
            Long medicoId,

            @Param("fechaInicio")
            LocalDateTime fechaInicio,

            @Param("fechaFin")
            LocalDateTime fechaFin
    );

    @Query("""
    SELECT c
    FROM Cita c
    WHERE c.estadoCita.codigo = 'PENDIENTE_PAGO'
      AND c.reservaExpiraEn IS NOT NULL
      AND c.reservaExpiraEn <= :ahora
""")
    List<Cita> buscarReservasExpiradas(
            @Param("ahora") LocalDateTime ahora
    );

    Optional<Cita> findByNumeroCitaIgnoreCase(
            String numeroCita
    );

    @Query("""
    SELECT c
    FROM Cita c
    JOIN FETCH c.paciente p
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.medico m
    JOIN FETCH m.personal
    JOIN FETCH c.estadoCita
    WHERE p.dpiHash = :dpiHash
    ORDER BY c.fechaHoraInicio DESC
""")
    List<Cita> buscarPorDpiHash(
            @Param("dpiHash") String dpiHash
    );

    @Query("""
    SELECT c
    FROM Cita c
    JOIN FETCH c.paciente
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.medico m
    JOIN FETCH m.personal
    JOIN FETCH c.estadoCita
    WHERE c.estadoCita.codigo = 'PACIENTE_PRESENTE'
    ORDER BY c.fechaHoraInicio ASC
""")
    List<Cita> listarPacientesPresentes();

    @Query("""
    SELECT c
    FROM Cita c
    JOIN FETCH c.paciente
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.medico m
    JOIN FETCH m.personal
    JOIN FETCH c.estadoCita
    WHERE c.estadoCita.codigo IN (
        'PACIENTE_PRESENTE',
        'SIGNOS_VITALES'
    )
    ORDER BY c.fechaHoraInicio ASC
""")
    List<Cita> listarPacientesParaEnfermeria();


    @Query("""
    SELECT c
    FROM Cita c
    JOIN FETCH c.paciente
    JOIN FETCH c.sucursal
    JOIN FETCH c.especialidad
    JOIN FETCH c.medico m
    JOIN FETCH m.personal p
    JOIN FETCH c.estadoCita
    WHERE c.id = :citaId
""")
    Optional<Cita> buscarCitaCompletaPorId(
            @Param("citaId") Long citaId
    );
    List<Cita> findByEstadoCitaCodigoOrderByFechaHoraInicioAsc(
            String codigo
    );
    List<Cita>
    findByMedicoIdAndEstadoCitaCodigoOrderByFechaHoraInicioAsc(
            Long medicoId,
            String codigo
    );

    @Query("""
    SELECT c
    FROM Cita c
    JOIN FETCH c.paciente
    JOIN FETCH c.medico m
    JOIN FETCH m.personal
    JOIN FETCH c.especialidad
    JOIN FETCH c.sucursal
    JOIN FETCH c.estadoCita
    WHERE c.origen = 'SEGUIMIENTO'
      AND c.recordatorioSeguimientoEnviado = false
      AND c.fechaHoraInicio >= :desde
      AND c.fechaHoraInicio < :hasta
      AND c.estadoCita.codigo NOT IN (
          'CANCELADA',
          'EXPIRADA',
          'NO_ASISTIO'
      )
    ORDER BY c.fechaHoraInicio ASC
""")
    List<Cita> buscarSeguimientosParaRecordatorio(
            @Param("desde")
            LocalDateTime desde,

            @Param("hasta")
            LocalDateTime hasta
    );
}



