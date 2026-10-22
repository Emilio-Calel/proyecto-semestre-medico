package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.id = :id")
    Optional<Medico> buscarParaAgendar(@Param("id") Long id);

    Optional<Medico> findByPersonalId(Long personalId);
    Optional<Medico> findByPersonalUsuarioNombreUsuario(
            String nombreUsuario
    );

    @Query("""
    SELECT new com.example.proyecto_semestre_medico.dto.MedicoOpcionDTO(
        m.id,
        p.nombreCompleto,
        e.nombre
    )
    FROM Medico m
    JOIN m.personal p
    JOIN m.especialidad e
    WHERE p.sucursal.id = :sucursalId
      AND e.id = :especialidadId
      AND m.activo = true
      AND p.activo = true
      AND p.usuario.activo = true
      AND e.activo = true
    ORDER BY p.nombreCompleto ASC
""")
    List<MedicoOpcionDTO> listarDisponiblesParaCita(
            @Param("sucursalId") Long sucursalId,
            @Param("especialidadId") Long especialidadId
    );
}
