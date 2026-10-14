package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.ResultadoLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultadoLaboratorioRepository
        extends JpaRepository<ResultadoLaboratorio, Long> {

    Optional<ResultadoLaboratorio>
    findByOrdenDetalleId(Long ordenDetalleId);

    List<ResultadoLaboratorio>
    findByOrdenDetalleOrdenLaboratorioId(
            Long ordenLaboratorioId
    );

    boolean existsByOrdenDetalleId(
            Long ordenDetalleId
    );
}