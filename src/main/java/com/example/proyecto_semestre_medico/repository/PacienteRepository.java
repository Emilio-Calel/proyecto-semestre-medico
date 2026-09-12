package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository
        extends JpaRepository<Paciente, Long> {

    boolean existsByDpiHash(String dpiHash);

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Paciente> findByDpiHash(String dpiHash);

    Optional<Paciente> findByUsuarioNombreUsuarioIgnoreCase(
            String nombreUsuario);


}

