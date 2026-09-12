package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.UsuarioInternoFormDTO;
import com.example.proyecto_semestre_medico.service.EspecialidadService;
import com.example.proyecto_semestre_medico.service.RolService;
import com.example.proyecto_semestre_medico.service.SucursalService;
import com.example.proyecto_semestre_medico.service.UsuarioInternoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.proyecto_semestre_medico.dto.UsuarioInternoEditarDTO;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.proyecto_semestre_medico.dto.UsuarioInternoFiltroDTO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioInternoController {

    private final UsuarioInternoService usuarioInternoService;
    private final RolService rolService;
    private final SucursalService sucursalService;
    private final EspecialidadService especialidadService;

    public UsuarioInternoController(
            UsuarioInternoService usuarioInternoService,
            RolService rolService,
            SucursalService sucursalService,
            EspecialidadService especialidadService
    ) {
        this.usuarioInternoService = usuarioInternoService;
        this.rolService = rolService;
        this.sucursalService = sucursalService;
        this.especialidadService = especialidadService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public String listar(
            @ModelAttribute("filtro")
            UsuarioInternoFiltroDTO filtro,
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int pagina,
            Model model
    ) {

        try {

            var resultado =
                    usuarioInternoService
                            .buscar(
                                    filtro,
                                    pagina
                            );

            model.addAttribute(
                    "usuarios",
                    resultado.getContent()
            );

            model.addAttribute(
                    "paginaActual",
                    resultado.getNumber()
            );

            model.addAttribute(
                    "totalPaginas",
                    resultado.getTotalPages()
            );

            model.addAttribute(
                    "totalElementos",
                    resultado.getTotalElements()
            );

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "usuarios",
                    List.of()
            );

            model.addAttribute(
                    "paginaActual",
                    0
            );

            model.addAttribute(
                    "totalPaginas",
                    0
            );

            model.addAttribute(
                    "totalElementos",
                    0
            );
        }

        cargarCatalogos(model);

        return "admin/usuarios/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String nuevo(Model model) {

        model.addAttribute(
                "usuarioForm",
                new UsuarioInternoFormDTO()
        );

        cargarCatalogos(model);

        return "admin/usuarios/nuevo";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "usuarioForm",
                usuarioInternoService
                        .buscarParaEditar(id)
        );

        cargarCatalogos(model);

        return "admin/usuarios/editar";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String guardar(
            @ModelAttribute("usuarioForm")
            UsuarioInternoFormDTO dto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println(
                ">>> POST /admin/usuarios/guardar RECIBIDO"
        );
        try {

            usuarioInternoService.crear(dto);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Usuario creado correctamente"
            );

            return "redirect:/admin/usuarios";

        } catch (Exception e) {

            e.printStackTrace();

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            cargarCatalogos(model);

            return "admin/usuarios/nuevo";
        }
    }

    @PostMapping("/actualizar")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String actualizar(
            @ModelAttribute("usuarioForm")
            UsuarioInternoEditarDTO dto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            usuarioInternoService.actualizar(dto);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Usuario actualizado correctamente"
            );

            return "redirect:/admin/usuarios";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            cargarCatalogos(model);

            return "admin/usuarios/editar";
        }
    }

    @PostMapping("/desactivar/{id}")
    @PreAuthorize("hasAuthority('USER_DISABLE')")
    public String desactivar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            usuarioInternoService.desactivar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Usuario desactivado correctamente"
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/usuarios";
    }

    @PostMapping("/reactivar/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String reactivar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            usuarioInternoService.reactivar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Usuario reactivado correctamente"
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/usuarios";
    }

    private void cargarCatalogos(Model model) {

        model.addAttribute(
                "roles",
                rolService.listarRolesInternosActivos()
        );

        model.addAttribute(
                "sucursales",
                sucursalService.listarActivas()
        );

        model.addAttribute(
                "especialidades",
                especialidadService.listarActivas()
        );
    }
}
