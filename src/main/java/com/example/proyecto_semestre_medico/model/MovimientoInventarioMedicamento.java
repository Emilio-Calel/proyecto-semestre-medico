package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "movimiento_inventario_medicamento"
)
public class MovimientoInventarioMedicamento {

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
            name = "inventario_id",
            nullable = false
    )
    private InventarioMedicamento inventario;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "tipo_movimiento",
            nullable = false,
            length = 30
    )
    private TipoMovimientoInventario tipoMovimiento;

    @Column(
            nullable = false
    )
    private Integer cantidad;

    @Column(
            name = "costo_unitario",
            precision = 10,
            scale = 2
    )
    private BigDecimal costoUnitario;

    @Column(
            length = 100
    )
    private String referencia;

    @Column(
            length = 500
    )
    private String motivo;

    @Column(
            name = "stock_anterior",
            nullable = false
    )
    private Integer stockAnterior;

    @Column(
            name = "stock_nuevo",
            nullable = false
    )
    private Integer stockNuevo;

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "usuario_id"
    )
    private Usuario usuario;

    @Column(
            name = "fecha_movimiento",
            nullable = false
    )
    private LocalDateTime fechaMovimiento;

    @Column(
            nullable = false
    )
    private Boolean activo = true;

    public MovimientoInventarioMedicamento() {
    }

    @PrePersist
    public void prePersist() {

        if (fechaMovimiento == null) {
            fechaMovimiento =
                    LocalDateTime.now();
        }

        if (activo == null) {
            activo = true;
        }
    }

    public Long getId() {
        return id;
    }

    public InventarioMedicamento getInventario() {
        return inventario;
    }

    public void setInventario(
            InventarioMedicamento inventario
    ) {
        this.inventario = inventario;
    }

    public TipoMovimientoInventario getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(
            TipoMovimientoInventario tipoMovimiento
    ) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(
            Integer cantidad
    ) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(
            BigDecimal costoUnitario
    ) {
        this.costoUnitario = costoUnitario;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(
            String referencia
    ) {
        this.referencia = referencia;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(
            String motivo
    ) {
        this.motivo = motivo;
    }

    public Integer getStockAnterior() {
        return stockAnterior;
    }

    public void setStockAnterior(
            Integer stockAnterior
    ) {
        this.stockAnterior = stockAnterior;
    }

    public Integer getStockNuevo() {
        return stockNuevo;
    }

    public void setStockNuevo(
            Integer stockNuevo
    ) {
        this.stockNuevo = stockNuevo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(
            Usuario usuario
    ) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(
            LocalDateTime fechaMovimiento
    ) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
        this.activo = activo;
    }
}