package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.OrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenLaboratorioRepository
        extends JpaRepository<OrdenLaboratorio, Long> {

    Optional<OrdenLaboratorio> findByNumeroOrden(String numeroOrden);

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
}