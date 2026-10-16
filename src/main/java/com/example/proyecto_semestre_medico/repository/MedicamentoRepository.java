package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicamentoRepository
        extends JpaRepository<Medicamento, Long> {

    List<Medicamento>
    findAllByOrderByNombreAsc();

    List<Medicamento>
    findByActivoTrueOrderByNombreAsc();

    Optional<Medicamento>
    findByNombreIgnoreCase(
            String nombre
    );
}
