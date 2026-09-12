package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.repository.RolRepository;
import com.example.proyecto_semestre_medico.service.RolService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    public RolServiceImpl(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    @Cacheable("rolesActivos")
    public List<Rol> listarActivos() {
        System.out.println(">>> CONSULTANDO ROLES EN POSTGRESQL");
        return rolRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Cacheable("rolesInternosActivos")
    public List<Rol> listarRolesInternosActivos() {
        return rolRepository
                .findByActivoTrueAndCodigoNotOrderByNombreAsc(
                        "PACIENTE"
                );
    }

    @Override
    public Rol buscarPorId(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Rol no encontrado"
                        )
                );
    }
}