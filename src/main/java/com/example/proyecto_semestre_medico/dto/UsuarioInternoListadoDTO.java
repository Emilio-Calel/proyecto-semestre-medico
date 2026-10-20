package com.example.proyecto_semestre_medico.dto;

public class UsuarioInternoListadoDTO {

    private Long usuarioId;
    private Long personalId;

    private String nombreUsuario;
    private String nombreCompleto;

    private String rol;
    private String nitCifrado;
    private String nit;

    private String sucursal;
    private Boolean activo;

    public UsuarioInternoListadoDTO(
            Long usuarioId,
            Long personalId,
            String nombreUsuario,
            String nombreCompleto,
            String rol,
            String nitCifrado,
            String sucursal,
            Boolean activo
    ) {
        this.usuarioId = usuarioId;
        this.personalId = personalId;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.nitCifrado = nitCifrado;
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

    public String getNitCifrado() {
        return nitCifrado;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getSucursal() {
        return sucursal;
    }

    public Boolean getActivo() {
        return activo;
    }
}