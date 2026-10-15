package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.ExamenOpcionDTO;
import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.OrdenLaboratorio;
import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioListaDTO;
import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioDetalleDTO;
import com.example.proyecto_semestre_medico.dto.ResultadoLaboratorioFormDTO;

import java.util.List;

public interface OrdenLaboratorioService {

    List<ExamenOpcionDTO> listarExamenesActivos();

    OrdenLaboratorio crearOrden(
            OrdenLaboratorioFormDTO dto,
            String nombreUsuario
    );

    List<OrdenLaboratorioListaDTO> listarOrdenes(
            String estado,
            String paciente,
            String medico
    );

    OrdenLaboratorioDetalleDTO obtenerDetalle(
            Long ordenId
    );

    void guardarResultado(
            ResultadoLaboratorioFormDTO dto,
            String nombreUsuario
    );

    void publicarResultado(
            Long detalleId,
            String nombreUsuario
    );
}
