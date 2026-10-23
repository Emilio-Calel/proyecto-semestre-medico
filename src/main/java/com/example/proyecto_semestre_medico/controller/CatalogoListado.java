package com.example.proyecto_semestre_medico.controller;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.ui.Model;

/** Paginación de los listados de catálogos existentes, sin cambiar sus consultas operativas. */
final class CatalogoListado {
    private CatalogoListado() { }
    static <T> void cargar(Model model, String atributo, List<T> registros, String filtro, int pagina,
            Function<T, String> texto) {
        String q = filtro == null ? "" : filtro.trim();
        var filtrados = registros.stream().filter(r -> texto.apply(r).toLowerCase(Locale.ROOT)
                .contains(q.toLowerCase(Locale.ROOT))).toList();
        int paginas = Math.max(1, (filtrados.size() + 9) / 10);
        int actual = Math.min(Math.max(pagina, 0), paginas - 1);
        model.addAttribute(atributo, filtrados.subList(actual * 10, Math.min(actual * 10 + 10, filtrados.size())));
        model.addAttribute("q", q);
        model.addAttribute("pagina", actual);
        model.addAttribute("totalPaginas", paginas);
        model.addAttribute("totalElementos", filtrados.size());
    }
}
