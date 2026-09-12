package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.NotificacionInterna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionInternaRepository
        extends JpaRepository<NotificacionInterna, Long> {

    List<NotificacionInterna>
    findByUsuarioDestinoIdAndLeidaFalseOrderByFechaCreacionDesc(
            Long usuarioId
    );
}