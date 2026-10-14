package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.CatalogoExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogoExamenRepository
        extends JpaRepository<CatalogoExamen, Long> {

    List<CatalogoExamen> findByActivoTrueOrderByNombreAsc();

    Optional<CatalogoExamen> findByCodigoIgnoreCase(String codigo);
}