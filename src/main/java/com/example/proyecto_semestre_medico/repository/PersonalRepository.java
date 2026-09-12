package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.dto.UsuarioInternoListadoDTO;
import com.example.proyecto_semestre_medico.model.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonalRepository
        extends JpaRepository<Personal, Long> {

    Optional<Personal> findByUsuarioId(Long usuarioId);

    boolean existsByDpiHash(String dpiHash);

    @Query("""
        SELECT new com.example.proyecto_semestre_medico.dto.UsuarioInternoListadoDTO(
            u.id,
            p.id,
            u.nombreUsuario,
            p.nombreCompleto,
            r.nombre,
            s.nombre,
            u.activo
        )
        FROM Personal p
        JOIN p.usuario u
        JOIN u.rol r
        JOIN p.sucursal s
        WHERE r.codigo <> 'PACIENTE'
        ORDER BY p.nombreCompleto ASC
    """)
    List<UsuarioInternoListadoDTO> listarUsuariosInternos();

    @Query("""
    SELECT new com.example.proyecto_semestre_medico.dto.UsuarioInternoListadoDTO(
        u.id,
        p.id,
        u.nombreUsuario,
        p.nombreCompleto,
        r.nombre,
        s.nombre,
        u.activo
    )
    FROM Personal p
    JOIN p.usuario u
    JOIN u.rol r
    JOIN p.sucursal s
    WHERE r.codigo <> 'PACIENTE'

      AND (
          :usuario = ''
          OR LOWER(u.nombreUsuario)
             LIKE CONCAT('%', LOWER(:usuario), '%')
      )

      AND (
          :nombre = ''
          OR LOWER(p.nombreCompleto)
             LIKE CONCAT('%', LOWER(:nombre), '%')
      )

      AND (
          :nitHash IS NULL
          OR p.nitHash = :nitHash
      )

      AND (
          :rolId IS NULL
          OR r.id = :rolId
      )

      AND (
          :sucursalId IS NULL
          OR s.id = :sucursalId
      )

    ORDER BY p.nombreCompleto ASC
""")
    Page<UsuarioInternoListadoDTO> buscarUsuariosInternos(
            @Param("usuario") String usuario,
            @Param("nombre") String nombre,
            @Param("nitHash") String nitHash,
            @Param("rolId") Long rolId,
            @Param("sucursalId") Long sucursalId,
            Pageable pageable
    );
}