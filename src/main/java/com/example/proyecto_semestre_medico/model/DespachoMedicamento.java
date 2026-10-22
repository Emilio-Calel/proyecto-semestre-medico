package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "despacho_medicamento")
public class DespachoMedicamento {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "receta_id",
            nullable = false
    )
    private Receta receta;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "sucursal_id",
            nullable = false
    )
    private Sucursal sucursal;

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "usuario_id"
    )
    private Usuario usuario;

    @Column(
            name = "fecha_despacho",
            nullable = false
    )
    private LocalDateTime fechaDespacho;

    @Column(
            name = "monto_total",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal montoTotal =
            BigDecimal.ZERO;

    @Column(
            nullable = false
    )
    private Boolean activo = true;

    @OneToMany(
            mappedBy = "despacho",
            cascade = CascadeType.ALL
    )
    private List<DespachoMedicamentoDetalle> detalles =
            new ArrayList<>();

    public DespachoMedicamento() {
    }

    @Transient
    private List<String> avisos = new ArrayList<>();

    public List<String> getAvisos() { return avisos; }

    @PrePersist
    public void prePersist() {

        if (fechaDespacho == null) {
            fechaDespacho =
                    LocalDateTime.now();
        }

        if (montoTotal == null) {
            montoTotal =
                    BigDecimal.ZERO;
        }

        if (activo == null) {
            activo = true;
        }
    }

    public Long getId() {
        return id;
    }

    public Receta getReceta() {
        return receta;
    }

    public void setReceta(
            Receta receta
    ) {
        this.receta = receta;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(
            Sucursal sucursal
    ) {
        this.sucursal = sucursal;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(
            Usuario usuario
    ) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaDespacho() {
        return fechaDespacho;
    }

    public void setFechaDespacho(
            LocalDateTime fechaDespacho
    ) {
        this.fechaDespacho =
                fechaDespacho;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(
            BigDecimal montoTotal
    ) {
        this.montoTotal = montoTotal;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
        this.activo = activo;
    }

    public List<DespachoMedicamentoDetalle>
    getDetalles() {
        return detalles;
    }

    public void setDetalles(
            List<DespachoMedicamentoDetalle> detalles
    ) {
        this.detalles = detalles;
    }
}
