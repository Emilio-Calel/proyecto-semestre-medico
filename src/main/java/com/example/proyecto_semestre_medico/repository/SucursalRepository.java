package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    List<Sucursal> findByActivoTrueOrderByNombreAsc();
    Optional<Sucursal> findByNombreIgnoreCase(String nombre);
}