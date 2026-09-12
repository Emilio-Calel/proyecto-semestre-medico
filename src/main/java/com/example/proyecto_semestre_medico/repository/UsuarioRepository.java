package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {
            "rol",
            "rol.permisos"
    })
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    long countByRolCodigoAndActivoTrue(String codigo);
}
