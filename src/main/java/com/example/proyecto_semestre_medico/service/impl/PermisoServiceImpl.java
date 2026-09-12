package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Permiso;
import com.example.proyecto_semestre_medico.repository.PermisoRepository;
import com.example.proyecto_semestre_medico.service.PermisoService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermisoServiceImpl implements PermisoService {

    private final PermisoRepository permisoRepository;

    public PermisoServiceImpl(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    @Override
    @Cacheable("permisosActivos")
    public List<Permiso> listarActivos() {
        System.out.println(">>> CONSULTANDO PERMISOS EN POSTGRESQL");
        return permisoRepository.findByActivoTrueOrderByCodigoAsc();
    }
}