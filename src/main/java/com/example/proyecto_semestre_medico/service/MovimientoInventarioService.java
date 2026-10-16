package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.MovimientoInventarioDTO;
import com.example.proyecto_semestre_medico.model.MovimientoInventarioMedicamento;
import com.example.proyecto_semestre_medico.dto.ResumenMensualInventarioDTO;

import java.util.List;

public interface MovimientoInventarioService {

    List<MovimientoInventarioMedicamento>
    listarTodos();

    List<MovimientoInventarioMedicamento>
    listarPorInventario(
            Long inventarioId
    );

    MovimientoInventarioMedicamento
    registrarMovimiento(
            MovimientoInventarioDTO dto,
            String nombreUsuario
    );

    List<MovimientoInventarioMedicamento>
    listarPorMes(
            int anio,
            int mes
    );

    ResumenMensualInventarioDTO
    obtenerResumenMensual(
            int anio,
            int mes
    );

    MovimientoInventarioMedicamento buscarPorId(
            Long id
    );

    List<MovimientoInventarioMedicamento> listarFiltrados(
            Long inventarioId,
            String tipoMovimiento,
            String estado
    );

    void desactivar(
            Long id,
            String nombreUsuario
    );

    void reactivar(
            Long id,
            String nombreUsuario
    );

    MovimientoInventarioMedicamento registrarDespachoAutomatico(
            Long medicamentoId,
            Long sucursalId,
            Integer cantidad,
            String referencia,
            String motivo,
            String nombreUsuario
    );
}