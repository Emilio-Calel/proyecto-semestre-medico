package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.Cie10DTO;
import com.example.proyecto_semestre_medico.service.Cie10Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cie10")
@PreAuthorize("hasRole('MEDICO')")
public class Cie10Controller {

    private final Cie10Service cie10Service;

    public Cie10Controller(
            Cie10Service cie10Service
    ) {
        this.cie10Service = cie10Service;
    }

    @GetMapping("/buscar")
    public List<Cie10DTO> buscar(
            @RequestParam("q") String texto
    ) {
        return cie10Service.buscar(texto);
    }
}