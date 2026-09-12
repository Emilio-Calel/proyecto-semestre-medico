package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.CorreoPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CorreoPendienteRepository
        extends JpaRepository<CorreoPendiente, Long> {

    List<CorreoPendiente>
    findByEstadoAndProximoIntentoLessThanEqualOrderByFechaCreacionAsc(
            String estado,
            LocalDateTime ahora
    );
}