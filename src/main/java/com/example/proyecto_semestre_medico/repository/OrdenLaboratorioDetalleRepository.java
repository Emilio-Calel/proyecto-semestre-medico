package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.OrdenLaboratorioDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenLaboratorioDetalleRepository
        extends JpaRepository<OrdenLaboratorioDetalle, Long> {

    List<OrdenLaboratorioDetalle> findByOrdenLaboratorioId(
            Long ordenLaboratorioId
    );

    Optional<OrdenLaboratorioDetalle>
    findByOrdenLaboratorioIdAndExamenId(
            Long ordenLaboratorioId,
            Long examenId
    );
}