package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.dto.SignosVitalesDTO;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.impl.SignosVitalesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignosVitalesCu07Tests {
    CitaRepository citas;
    SignosVitalesRepository signos;
    UsuarioRepository usuarios;
    EstadoCitaRepository estados;
    SignosVitalesServiceImpl service;
    SignosVitalesDTO dto;
    Cita cita;
    Usuario enfermera;

    @BeforeEach void preparar() {
        citas = mock(CitaRepository.class);
        signos = mock(SignosVitalesRepository.class);
        usuarios = mock(UsuarioRepository.class);
        estados = mock(EstadoCitaRepository.class);
        service = new SignosVitalesServiceImpl(citas, signos, usuarios, estados,
                mock(NotificacionInternaRepository.class), mock(AuditoriaRepository.class));
        dto = new SignosVitalesDTO();
        dto.setCitaId(1L);
        dto.setPresionSistolica(120);
        dto.setPresionDiastolica(80);
        dto.setTemperatura(new BigDecimal("36.5"));
        dto.setPeso(new BigDecimal("70.50"));
        dto.setTalla(new BigDecimal("170.00"));
        dto.setPulso(72);
        cita = spy(new Cita());
        when(cita.getId()).thenReturn(1L);
        EstadoCita actual = new EstadoCita();
        actual.setCodigo("SIGNOS_VITALES");
        cita.setEstadoCita(actual);
        EstadoCita espera = new EstadoCita();
        espera.setCodigo("EN_ESPERA");
        enfermera = new Usuario();
        when(citas.findById(1L)).thenReturn(Optional.of(cita));
        when(usuarios.findByNombreUsuarioIgnoreCase("enfermera")).thenReturn(Optional.of(enfermera));
        when(estados.findByCodigo("EN_ESPERA")).thenReturn(Optional.of(espera));
        when(signos.save(any(SignosVitales.class))).thenAnswer(i -> i.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({"presionSistolica,59", "presionSistolica,251", "presionDiastolica,39",
        "presionDiastolica,151", "temperatura,33.9", "temperatura,42.1", "temperatura,36.55",
        "peso,0.49", "peso,300.01", "peso,70.501", "talla,29.99", "talla,250.01",
        "talla,170.001", "pulso,29", "pulso,221", "presionSistolica,null", "temperatura,null",
        "peso,null", "talla,null", "pulso,null"})
    void rechazaFueraDeRangoConMensajeLiteral(String campo, String valor) {
        boolean vacio = valor.equals("null");
        String esperado;
        switch (campo) {
            case "presionSistolica", "presionDiastolica" -> {
                if (campo.equals("presionSistolica")) dto.setPresionSistolica(vacio ? null : Integer.valueOf(valor));
                else dto.setPresionDiastolica(vacio ? null : Integer.valueOf(valor));
                esperado = "La presión arterial debe ingresarse en formato sistólica/diastólica (ej: 120/80) dentro de rangos válidos.";
            }
            case "temperatura" -> {
                dto.setTemperatura(vacio ? null : new BigDecimal(valor));
                esperado = "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.";
            }
            case "peso" -> {
                dto.setPeso(vacio ? null : new BigDecimal(valor));
                esperado = "El peso debe estar entre 0.5 y 300 kg con dos decimales.";
            }
            case "talla" -> {
                dto.setTalla(vacio ? null : new BigDecimal(valor));
                esperado = "La talla debe estar entre 30 y 250 cm con dos decimales.";
            }
            default -> {
                dto.setPulso(vacio ? null : Integer.valueOf(valor));
                esperado = "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.";
            }
        }
        assertEquals(esperado, assertThrows(IllegalArgumentException.class,
                () -> service.registrar("enfermera", dto)).getMessage());
        verifyNoInteractions(signos);
    }

    @Test void normalesSinAlertasYUsuarioAutenticado() {
        SignosVitales result = service.registrar("enfermera", dto);
        assertNull(result.getAlertasClinicas());
        assertSame(enfermera, result.getUsuarioEnfermeria());
        assertEquals("EN_ESPERA", cita.getEstadoCita().getCodigo());
    }

    @Test void emergenciaPersisteAlertasYPrioridadSinIniciarConsulta() {
        dto.setEmergencia(true);
        dto.setPresionSistolica(150);
        dto.setTemperatura(new BigDecimal("38.0"));
        dto.setPulso(110);
        SignosVitales result = service.registrar("enfermera", dto);
        assertTrue(result.getEmergencia());
        assertTrue(cita.getPrioridadEmergencia());
        assertEquals("EN_ESPERA", cita.getEstadoCita().getCodigo());
        assertEquals("Presión arterial fuera de rango normal. Temperatura fuera de rango normal. Frecuencia cardíaca fuera de rango normal.", result.getAlertasClinicas());
    }

    @ParameterizedTest
    @CsvSource({"60,40,34.0,0.50,30.00,30", "250,150,42.0,300.00,250.00,220"})
    void aceptaLimitesDeCaptura(int sistolica, int diastolica, String temperatura,
            String peso, String talla, int pulso) {
        dto.setPresionSistolica(sistolica);
        dto.setPresionDiastolica(diastolica);
        dto.setTemperatura(new BigDecimal(temperatura));
        dto.setPeso(new BigDecimal(peso));
        dto.setTalla(new BigDecimal(talla));
        dto.setPulso(pulso);
        assertNotNull(service.registrar("enfermera", dto));
    }

    @Test void noEliminaPrioridadDeRecepcion() {
        cita.setPrioridadEmergencia(true);
        service.registrar("enfermera", dto);
        assertTrue(cita.getPrioridadEmergencia());
    }

    @Test void noPermiteRegistroDuplicado() {
        when(signos.existsByCitaId(1L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.registrar("enfermera", dto));
        verify(signos, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"presionSistolica,120.5", "presionDiastolica,80.5", "temperatura,abc",
        "peso,abc", "talla,abc", "pulso,72.5"})
    void conversionInvalidaMuestraMensajeDelCampo(String campo, String valor) {
        var servicio = mock(com.example.proyecto_semestre_medico.service.SignosVitalesService.class);
        when(servicio.buscarCitaParaSignos(1L)).thenReturn(cita);
        var controller = new com.example.proyecto_semestre_medico.controller.EnfermeriaController(servicio);
        var binder = new org.springframework.web.bind.WebDataBinder(dto, "signosForm");
        binder.bind(new org.springframework.beans.MutablePropertyValues(java.util.Map.of(campo, valor)));
        assertTrue(binder.getBindingResult().hasErrors());
        var model = new org.springframework.ui.ExtendedModelMap();
        String view = controller.guardar(dto, binder.getBindingResult(), null, model,
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap());
        assertEquals("enfermeria/signos", view);
        String esperado = switch (campo) {
            case "presionSistolica", "presionDiastolica" -> "La presión arterial debe ingresarse en formato sistólica/diastólica (ej: 120/80) dentro de rangos válidos.";
            case "temperatura" -> "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.";
            case "peso" -> "El peso debe estar entre 0.5 y 300 kg con dos decimales.";
            case "talla" -> "La talla debe estar entre 30 y 250 cm con dos decimales.";
            default -> "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.";
        };
        assertEquals(esperado, model.get("error"));
        verify(servicio, never()).registrar(any(), any());
    }
}
