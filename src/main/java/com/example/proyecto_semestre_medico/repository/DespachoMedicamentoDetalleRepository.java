package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.DespachoMedicamentoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DespachoMedicamentoDetalleRepository
        extends JpaRepository<
        DespachoMedicamentoDetalle,
        Long
        > {

    List<DespachoMedicamentoDetalle>
    findByDespachoIdOrderByIdAsc(
            Long despachoId
    );
}