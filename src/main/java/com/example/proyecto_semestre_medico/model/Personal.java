package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal")
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(name = "dpi_cifrado", columnDefinition = "TEXT")
    private String dpiCifrado;

    @Column(name = "dpi_hash", length = 64)
    private String dpiHash;

    @Column(name = "nit_cifrado", columnDefinition = "TEXT")
    private String nitCifrado;

    @Column(length = 8)
    private String telefono;

    @Column(name = "numero_seguro", length = 50)
    private String numeroSeguro;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "nit_hash", length = 64)
    private String nitHash;

    public Personal() {
    }

    @PrePersist
    public void prePersist() {
        if (activo == null) {
            activo = true;
        }

        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getDpiCifrado() {
        return dpiCifrado;
    }

    public void setDpiCifrado(String dpiCifrado) {
        this.dpiCifrado = dpiCifrado;
    }

    public String getDpiHash() {
        return dpiHash;
    }

    public void setDpiHash(String dpiHash) {
        this.dpiHash = dpiHash;
    }

    public String getNitCifrado() {
        return nitCifrado;
    }

    public void setNitCifrado(String nitCifrado) {
        this.nitCifrado = nitCifrado;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getNitHash() {
        return nitHash;
    }

    public void setNitHash(String nitHash) {
        this.nitHash = nitHash;
    }
}