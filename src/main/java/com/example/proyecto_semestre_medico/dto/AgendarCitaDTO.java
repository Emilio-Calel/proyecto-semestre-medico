package com.example.proyecto_semestre_medico.dto;

import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

public class AgendarCitaDTO {

    private Long sucursalId;
    private Long especialidadId;
    private Long medicoId;

    private LocalDateTime fechaHoraInicio;

    private String motivoConsulta;
    private MultipartFile documento;

    public AgendarCitaDTO() {
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public Long getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(Long especialidadId) {
        this.especialidadId = especialidadId;
    }

    public Long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(Long medicoId) {
        this.medicoId = medicoId;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(String motivoConsulta) {
        this.motivoConsulta = motivoConsulta;
    }
    public MultipartFile getDocumento() {
        return documento;
    }

    public void setDocumento(
            MultipartFile documento
    ) {
        this.documento = documento;
    }
}