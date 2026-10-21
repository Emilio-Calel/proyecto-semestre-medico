package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_id",
            unique = true
    )
    private Usuario usuario;

    @Column(
            name = "nombre_completo",
            nullable = false,
            length = 100
    )
    private String nombreCompleto;

    @Column(
            name = "dpi_cifrado",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String dpiCifrado;

    @Column(
            name = "dpi_hash",
            nullable = false,
            length = 64,
            unique = true
    )
    private String dpiHash;

    @Column(
            name = "nit_cifrado",
            columnDefinition = "TEXT"
    )
    private String nitCifrado;

    @Column(length = 8)
    private String telefono;

    @Column(name = "numero_seguro", length = 50)
    private String numeroSeguro;

    @Column(length = 254)
    private String correo;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    public Paciente() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}