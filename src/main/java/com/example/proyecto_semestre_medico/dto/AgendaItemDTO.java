package com.example.proyecto_semestre_medico.dto;

import java.time.LocalDateTime;

public class AgendaItemDTO {

    private Long id;
    private String tipo;

    private String titulo;
    private String descripcion;

    private LocalDateTime inicio;
    private LocalDateTime fin;

    private String estado;
    private String tipoEvento;

    private Boolean todoElDia;

    public AgendaItemDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getTipoEventoDescripcion() {
        if (tipoEvento == null) return "";
        return switch (tipoEvento) {
            case "BLOQUEO" -> "Bloqueo de disponibilidad";
            case "PERSONAL" -> "Evento personal";
            case "CAPACITACION" -> "Capacitación";
            case "VACACIONES" -> "Vacaciones";
            default -> "Bloqueo de disponibilidad (" + tipoEvento + ")";
        };
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Boolean getTodoElDia() {
        return todoElDia;
    }

    public void setTodoElDia(Boolean todoElDia) {
        this.todoElDia = todoElDia;
    }
}
