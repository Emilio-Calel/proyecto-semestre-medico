package com.example.proyecto_semestre_medico.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


public class EventoAgendaFormDTO {



    private Long id;

    private String titulo;
    private String descripcion;

    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime fechaInicio;

    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime fechaFin;

    private String tipoEvento;

    private Boolean todoElDia = false;

    public EventoAgendaFormDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getTipoEvento() {
        return tipoEvento;
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