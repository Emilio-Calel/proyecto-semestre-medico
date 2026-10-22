package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.EstadoReceta;
import com.example.proyecto_semestre_medico.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface RecetaRepository
        extends JpaRepository<Receta, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Receta r where r.id = :id")
    Optional<Receta> buscarParaDespacho(@Param("id") Long id);

    @Query("""
        select r from Receta r where r.activo = true
        and (:recetaId is null or r.id = :recetaId)
        and (:consultaId is null or r.consultaMedica.id = :consultaId)
        order by r.fechaEmision desc
        """)
    List<Receta> buscarActivasFarmacia(@Param("recetaId") Long recetaId, @Param("consultaId") Long consultaId);

    List<Receta>
    findAllByOrderByFechaEmisionDesc();

    List<Receta>
    findByEstadoOrderByFechaEmisionDesc(
            EstadoReceta estado
    );

    Optional<Receta>
    findByIdAndActivoTrue(
            Long id
    );

    List<Receta>
    findByConsultaMedicaIdOrderByFechaEmisionDesc(
            Long consultaMedicaId
    );
}
