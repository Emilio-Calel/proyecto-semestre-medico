package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.Cie10DTO;
import com.example.proyecto_semestre_medico.model.Cie10;
import com.example.proyecto_semestre_medico.repository.Cie10Repository;
import com.example.proyecto_semestre_medico.service.Cie10Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class Cie10ServiceImpl implements Cie10Service {

    private final Cie10Repository cie10Repository;

    public Cie10ServiceImpl(
            Cie10Repository cie10Repository
    ) {
        this.cie10Repository = cie10Repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cie10DTO> buscar(String texto) {

        if (texto == null) {
            return List.of();
        }

        String termino = texto.trim();

        if (termino.length() < 2) {
            return List.of();
        }

        List<Cie10> resultados =
                cie10Repository
                        .findTop20ByActivoTrueAndCodigoContainingIgnoreCaseOrActivoTrueAndDescripcionContainingIgnoreCase(
                                termino,
                                termino
                        );

        return resultados.stream()
                .map(cie -> new Cie10DTO(
                        cie.getId(),
                        cie.getCodigo(),
                        cie.getDescripcion()
                ))
                .toList();
    }
}