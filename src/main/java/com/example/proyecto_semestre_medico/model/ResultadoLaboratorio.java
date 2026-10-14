package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resultado_laboratorio")
public class ResultadoLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "orden_detalle_id",
            nullable = false,
            unique = true
    )
    private OrdenLaboratorioDetalle ordenDetalle;

    @Column(
            name = "valor_resultado",
            nullable = false,
            length = 500
    )
    private String valorResultado;

    @Column(
            name = "unidad",
            length = 50
    )
    private String unidad;

    @Column(
            name = "rango_referencia",
            length = 100
    )
    private String rangoReferencia;

    @Column(
            name = "fecha_resultado",
            nullable = false
    )
    private LocalDateTime fechaResultado;

    @Column(
            name = "fuera_rango",
            nullable = false
    )
    private Boolean fueraRango;

    @Column(
            name = "notas",
            length = 2000
    )
    private String notas;

    @Column(
            name = "publicado",
            nullable = false
    )
    private Boolean publicado;

    @Column(
            name = "fecha_publicacion"
    )
    private LocalDateTime fechaPublicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_registro_id",
            nullable = false
    )
    private Usuario usuarioRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_publicacion_id"
    )
    private Usuario usuarioPublicacion;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {

        if (fechaResultado == null) {
            fechaResultado = LocalDateTime.now();
        }

        if (fueraRango == null) {
            fueraRango = false;
        }

        if (publicado == null) {
            publicado = false;
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

    public OrdenLaboratorioDetalle getOrdenDetalle() {
        return ordenDetalle;
    }

    public void setOrdenDetalle(OrdenLaboratorioDetalle ordenDetalle) {
        this.ordenDetalle = ordenDetalle;
    }

    public String getValorResultado() {
        return valorResultado;
    }

    public void setValorResultado(String valorResultado) {
        this.valorResultado = valorResultado;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getRangoReferencia() {
        return rangoReferencia;
    }

    public void setRangoReferencia(String rangoReferencia) {
        this.rangoReferencia = rangoReferencia;
    }

    public LocalDateTime getFechaResultado() {
        return fechaResultado;
    }

    public void setFechaResultado(LocalDateTime fechaResultado) {
        this.fechaResultado = fechaResultado;
    }

    public Boolean getFueraRango() {
        return fueraRango;
    }

    public void setFueraRango(Boolean fueraRango) {
        this.fueraRango = fueraRango;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Boolean getPublicado() {
        return publicado;
    }

    public void setPublicado(Boolean publicado) {
        this.publicado = publicado;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Usuario getUsuarioPublicacion() {
        return usuarioPublicacion;
    }

    public void setUsuarioPublicacion(Usuario usuarioPublicacion) {
        this.usuarioPublicacion = usuarioPublicacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}