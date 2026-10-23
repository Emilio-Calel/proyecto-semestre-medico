package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.MovimientoInventarioMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioMedicamentoRepository
        extends JpaRepository<
        MovimientoInventarioMedicamento,
        Long
        >, org.springframework.data.jpa.repository.JpaSpecificationExecutor<MovimientoInventarioMedicamento> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    org.springframework.data.domain.Page<MovimientoInventarioMedicamento> findAll(
            org.springframework.data.jpa.domain.Specification<MovimientoInventarioMedicamento> filtro,
            org.springframework.data.domain.Pageable pagina);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento> findAll(
            org.springframework.data.jpa.domain.Specification<MovimientoInventarioMedicamento> filtro,
            org.springframework.data.domain.Sort orden);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    java.util.Optional<MovimientoInventarioMedicamento> findById(Long id);


    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento>
    findAllByOrderByFechaMovimientoDesc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento>
    findByInventarioIdOrderByFechaMovimientoDesc(
            Long inventarioId
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento>
    findByInventarioIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            Long inventarioId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento>
    findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento>
    findByFechaMovimientoGreaterThanEqualAndFechaMovimientoLessThanOrderByFechaMovimientoDesc(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento>
    findByActivoTrueOrderByFechaMovimientoDesc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"inventario.medicamento", "inventario.sucursal", "usuario"})
    List<MovimientoInventarioMedicamento>
    findByActivoFalseOrderByFechaMovimientoDesc();
}
