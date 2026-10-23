package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.Permiso;
import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.service.PermisoService;
import com.example.proyecto_semestre_medico.service.RolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
public class PruebaController {

    private final RolService rolService;
    private final PermisoService permisoService;

    public PruebaController(
            RolService rolService,
            PermisoService permisoService
    ) {
        this.rolService = rolService;
        this.permisoService = permisoService;
    }

    @GetMapping("/prueba/roles")
    public List<Rol> listarRoles() {
        return rolService.listarActivos();
    }

    @GetMapping("/prueba/permisos")
    public List<Permiso> listarPermisos() {
        return permisoService.listarActivos();
    }
}
