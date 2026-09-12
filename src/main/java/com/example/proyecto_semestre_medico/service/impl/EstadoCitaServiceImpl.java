package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.EstadoCita;
import com.example.proyecto_semestre_medico.repository.EstadoCitaRepository;
import com.example.proyecto_semestre_medico.service.EstadoCitaService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadoCitaServiceImpl
        implements EstadoCitaService {

    private final EstadoCitaRepository estadoCitaRepository;

    public EstadoCitaServiceImpl(
            EstadoCitaRepository estadoCitaRepository
    ) {
        this.estadoCitaRepository =
                estadoCitaRepository;
    }

    @Override
    @Cacheable("estadosCitaActivos")
    public List<EstadoCita> listarActivos() {
        return estadoCitaRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Cacheable(
            value = "estadoCitaPorCodigo",
            key = "#codigo"
    )
    public EstadoCita buscarPorCodigo(
            String codigo
    ) {

        return estadoCitaRepository
                .findByCodigo(codigo)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el estado de cita: "
                                        + codigo
                        )
                );
    }
}