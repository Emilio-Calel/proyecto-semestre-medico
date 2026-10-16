package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.model.EstadoReceta;
import com.example.proyecto_semestre_medico.model.Receta;
import com.example.proyecto_semestre_medico.service.RecetaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/farmacia")
@PreAuthorize(
        "hasRole('FARMACEUTICO') or hasRole('ADMINISTRADOR')"
)
public class FarmaciaController {

    private final RecetaService recetaService;

    public FarmaciaController(
            RecetaService recetaService
    ) {
        this.recetaService =
                recetaService;
    }

    @GetMapping
    public String listarRecetas(
            @RequestParam(
                    name = "estado",
                    required = false
            )
            String estado,

            Model model
    ) {

        List<Receta> recetas =
                recetaService
                        .listarTodas();

        if (estado != null
                && !estado.isBlank()
                && !"TODOS".equalsIgnoreCase(
                estado
        )) {

            try {

                EstadoReceta estadoReceta =
                        EstadoReceta.valueOf(
                                estado.toUpperCase()
                        );

                recetas =
                        recetas.stream()
                                .filter(r ->
                                        r.getEstado()
                                                == estadoReceta
                                )
                                .toList();

            } catch (IllegalArgumentException ignored) {
            }
        }

        model.addAttribute(
                "recetas",
                recetas
        );

        model.addAttribute(
                "estadoSeleccionado",
                estado
        );

        model.addAttribute(
                "estados",
                EstadoReceta.values()
        );

        return "farmacia/recetas/lista";
    }

    @GetMapping("/recetas/{id}")
    public String verReceta(
            @PathVariable Long id,
            Model model
    ) {

        Receta receta =
                recetaService
                        .buscarPorId(id);

        model.addAttribute(
                "receta",
                receta
        );

        return "farmacia/recetas/detalle";
    }
}