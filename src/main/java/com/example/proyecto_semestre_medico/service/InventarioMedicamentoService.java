package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.InventarioMedicamento;

import java.util.List;

public interface InventarioMedicamentoService {

    List<InventarioMedicamento> listarTodos();

    InventarioMedicamento buscarPorId(Long id);

    InventarioMedicamento guardar(
            Long medicamentoId,
            Long sucursalId,
            Integer stockActual,
            String nombreUsuario
    );

    void desactivar(
            Long id,
            String nombreUsuario
    );

    void reactivar(
            Long id,
            String nombreUsuario
    );
}