package com.example.proyecto_semestre_medico.dto;

public class UsuarioInternoEditarDTO {

    private Long usuarioId;
    private Long personalId;

    private String nombreCompleto;
    private String nombreUsuario;
    private String nuevaPassword;

    private Long rolId;
    private Long sucursalId;
    private Long especialidadId;

    /*
     * Por seguridad NO cargaremos el DPI/NIT descifrado
     * automáticamente en pantalla.
     *
     * Si estos campos llegan vacíos al actualizar,
     * conservaremos el valor actual.
     */
    private String nuevoDpi;
    private String nuevoNit;

    private String telefono;
    private String numeroSeguro;

    private Boolean tieneDpi;
    private Boolean tieneNit;

    public UsuarioInternoEditarDTO() {
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getPersonalId() {
        return personalId;
    }

    public void setPersonalId(Long personalId) {
        this.personalId = personalId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
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

    public Long getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(Long especialidadId) {
        this.especialidadId = especialidadId;
    }

    public String getNuevoDpi() {
        return nuevoDpi;
    }

    public void setNuevoDpi(String nuevoDpi) {
        this.nuevoDpi = nuevoDpi;
    }

    public String getNuevoNit() {
        return nuevoNit;
    }

    public void setNuevoNit(String nuevoNit) {
        this.nuevoNit = nuevoNit;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getNumeroSeguro() {
        return numeroSeguro;
    }

    public void setNumeroSeguro(String numeroSeguro) {
        this.numeroSeguro = numeroSeguro;
    }

    public Boolean getTieneDpi() {
        return tieneDpi;
    }

    public void setTieneDpi(Boolean tieneDpi) {
        this.tieneDpi = tieneDpi;
    }

    public Boolean getTieneNit() {
        return tieneNit;
    }

    public void setTieneNit(Boolean tieneNit) {
        this.tieneNit = tieneNit;
    }
}