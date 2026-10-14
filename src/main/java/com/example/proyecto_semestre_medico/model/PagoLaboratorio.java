package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago_laboratorio")
public class PagoLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "orden_laboratorio_id",
            nullable = false
    )
    private OrdenLaboratorio ordenLaboratorio;

    @Column(
            name = "numero_transaccion",
            nullable = false,
            unique = true,
            length = 40
    )
    private String numeroTransaccion;

    @Column(
            name = "monto",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal monto;

    @Column(
            name = "metodo_pago",
            nullable = false,
            length = 20
    )
    private String metodoPago;

    @Column(
            name = "monto_recibido",
            precision = 10,
            scale = 2
    )
    private BigDecimal montoRecibido;

    @Column(
            name = "cambio",
            precision = 10,
            scale = 2
    )
    private BigDecimal cambio;

    @Column(
            name = "ultimos4",
            length = 4
    )
    private String ultimos4;

    @Column(
            name = "estado",
            nullable = false,
            length = 20
    )
    private String estado;

    @Column(
            name = "referencia",
            length = 100
    )
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_cajero_id",
            nullable = false
    )
    private Usuario usuarioCajero;

    @Column(
            name = "fecha_pago",
            nullable = false
    )
    private LocalDateTime fechaPago;

    @PrePersist
    public void prePersist() {

        if (fechaPago == null) {
            fechaPago = LocalDateTime.now();
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

    public void setOrdenLaboratorio(OrdenLaboratorio ordenLaboratorio) {
        this.ordenLaboratorio = ordenLaboratorio;
    }

    public String getNumeroTransaccion() {
        return numeroTransaccion;
    }

    public void setNumeroTransaccion(String numeroTransaccion) {
        this.numeroTransaccion = numeroTransaccion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(BigDecimal montoRecibido) {
        this.montoRecibido = montoRecibido;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    public void setCambio(BigDecimal cambio) {
        this.cambio = cambio;
    }

    public String getUltimos4() {
        return ultimos4;
    }

    public void setUltimos4(String ultimos4) {
        this.ultimos4 = ultimos4;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public Usuario getUsuarioCajero() {
        return usuarioCajero;
    }

    public void setUsuarioCajero(Usuario usuarioCajero) {
        this.usuarioCajero = usuarioCajero;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }
}