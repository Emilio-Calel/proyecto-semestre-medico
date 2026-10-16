package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.InventarioMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventarioMedicamentoRepository
        extends JpaRepository<
        InventarioMedicamento,
        Long
        > {

    List<InventarioMedicamento>
    findAllByOrderByIdDesc();

    List<InventarioMedicamento>
    findByActivoTrueOrderByIdDesc();

    Optional<InventarioMedicamento>
    findByMedicamentoIdAndSucursalId(
            Long medicamentoId,
            Long sucursalId
    );

    Optional<InventarioMedicamento>
    findByMedicamentoIdAndSucursalIdAndActivoTrue(
            Long medicamentoId,
            Long sucursalId
    );

    List<InventarioMedicamento>
    findBySucursalIdAndActivoTrueOrderByMedicamentoNombreAsc(
            Long sucursalId
    );

    List<InventarioMedicamento>
    findByMedicamentoIdAndActivoTrueOrderBySucursalNombreAsc(
            Long medicamentoId
    );
}