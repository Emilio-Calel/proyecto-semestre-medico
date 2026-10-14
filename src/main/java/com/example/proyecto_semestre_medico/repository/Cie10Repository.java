package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Cie10;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Cie10Repository
        extends JpaRepository<Cie10, Long> {

    Optional<Cie10> findByCodigoIgnoreCase(String codigo);

    List<Cie10> findTop20ByActivoTrueAndCodigoContainingIgnoreCaseOrActivoTrueAndDescripcionContainingIgnoreCase(
            String codigo,
            String descripcion
    );
}