package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SucursalRepository
        extends JpaRepository<Sucursal, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sucursal s WHERE s.id = :id")
    Optional<Sucursal> buscarParaAsignarEspecialidad(@Param("id") Long id);

    List<Sucursal>
    findByActivoTrueOrderByNombreAsc();

    List<Sucursal>
    findAllByOrderByNombreAsc();

    Optional<Sucursal>
    findByNombreIgnoreCase(
            String nombre
    );
}
