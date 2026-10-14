package com.example.proyecto_semestre_medico.dto;

import java.math.BigDecimal;

public class ExamenOpcionDTO {

    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String rangoReferencia;
    private BigDecimal precio;

    public ExamenOpcionDTO() {
    }

    public ExamenOpcionDTO(
            Long id,
            String codigo,
            String nombre,
            String descripcion,
            String rangoReferencia,
            BigDecimal precio
    ) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.rangoReferencia = rangoReferencia;
        this.precio = precio;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getRangoReferencia() {
        return rangoReferencia;
    }

    public BigDecimal getPrecio() {
        return precio;
    }
}