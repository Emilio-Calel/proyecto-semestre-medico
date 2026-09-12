package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion_interna")
public class NotificacionInterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_destino_id",
            nullable = false
    )
    private Usuario usuarioDestino;

    @Column(
            nullable = false,
            length = 50
    )
    private String tipo;

    @Column(
            nullable = false,
            length = 200
    )
    private String titulo;

    @Column(
            nullable = false,
            length = 1000
    )
    private String mensaje;

    @Column(
            name = "referencia_tipo",
            length = 50
    )
    private String referenciaTipo;

    @Column(
            name = "referencia_id",
            length = 100
    )
    private String referenciaId;

    @Column(nullable = false)
    private Boolean leida = false;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    public NotificacionInterna() {
    }

    @PrePersist
    public void prePersist() {

        if (leida == null) {
            leida = false;
        }

        if (fechaCreacion == null) {
            fechaCreacion =
                    LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuarioDestino() {
        return usuarioDestino;
    }

    public void setUsuarioDestino(
            Usuario usuarioDestino
    ) {
        this.usuarioDestino =
                usuarioDestino;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getReferenciaTipo() {
        return referenciaTipo;
    }

    public void setReferenciaTipo(
            String referenciaTipo
    ) {
        this.referenciaTipo =
                referenciaTipo;
    }

    public String getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(
            String referenciaId
    ) {
        this.referenciaId =
                referenciaId;
    }

    public Boolean getLeida() {
        return leida;
    }

    public void setLeida(Boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(
            LocalDateTime fechaLectura
    ) {
        this.fechaLectura =
                fechaLectura;
    }
}