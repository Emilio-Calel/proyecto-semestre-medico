package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.PagoLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoLaboratorioRepository
        extends JpaRepository<PagoLaboratorio, Long> {

    List<PagoLaboratorio>
    findByOrdenLaboratorioIdOrderByFechaPagoDesc(
            Long ordenLaboratorioId
    );

    Optional<PagoLaboratorio>
    findFirstByOrdenLaboratorioIdAndEstadoOrderByFechaPagoDesc(
            Long ordenLaboratorioId,
            String estado
    );

    boolean existsByOrdenLaboratorioIdAndEstado(
            Long ordenLaboratorioId,
            String estado
    );

    boolean existsByNumeroTransaccion(
            String numeroTransaccion
    );
}