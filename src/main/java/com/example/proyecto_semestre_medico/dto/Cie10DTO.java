package com.example.proyecto_semestre_medico.dto;

public class Cie10DTO {

    private Long id;
    private String codigo;
    private String descripcion;

    public Cie10DTO() {
    }

    public Cie10DTO(
            Long id,
            String codigo,
            String descripcion
    ) {
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}