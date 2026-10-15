package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.OrdenLaboratorioPagoDTO;
import com.example.proyecto_semestre_medico.dto.PagoLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.PagoLaboratorio;

import java.util.List;

public interface PagoLaboratorioService {

    List<OrdenLaboratorioPagoDTO> buscarPorDpi(
            String dpi
    );

    List<OrdenLaboratorioPagoDTO> buscarPorNumeroOrden(
            String numeroOrden
    );

    OrdenLaboratorioPagoDTO obtenerOrdenParaPago(
            Long ordenId
    );

    PagoLaboratorio procesarPago(
            String nombreUsuarioCajero,
            PagoLaboratorioFormDTO dto
    );
}