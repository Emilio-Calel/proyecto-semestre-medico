package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventario_medicamento",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventario_medicamento_sucursal",
                        columnNames = {
                                "medicamento_id",
                                "sucursal_id"
                        }
                )
        }
)
public class InventarioMedicamento {

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
            name = "medicamento_id",
            nullable = false
    )
    private Medicamento medicamento;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "sucursal_id",
            nullable = false
    )
    private Sucursal sucursal;

    @Column(
            name = "stock_actual",
            nullable = false
    )
    private Integer stockActual = 0;

    @Column(
            nullable = false
    )
    private Boolean activo = true;

    @Version
    @Column(
            nullable = false
    )
    private Long version;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(
            name = "fecha_actualizacion",
            nullable = false
    )
    private LocalDateTime fechaActualizacion;

    public InventarioMedicamento() {
    }

    @PrePersist
    public void prePersist() {

        if (stockActual == null) {
            stockActual = 0;
        }

        if (activo == null) {
            activo = true;
        }

        LocalDateTime ahora =
                LocalDateTime.now();

        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }

        fechaActualizacion = ahora;
    }

    @PreUpdate
    public void preUpdate() {

        fechaActualizacion =
                LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(
            Medicamento medicamento
    ) {
        this.medicamento =
                medicamento;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(
            Sucursal sucursal
    ) {
        this.sucursal =
                sucursal;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(
            Integer stockActual
    ) {
        this.stockActual =
                stockActual;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
        this.activo =
                activo;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(
            LocalDateTime fechaCreacion
    ) {
        this.fechaCreacion =
                fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(
            LocalDateTime fechaActualizacion
    ) {
        this.fechaActualizacion =
                fechaActualizacion;
    }
}