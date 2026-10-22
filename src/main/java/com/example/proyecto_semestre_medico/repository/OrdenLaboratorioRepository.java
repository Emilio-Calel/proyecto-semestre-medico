package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.OrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdenLaboratorioRepository
        extends JpaRepository<OrdenLaboratorio, Long> {

    Optional<OrdenLaboratorio> findByNumeroOrden(String numeroOrden);

    // Serializa registro/publicación de una misma orden, incluido el último examen.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrdenLaboratorio o WHERE o.id = :id")
    Optional<OrdenLaboratorio> buscarParaProcesar(@Param("id") Long id);

    List<OrdenLaboratorio> findByEstadoOrderByFechaCreacionDesc(
            String estado
    );

    List<OrdenLaboratorio> findByPacienteIdOrderByFechaCreacionDesc(
            Long pacienteId
    );

    List<OrdenLaboratorio> findByMedicoIdOrderByFechaCreacionDesc(
            Long medicoId
    );

    List<OrdenLaboratorio> findByPacienteIdAndEstadoOrderByFechaCreacionDesc(
            Long pacienteId,
            String estado
    );

    List<OrdenLaboratorio> findByMedicoIdAndEstadoOrderByFechaCreacionDesc(
            Long medicoId,
            String estado
    );

    List<OrdenLaboratorio>
    findByPacienteDpiHashAndEstadoAndExternaFalseOrderByFechaCreacionDesc(
            String dpiHash,
            String estado
    );

    @Query("""
        SELECT o
        FROM OrdenLaboratorio o
        JOIN o.paciente p
        JOIN o.medico m
        JOIN m.personal mp
        WHERE
            (:estado IS NULL
                OR :estado = ''
                OR o.estado = :estado)
        AND
            (:paciente IS NULL
                OR :paciente = ''
                OR LOWER(p.nombreCompleto)
                   LIKE LOWER(CONCAT('%', :paciente, '%')))
        AND
            (:medico IS NULL
                OR :medico = ''
                OR LOWER(mp.nombreCompleto)
                   LIKE LOWER(CONCAT('%', :medico, '%')))
        ORDER BY o.fechaCreacion DESC
        """)
    List<OrdenLaboratorio> buscar(
            @Param("estado") String estado,
            @Param("paciente") String paciente,
            @Param("medico") String medico
    );
}
