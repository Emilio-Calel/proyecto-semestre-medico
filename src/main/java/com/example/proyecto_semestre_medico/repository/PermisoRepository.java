package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findByActivoTrueOrderByCodigoAsc();

    Optional<Permiso> findByCodigo(String codigo);
}