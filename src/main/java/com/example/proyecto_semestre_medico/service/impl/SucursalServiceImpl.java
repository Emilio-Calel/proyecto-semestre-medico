package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Sucursal;
import com.example.proyecto_semestre_medico.repository.SucursalRepository;
import com.example.proyecto_semestre_medico.service.SucursalService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SucursalServiceImpl implements SucursalService {

    private final SucursalRepository sucursalRepository;

    public SucursalServiceImpl(
            SucursalRepository sucursalRepository
    ) {
        this.sucursalRepository = sucursalRepository;
    }

    @Override
    @Cacheable("sucursalesActivas")
    public List<Sucursal> listarActivas() {
        return sucursalRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    public Sucursal buscarPorId(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sucursal no encontrada"
                        )
                );
    }
}