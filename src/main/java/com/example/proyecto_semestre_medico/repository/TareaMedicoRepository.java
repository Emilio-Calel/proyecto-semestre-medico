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
}