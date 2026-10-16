package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.RecetaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaDetalleRepository
        extends JpaRepository<RecetaDetalle, Long> {

    List<RecetaDetalle>
    findByRecetaIdAndActivoTrueOrderByIdAsc(
            Long recetaId
    );
}