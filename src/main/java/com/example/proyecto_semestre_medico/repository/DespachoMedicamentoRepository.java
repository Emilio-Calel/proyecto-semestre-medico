package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.DespachoMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface DespachoMedicamentoRepository
        extends JpaRepository<DespachoMedicamento, Long> {

    @EntityGraph(attributePaths = {
            "sucursal",
            "usuario",
            "detalles",
            "detalles.recetaDetalle",
            "detalles.recetaDetalle.medicamento",
            "detalles.medicamentoDespachado"
    })
    List<DespachoMedicamento>
    findByRecetaIdOrderByFechaDespachoDesc(
            Long recetaId
    );

    List<DespachoMedicamento>
    findAllByOrderByFechaDespachoDesc();
}