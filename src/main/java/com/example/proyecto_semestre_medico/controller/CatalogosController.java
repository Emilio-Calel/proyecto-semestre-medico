package com.example.proyecto_semestre_medico.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CatalogosController {
    @GetMapping("/admin/catalogos")
    @PreAuthorize("hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')")
    public String catalogos() { return "admin/catalogos"; }
}
