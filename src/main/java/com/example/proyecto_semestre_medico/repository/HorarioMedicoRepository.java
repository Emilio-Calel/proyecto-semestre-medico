package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.HorarioMedico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioMedicoRepository
        extends JpaRepository<HorarioMedico, Long> {

    List<HorarioMedico>
    findByMedicoIdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
            Long medicoId,
            String diaSemana
    );
}