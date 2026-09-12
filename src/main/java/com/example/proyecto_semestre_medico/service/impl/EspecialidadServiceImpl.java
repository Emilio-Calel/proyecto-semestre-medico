package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Especialidad;
import com.example.proyecto_semestre_medico.repository.EspecialidadRepository;
import com.example.proyecto_semestre_medico.service.EspecialidadService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecialidadServiceImpl
        implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadServiceImpl(
            EspecialidadRepository especialidadRepository
    ) {
        this.especialidadRepository = especialidadRepository;
    }

    @Override
    @Cacheable("especialidadesActivas")
    public List<Especialidad> listarActivas() {
        return especialidadRepository
                .findByActivoTrueOrderByNombreAsc();
    }

    @Override
    public Especialidad buscarPorId(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Especialidad no encontrada"
                        )
                );
    }
}