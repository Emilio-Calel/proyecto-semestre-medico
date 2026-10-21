package com.example.proyecto_semestre_medico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Column(
            name = "numero_transaccion",
            nullable = false,
            unique = true,
            length = 50
    )
    private String numeroTransaccion;

    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal monto;

    @Column(
            name = "metodo_pago",
            nullable = false,
            length = 30
    )
    private String metodoPago;

    @Column(
            nullable = false,
            length = 20
    )
    private String estado;

    @Column(
            name = "ultimos_4",
            length = 4
    )
    private String ultimos4;

    @Column(
            name = "referencia_pasarela",
            length = 100
    )
    private String referenciaPasarela;

    @Column(
            name = "fecha_pago",
            nullable = false
    )
    private LocalDateTime fechaPago;

    @Column(
            name = "clave_idempotencia",
            nullable = false,
            unique = true,
            length = 100
    )
    private String claveIdempotencia;

    public Pago() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaPago == null) {
            fechaPago = LocalDateTime.now();
        }
    }

    @Transient
    private String mensajePasarela;

    @Column(
            nullable = false,
            length = 20
    )
    private String canal;

    public Long getId() {
        return id;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    public String getNumeroTransaccion() {
        return numeroTransaccion;
    }

    public void setNumeroTransaccion(
            String numeroTransaccion
    ) {
        this.numeroTransaccion =
                numeroTransaccion;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUltimos4() {
        return ultimos4;
    }

    public void setUltimos4(String ultimos4) {
        this.ultimos4 = ultimos4;
    }

    public String getReferenciaPasarela() {
        return referenciaPasarela;
    }

    public void setReferenciaPasarela(
            String referenciaPasarela
    ) {
        this.referenciaPasarela =
                referenciaPasarela;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(
            String claveIdempotencia
    ) {
        this.claveIdempotencia =
                claveIdempotencia;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getMensajePasarela() {
        return mensajePasarela;
    }

    public void setMensajePasarela(
            String mensajePasarela
    ) {
        this.mensajePasarela =
                mensajePasarela;
    }
}