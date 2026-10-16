package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.MovimientoInventarioMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioMedicamentoRepository
        extends JpaRepository<
        MovimientoInventarioMedicamento,
        Long
        > {

    List<MovimientoInventarioMedicamento>
    findAllByOrderByFechaMovimientoDesc();

    List<MovimientoInventarioMedicamento>
    findByInventarioIdOrderByFechaMovimientoDesc(
            Long inventarioId
    );

    List<MovimientoInventarioMedicamento>
    findByInventarioIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            Long inventarioId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<MovimientoInventarioMedicamento>
    findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<MovimientoInventarioMedicamento>
    findByFechaMovimientoGreaterThanEqualAndFechaMovimientoLessThanOrderByFechaMovimientoDesc(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<MovimientoInventarioMedicamento>
    findByActivoTrueOrderByFechaMovimientoDesc();

    List<MovimientoInventarioMedicamento>
    findByActivoFalseOrderByFechaMovimientoDesc();
}