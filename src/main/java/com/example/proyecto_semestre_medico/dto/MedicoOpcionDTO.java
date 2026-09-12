package com.example.proyecto_semestre_medico.dto;

public class MedicoOpcionDTO {

    private Long id;
    private String nombre;
    private String especialidad;

    public MedicoOpcionDTO(
            Long id,
            String nombre,
            String especialidad
    ) {
        this.id = id;
        this.nombre = nombre;
        this.especialidad = especialidad;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }
}