package com.example.proyecto_semestre_medico.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class TareaMedicoFormDTO {

    private Long id;

    private String titulo;
    private String descripcion;

    private String prioridad;
    private String estado = "PENDIENTE";

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime fechaLimite;

    public TareaMedicoFormDTO() {
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

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDateTime fechaLimite) {
        this.fechaLimite = fechaLimite;
    }
}
