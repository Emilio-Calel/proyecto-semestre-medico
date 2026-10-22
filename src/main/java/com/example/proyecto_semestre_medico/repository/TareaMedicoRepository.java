package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.TareaMedico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TareaMedicoRepository
        extends JpaRepository<TareaMedico, Long> {

    List<TareaMedico>
    findByMedicoIdAndActivoTrueOrderByCompletadaAscFechaLimiteAsc(
            Long medicoId
    );

    List<TareaMedico>
    findByMedicoIdAndActivoTrueAndCompletadaOrderByFechaLimiteAsc(
            Long medicoId,
            Boolean completada
    );

    Optional<TareaMedico>
    findByIdAndMedicoIdAndActivoTrue(
            Long id,
            Long medicoId
    );

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TareaMedico t WHERE t.id = :id AND t.medico.id = :medicoId AND t.activo = true")
    Optional<TareaMedico> buscarParaModificar(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("medicoId") Long medicoId);
}
