package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "orden_laboratorio_detalle",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_orden_examen",
                        columnNames = {
                                "orden_laboratorio_id",
                                "examen_id"
                        }
                )
        }
)
public class OrdenLaboratorioDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "orden_laboratorio_id",
            nullable = false
    )
    private OrdenLaboratorio ordenLaboratorio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "examen_id",
            nullable = false
    )
    private CatalogoExamen examen;

    @Column(
            name = "precio",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precio;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @OneToOne(
            mappedBy = "ordenDetalle",
            fetch = FetchType.LAZY
    )
    private ResultadoLaboratorio resultado;

    @PrePersist
    public void prePersist() {

        if (fechaCreacion == null) {
            fechaCreacion =
                    LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrdenLaboratorio getOrdenLaboratorio() {
        return ordenLaboratorio;
    }

    public void setOrdenLaboratorio(
            OrdenLaboratorio ordenLaboratorio
    ) {
        this.ordenLaboratorio =
                ordenLaboratorio;
    }

    public CatalogoExamen getExamen() {
        return examen;
    }

    public void setExamen(
            CatalogoExamen examen
    ) {
        this.examen = examen;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(
            BigDecimal precio
    ) {
        this.precio = precio;
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

    public ResultadoLaboratorio getResultado() {
        return resultado;
    }

    public void setResultado(
            ResultadoLaboratorio resultado
    ) {
        this.resultado = resultado;
    }
}