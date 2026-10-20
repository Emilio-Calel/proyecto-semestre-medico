package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.dto.UsuarioInternoEditarDTO;
import com.example.proyecto_semestre_medico.dto.UsuarioInternoFormDTO;
import com.example.proyecto_semestre_medico.dto.UsuarioInternoListadoDTO;
import com.example.proyecto_semestre_medico.dto.UsuarioInternoFiltroDTO;
import org.springframework.data.domain.Page;

import java.util.List;



public interface UsuarioInternoService {

    List<UsuarioInternoListadoDTO> listar();

    void crear(UsuarioInternoFormDTO dto);

    UsuarioInternoEditarDTO buscarParaEditar(Long usuarioId);

    void actualizar(UsuarioInternoEditarDTO dto);

    String desactivar(Long usuarioId);

    void reactivar(Long usuarioId);

    Page<UsuarioInternoListadoDTO> buscar(
            UsuarioInternoFiltroDTO filtro,
            int pagina
    );


}
