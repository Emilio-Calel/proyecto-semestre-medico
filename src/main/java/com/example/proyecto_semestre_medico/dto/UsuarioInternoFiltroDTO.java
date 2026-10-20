package com.example.proyecto_semestre_medico.dto;

public class UsuarioInternoFiltroDTO {

    private String usuario;
    private String nombre;
    private String nit;
    private Long rolId;
    private Long sucursalId;
    private String ordenCampo = "nombre";
    private String ordenDireccion = "asc";

    public UsuarioInternoFiltroDTO() {
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public Long getRolId() {
        return rolId;
    }

    public void setRolId(Long rolId) {
        this.rolId = rolId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getOrdenCampo() {
        return ordenCampo;
    }

    public void setOrdenCampo(String ordenCampo) {
        this.ordenCampo = ordenCampo;
    }

    public String getOrdenDireccion() {
        return ordenDireccion;
    }

    public void setOrdenDireccion(String ordenDireccion) {
        this.ordenDireccion = ordenDireccion;
    }
}