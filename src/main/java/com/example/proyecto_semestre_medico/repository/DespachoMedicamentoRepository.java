package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.DespachoMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DespachoMedicamentoRepository
        extends JpaRepository<DespachoMedicamento, Long> {

    List<DespachoMedicamento>
    findByRecetaIdOrderByFechaDespachoDesc(
            Long recetaId
    );

    List<DespachoMedicamento>
    findAllByOrderByFechaDespachoDesc();
}