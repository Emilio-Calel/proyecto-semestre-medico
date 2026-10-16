package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.DespachoMedicamentoDTO;
import com.example.proyecto_semestre_medico.dto.MedicamentoRecetaFarmaciaDTO;
import com.example.proyecto_semestre_medico.model.DespachoMedicamento;

import java.util.List;

public interface DespachoMedicamentoService {

    List<MedicamentoRecetaFarmaciaDTO>
    obtenerDetalleParaDespacho(
            Long recetaId,
            Long sucursalId
    );

    DespachoMedicamento registrarDespacho(
            DespachoMedicamentoDTO dto,
            String nombreUsuario
    );

    List<DespachoMedicamento>
    listarDespachosPorReceta(
            Long recetaId
    );
}