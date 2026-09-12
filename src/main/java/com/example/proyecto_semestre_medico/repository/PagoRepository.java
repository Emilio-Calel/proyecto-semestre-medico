package com.example.proyecto_semestre_medico.repository;

import com.example.proyecto_semestre_medico.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagoRepository
        extends JpaRepository<Pago, Long> {

    boolean existsByNumeroTransaccion(
            String numeroTransaccion
    );

    Optional<Pago> findByClaveIdempotencia(
            String claveIdempotencia
    );

    Optional<Pago> findFirstByCitaIdAndEstadoOrderByFechaPagoDesc(
            Long citaId,
            String estado
    );
}