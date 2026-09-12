package com.example.proyecto_semestre_medico.dto;

public class UsuarioInternoListadoDTO {

    private Long usuarioId;
    private Long personalId;
    private String nombreUsuario;
    private String nombreCompleto;
    private String rol;
    private String sucursal;
    private Boolean activo;

    public UsuarioInternoListadoDTO(
            Long usuarioId,
            Long personalId,
            String nombreUsuario,
            String nombreCompleto,
            String rol,
            String sucursal,
            Boolean activo
    ) {
        this.usuarioId = usuarioId;
        this.personalId = personalId;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.sucursal = sucursal;
        this.activo = activo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getPersonalId() {
        return personalId;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public String getSucursal() {
        return sucursal;
    }

    public Boolean getActivo() {
        return activo;
    }
}