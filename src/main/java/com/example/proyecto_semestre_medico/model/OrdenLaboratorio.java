package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_laboratorio")
public class OrdenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "numero_orden",
            nullable = false,
            unique = true,
            length = 30
    )
    private String numeroOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "consulta_medica_id",
            nullable = false
    )
    private ConsultaMedica consultaMedica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "paciente_id",
            nullable = false
    )
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "medico_id",
            nullable = false
    )
    private Medico medico;

    @Column(
            name = "estado",
            nullable = false,
            length = 20
    )
    private String estado;

    @Column(
            name = "externa",
            nullable = false
    )
    private Boolean externa;

    @Column(
            name = "notas",
            length = 2000
    )
    private String notas;

    @Column(
            name = "monto_total",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal montoTotal;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(
            name = "fecha_actualizacion"
    )
    private LocalDateTime fechaActualizacion;

    @OneToMany(
            mappedBy = "ordenLaboratorio",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrdenLaboratorioDetalle> detalles =
            new ArrayList<>();

    @PrePersist
    public void prePersist() {

        if (estado == null) {
            estado = "PENDIENTE";
        }

        if (externa == null) {
            externa = false;
        }

        if (montoTotal == null) {
            montoTotal =
                    BigDecimal.ZERO;
        }

        if (fechaCreacion == null) {
            fechaCreacion =
                    LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion =
                LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(
            String numeroOrden
    ) {
        this.numeroOrden = numeroOrden;
    }

    public ConsultaMedica getConsultaMedica() {
        return consultaMedica;
    }

    public void setConsultaMedica(
            ConsultaMedica consultaMedica
    ) {
        this.consultaMedica =
                consultaMedica;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(
            Paciente paciente
    ) {
        this.paciente = paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(
            Medico medico
    ) {
        this.medico = medico;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(
            String estado
    ) {
        this.estado = estado;
    }

    public Boolean getExterna() {
        return externa;
    }

    public void setExterna(
            Boolean externa
    ) {
        this.externa = externa;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(
            String notas
    ) {
        this.notas = notas;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(
            BigDecimal montoTotal
    ) {
        this.montoTotal =
                montoTotal;
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

    public List<OrdenLaboratorioDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(
            List<OrdenLaboratorioDetalle> detalles
    ) {
        this.detalles = detalles;
    }
}