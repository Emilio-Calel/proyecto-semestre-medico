package com.example.proyecto_semestre_medico.controller;

import com.example.proyecto_semestre_medico.dto.AgendaItemDTO;
import com.example.proyecto_semestre_medico.dto.EventoAgendaFormDTO;
import com.example.proyecto_semestre_medico.dto.TareaMedicoFormDTO;
import com.example.proyecto_semestre_medico.service.AgendaMedicoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/medico/agenda")
@PreAuthorize("hasRole('MEDICO')")
public class AgendaMedicoController {

    private final AgendaMedicoService
            agendaMedicoService;

    public AgendaMedicoController(
            AgendaMedicoService agendaMedicoService
    ) {
        this.agendaMedicoService =
                agendaMedicoService;
    }

    /*
     * =========================================================
     * AGENDA PRINCIPAL
     * =========================================================
     */

    @GetMapping
    public String agenda(
            @RequestParam(
                    name = "vista",
                    defaultValue = "SEMANA"
            )
            String vista,

            @RequestParam(
                    name = "fecha",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fecha,

            @RequestParam(
                    name = "filtroTareas",
                    defaultValue = "TODAS"
            )
            String filtroTareas,

            Authentication authentication,
            Model model
    ) {

        LocalDate fechaBase =
                fecha != null
                        ? fecha
                        : LocalDate.now();

        String vistaNormalizada =
                normalizarVista(
                        vista
                );

        LocalDate desdeFecha;
        LocalDate hastaFecha;

        switch (vistaNormalizada) {

            case "DIA" -> {

                desdeFecha =
                        fechaBase;

                hastaFecha =
                        fechaBase;
            }

            case "MES" -> {

                YearMonth mes =
                        YearMonth.from(
                                fechaBase
                        );

                desdeFecha =
                        mes.atDay(1);

                hastaFecha =
                        mes.atEndOfMonth();
            }

            default -> {

                desdeFecha =
                        fechaBase.with(
                                TemporalAdjusters
                                        .previousOrSame(
                                                DayOfWeek.MONDAY
                                        )
                        );

                hastaFecha =
                        fechaBase.with(
                                TemporalAdjusters
                                        .nextOrSame(
                                                DayOfWeek.SUNDAY
                                        )
                        );
            }
        }

        LocalDateTime desde =
                desdeFecha.atStartOfDay();

        LocalDateTime hasta =
                hastaFecha
                        .plusDays(1)
                        .atStartOfDay();

        List<AgendaItemDTO> agenda =
                agendaMedicoService
                        .listarAgenda(
                                authentication.getName(),
                                desde,
                                hasta
                        );

        /*
         * Agrupamos los elementos por día.
         *
         * Así el HTML puede mostrar Día / Semana / Mes
         * sin necesitar una librería externa.
         */
        Map<LocalDate, List<AgendaItemDTO>>
                agendaPorDia =
                new LinkedHashMap<>();

        LocalDate cursor =
                desdeFecha;

        while (!cursor.isAfter(
                hastaFecha
        )) {

            LocalDate diaActual =
                    cursor;

            List<AgendaItemDTO> elementosDia =
                    agenda.stream()
                            .filter(item ->
                                    item.getInicio() != null
                                            && item.getInicio()
                                            .toLocalDate()
                                            .equals(
                                                    diaActual
                                            )
                            )
                            .toList();

            agendaPorDia.put(
                    diaActual,
                    elementosDia
            );

            cursor =
                    cursor.plusDays(1);
        }

        model.addAttribute(
                "vista",
                vistaNormalizada
        );

        model.addAttribute(
                "fechaBase",
                fechaBase
        );

        model.addAttribute(
                "desdeFecha",
                desdeFecha
        );

        model.addAttribute(
                "hastaFecha",
                hastaFecha
        );

        model.addAttribute(
                "agenda",
                agenda
        );

        model.addAttribute(
                "agendaPorDia",
                agendaPorDia
        );

        model.addAttribute(
                "filtroTareas",
                filtroTareas
        );

        model.addAttribute(
                "tareas",
                agendaMedicoService
                        .listarTareas(
                                authentication.getName(),
                                filtroTareas
                        )
        );

        if (!model.containsAttribute(
                "eventoForm"
        )) {

            EventoAgendaFormDTO eventoForm =
                    new EventoAgendaFormDTO();

            /*
             * Valores iniciales cómodos para creación.
             */
            eventoForm.setFechaInicio(
                    LocalDateTime.of(
                            fechaBase,
                            LocalTime.of(
                                    8,
                                    0
                            )
                    )
            );

            eventoForm.setFechaFin(
                    LocalDateTime.of(
                            fechaBase,
                            LocalTime.of(
                                    9,
                                    0
                            )
                    )
            );

            eventoForm.setTipoEvento(
                    "REUNION"
            );

            eventoForm.setTodoElDia(
                    false
            );

            model.addAttribute(
                    "eventoForm",
                    eventoForm
            );
        }

        if (!model.containsAttribute(
                "tareaForm"
        )) {

            TareaMedicoFormDTO tareaForm =
                    new TareaMedicoFormDTO();

            tareaForm.setPrioridad(
                    "NORMAL"
            );

            tareaForm.setFechaLimite(
                    LocalDateTime.of(
                            fechaBase.plusDays(1),
                            LocalTime.of(
                                    17,
                                    0
                            )
                    )
            );

            model.addAttribute(
                    "tareaForm",
                    tareaForm
            );
        }

        return "medico/agenda";
    }

    /*
     * =========================================================
     * EVENTO - GUARDAR
     * =========================================================
     */

    @PostMapping("/evento/guardar")
    public String guardarEvento(
            @ModelAttribute("eventoForm")
            EventoAgendaFormDTO dto,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            agendaMedicoService
                    .guardarEvento(
                            dto,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            dto.getId() == null
                                    ? "Evento creado correctamente."
                                    : "Evento actualizado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "eventoForm",
                            dto
                    );
        }

        return "redirect:/medico/agenda";
    }

    /*
     * =========================================================
     * EVENTO - EDITAR
     * =========================================================
     */

    @GetMapping("/evento/editar/{id}")
    public String editarEvento(
            @PathVariable Long id,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            EventoAgendaFormDTO evento =
                    agendaMedicoService
                            .obtenerEvento(
                                    id,
                                    authentication.getName()
                            );

            redirectAttributes
                    .addFlashAttribute(
                            "eventoForm",
                            evento
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "modoEdicionEvento",
                            true
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/medico/agenda";
    }

    /*
     * =========================================================
     * EVENTO - ELIMINAR
     * =========================================================
     */

    @PostMapping("/evento/eliminar/{id}")
    public String eliminarEvento(
            @PathVariable Long id,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            agendaMedicoService
                    .eliminarEvento(
                            id,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Evento eliminado correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/medico/agenda";
    }

    /*
     * =========================================================
     * TAREA - GUARDAR
     * =========================================================
     */

    @PostMapping("/tarea/guardar")
    public String guardarTarea(
            @ModelAttribute("tareaForm")
            TareaMedicoFormDTO dto,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            agendaMedicoService
                    .guardarTarea(
                            dto,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Tarea guardada correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "tareaForm",
                            dto
                    );
        }

        return "redirect:/medico/agenda";
    }

    /*
     * =========================================================
     * TAREA - CAMBIAR ESTADO
     * =========================================================
     */

    @PostMapping("/tarea/estado/{id}")
    public String cambiarEstadoTarea(
            @PathVariable Long id,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            agendaMedicoService
                    .cambiarEstadoTarea(
                            id,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Estado de la tarea actualizado."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/medico/agenda";
    }

    /*
     * =========================================================
     * TAREA - ELIMINAR
     * =========================================================
     */

    @PostMapping("/tarea/eliminar/{id}")
    public String eliminarTarea(
            @PathVariable Long id,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            agendaMedicoService
                    .eliminarTarea(
                            id,
                            authentication.getName()
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "mensaje",
                            "Tarea eliminada correctamente."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            e.getMessage()
                    );
        }

        return "redirect:/medico/agenda";
    }

    /*
     * =========================================================
     * UTILIDADES
     * =========================================================
     */

    private String normalizarVista(
            String vista
    ) {

        if (vista == null) {
            return "SEMANA";
        }

        String valor =
                vista.trim()
                        .toUpperCase();

        return switch (valor) {

            case "DIA" ->
                    "DIA";

            case "MES" ->
                    "MES";

            default ->
                    "SEMANA";
        };
    }
}