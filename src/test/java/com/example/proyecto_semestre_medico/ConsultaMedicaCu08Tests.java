package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.MedicoController;
import com.example.proyecto_semestre_medico.dto.*;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultaMedicaCu08Tests {
    CitaRepository citas;
    ConsultaMedicaRepository consultas;
    MedicoRepository medicos;
    EstadoCitaRepository estados;
    SignosVitalesRepository signos;
    Cie10Repository cie10;
    ConsultaMedicaServiceImpl service;
    Medico medico;
    Cita cita;
    ConsultaMedica consulta;
    ConsultaMedicaFormDTO form;
    RecetaRepository recetas;
    MedicamentoRepository medicamentos;
    RecetaServiceImpl recetaService;
    OrdenLaboratorioRepository ordenes;
    CatalogoExamenRepository examenes;
    OrdenLaboratorioServiceImpl ordenService;

    EstadoCita estado(String codigo) {
        EstadoCita e = new EstadoCita(); e.setCodigo(codigo); return e;
    }

    @BeforeEach void preparar() {
        citas = mock(CitaRepository.class);
        consultas = mock(ConsultaMedicaRepository.class);
        medicos = mock(MedicoRepository.class);
        estados = mock(EstadoCitaRepository.class);
        signos = mock(SignosVitalesRepository.class);
        cie10 = mock(Cie10Repository.class);
        service = new ConsultaMedicaServiceImpl(citas, consultas, cie10, estados, medicos, signos);
        medico = new Medico(); medico.setId(7L);
        Personal personal = new Personal();
        Sucursal sucursal = mock(Sucursal.class); when(sucursal.getId()).thenReturn(3L);
        personal.setSucursal(sucursal); medico.setPersonal(personal);
        cita = spy(new Cita()); when(cita.getId()).thenReturn(11L);
        cita.setNumeroCita("T-123"); cita.setMedico(medico); cita.setSucursal(sucursal);
        cita.setEstadoCita(estado("CONSULTA_MEDICA")); cita.setMotivoConsulta("Dolor de cabeza");
        Paciente paciente = mock(Paciente.class); when(paciente.getNombreCompleto()).thenReturn("Ana Paciente");
        cita.setPaciente(paciente);
        consulta = new ConsultaMedica(); consulta.setId(21L); consulta.setCita(cita);
        consulta.setMedico(medico); consulta.setEstado("EN_CURSO");
        when(medicos.findByPersonalUsuarioNombreUsuario("doctor")).thenReturn(Optional.of(medico));
        when(citas.findById(11L)).thenReturn(Optional.of(cita));
        when(citas.buscarParaIniciarConsulta(11L)).thenReturn(Optional.of(cita));
        when(consultas.findByCitaId(11L)).thenReturn(Optional.of(consulta));
        when(signos.existsByCitaId(11L)).thenReturn(true);
        for (String codigo : List.of("CONSULTA_MEDICA", "PENDIENTE_CIERRE", "ATENCION_FINALIZADA", "NO_ASISTIO"))
            when(estados.findByCodigo(codigo)).thenReturn(Optional.of(estado(codigo)));
        form = new ConsultaMedicaFormDTO(); form.setCitaId(11L); form.setEstado("FINALIZADA");
        form.setMotivoVisita("Dolor de cabeza"); form.setHallazgosClinicos("Sin fiebre");
        form.setDiagnostico("Cefalea tensional"); form.setPlanTratamiento("Reposo e hidratacion");
        recetas = mock(RecetaRepository.class); medicamentos = mock(MedicamentoRepository.class);
        recetaService = new RecetaServiceImpl(recetas, consultas, medicamentos, mock(UsuarioRepository.class), mock(AuditoriaRepository.class), medicos);
        when(recetas.save(any())).thenAnswer(i -> { Receta r = i.getArgument(0); r.setId(31L); return r; });
        Medicamento medicamento = new Medicamento(); medicamento.setNombre("Paracetamol"); medicamento.setActivo(true);
        when(medicamentos.findById(5L)).thenReturn(Optional.of(medicamento));
        ordenes = mock(OrdenLaboratorioRepository.class); examenes = mock(CatalogoExamenRepository.class);
        ordenService = new OrdenLaboratorioServiceImpl(ordenes, consultas, citas, examenes, medicos,
                mock(OrdenLaboratorioDetalleRepository.class), mock(ResultadoLaboratorioRepository.class), mock(UsuarioRepository.class));
        when(ordenes.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void iniciaSoloConSignosYConservaEmergencia() {
        cita.setEstadoCita(estado("EN_ESPERA")); cita.setPrioridadEmergencia(true);
        service.iniciarConsulta(11L, "doctor");
        assertEquals("CONSULTA_MEDICA", cita.getEstadoCita().getCodigo()); assertTrue(cita.getPrioridadEmergencia());
        verify(consultas).save(argThat(c -> c.getMedico() == medico && c.getCita() == cita && "EN_CURSO".equals(c.getEstado())));
    }
    @Test void iniciaEmergenciaSinMedicoDeMismaSucursal() {
        cita.setEstadoCita(estado("EN_ESPERA")); cita.setPrioridadEmergencia(true); cita.setMedico(null);
        service.iniciarConsulta(11L, "doctor"); assertSame(medico, cita.getMedico());
    }
    @Test void rechazaEmergenciaDeOtraSucursal() {
        cita.setEstadoCita(estado("EN_ESPERA")); cita.setPrioridadEmergencia(true); cita.setMedico(null);
        Sucursal otra = mock(Sucursal.class); when(otra.getId()).thenReturn(9L); cita.setSucursal(otra);
        assertThrows(IllegalArgumentException.class, () -> service.iniciarConsulta(11L, "doctor"));
        verify(consultas, never()).save(any());
    }
    @Test void rechazaSinSignosVitales() {
        cita.setEstadoCita(estado("EN_ESPERA")); when(signos.existsByCitaId(11L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.iniciarConsulta(11L, "doctor"));
        verify(consultas, never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings={"PACIENTE_PRESENTE", "SIGNOS_VITALES", "CONSULTA_MEDICA", "PENDIENTE_CIERRE", "NO_ASISTIO", "ATENCION_FINALIZADA"})
    void rechazaInicioDesdeEstadoIncompatible(String codigo) {
        cita.setEstadoCita(estado(codigo));
        assertThrows(IllegalArgumentException.class, () -> service.iniciarConsulta(11L, "doctor"));
        verify(consultas, never()).save(any());
    }
    @Test void rechazaConsultaDuplicada() {
        cita.setEstadoCita(estado("EN_ESPERA")); when(consultas.existsByCitaId(11L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.iniciarConsulta(11L, "doctor"));
    }
    @Test void medicoAjenoNoPuedeOperar() {
        Medico otro = new Medico(); otro.setId(99L); cita.setMedico(otro);
        cita.setEstadoCita(estado("EN_ESPERA"));
        assertThrows(IllegalArgumentException.class, () -> service.iniciarConsulta(11L, "doctor"));
        assertThrows(IllegalArgumentException.class, () -> service.obtenerConsulta(11L, "doctor"));
        assertThrows(IllegalArgumentException.class, () -> service.guardarConsulta(form, "doctor"));
        assertThrows(IllegalArgumentException.class, () -> service.marcarNoAsistio(11L, "doctor"));
        assertThrows(IllegalArgumentException.class, () -> service.finalizarAtencion(11L, "doctor"));
    }
    @Test void usuarioSinMedicoNoPuedeOperar() {
        assertThrows(IllegalArgumentException.class, () -> service.iniciarConsulta(11L, "intruso"));
        assertThrows(IllegalArgumentException.class, () -> service.guardarConsulta(form, "intruso"));
    }
    @Test void noAsistioCierraCitaYDevuelveNumeroReal() {
        cita.setEstadoCita(estado("EN_ESPERA")); assertEquals("T-123", service.marcarNoAsistio(11L, "doctor"));
        assertEquals("NO_ASISTIO", cita.getEstadoCita().getCodigo());
    }
    @ParameterizedTest @ValueSource(strings={"CONSULTA_MEDICA", "PENDIENTE_CIERRE", "ATENCION_FINALIZADA", "NO_ASISTIO"})
    void noAsistioRechazaEstadosIncompatibles(String codigo) {
        cita.setEstadoCita(estado(codigo));
        assertThrows(IllegalArgumentException.class, () -> service.marcarNoAsistio(11L, "doctor"));
    }
    @Test void guardaEnCursoSinDatosDeCierre() {
        form.setEstado("EN_CURSO"); form.setHallazgosClinicos(null); form.setDiagnostico("Pendiente"); form.setPlanTratamiento(null);
        service.guardarConsulta(form, "doctor"); assertEquals("EN_CURSO", consulta.getEstado());
        assertEquals("CONSULTA_MEDICA", cita.getEstadoCita().getCodigo()); assertNull(consulta.getFechaFinalizacion());
        verify(consultas).save(consulta);
    }
    @ParameterizedTest @ValueSource(strings={"motivo", "hallazgos", "plan"})
    void camposObligatoriosAlFinalizar(String campo) {
        switch(campo) { case "motivo" -> form.setMotivoVisita(" "); case "hallazgos" -> form.setHallazgosClinicos(null); default -> form.setPlanTratamiento(""); }
        assertEquals("Debe completar todos los campos obligatorios para cerrar la consulta.",
            assertThrows(IllegalArgumentException.class, () -> service.guardarConsulta(form, "doctor")).getMessage());
        verify(consultas, never()).save(any());
    }
    @ParameterizedTest @ValueSource(ints={0,9,5001})
    void longitudDiagnostico(int longitud) {
        form.setDiagnostico("x".repeat(longitud));
        assertEquals("El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.",
            assertThrows(IllegalArgumentException.class, () -> service.guardarConsulta(form, "doctor")).getMessage());
    }
    @ParameterizedTest @ValueSource(ints={10,5000})
    void finalizaConsultaSinFinalizarAtencionYCieOpcional(int longitud) {
        form.setDiagnostico("x".repeat(longitud)); form.setConsultaId(999L);
        service.guardarConsulta(form, "doctor");
        assertEquals("PENDIENTE_CIERRE", cita.getEstadoCita().getCodigo());
        assertEquals("FINALIZADA", consulta.getEstado()); assertNotNull(consulta.getFechaFinalizacion());
        assertEquals(form.getDiagnostico(), consulta.getDiagnostico());
        assertEquals(form.getPlanTratamiento(), consulta.getPlanTratamiento()); assertNull(consulta.getCie10());
    }
    @ParameterizedTest @ValueSource(strings={"PENDIENTE_CIERRE", "ATENCION_FINALIZADA", "NO_ASISTIO"})
    void noReabreConsultaFinalizada(String codigo) {
        cita.setEstadoCita(estado(codigo)); consulta.setEstado("FINALIZADA"); form.setEstado("EN_CURSO");
        assertThrows(IllegalArgumentException.class, () -> service.guardarConsulta(form, "doctor"));
        assertEquals("FINALIZADA", consulta.getEstado()); verify(consultas, never()).save(any());
    }
    @Test void estadoManipuladoEsRechazado() {
        form.setEstado("ATENCION_FINALIZADA");
        assertThrows(IllegalArgumentException.class, () -> service.guardarConsulta(form, "doctor"));
    }
    @Test void finalizaAtencionSoloDespuesDeConsulta() {
        assertThrows(IllegalArgumentException.class, () -> service.finalizarAtencion(11L, "doctor"));
        consulta.setEstado("FINALIZADA"); cita.setEstadoCita(estado("PENDIENTE_CIERRE"));
        assertEquals("T-123", service.finalizarAtencion(11L, "doctor"));
        assertEquals("ATENCION_FINALIZADA", cita.getEstadoCita().getCodigo());
        assertThrows(IllegalArgumentException.class, () -> service.finalizarAtencion(11L, "doctor"));
    }
    @Test void panelConservaIndicadorEmergencia() {
        cita.setPrioridadEmergencia(true);
        when(citas.listarEnEsperaParaMedico(7L,3L)).thenReturn(List.of(cita));
        assertTrue(service.listarEnEspera("doctor").get(0).getEmergencia());
    }

    RecetaFormDTO recetaForm() {
        RecetaDetalleFormDTO d = new RecetaDetalleFormDTO(); d.setMedicamentoId(5L); d.setDosis("1 tableta");
        d.setFrecuencia("cada 8 horas"); d.setDuracion("7 dias"); d.setCantidad(21);
        RecetaFormDTO r = new RecetaFormDTO(); r.setCitaId(11L); r.setDetalles(new ArrayList<>(List.of(d))); return r;
    }
    void evaluado() { consulta.setEstado("FINALIZADA"); cita.setEstadoCita(estado("PENDIENTE_CIERRE")); }

    @ParameterizedTest @ValueSource(strings={"medicamento","dosis","frecuencia","duracion","detalles"})
    void recetaExigeCamposConMensajeLiteral(String campo) {
        var r=recetaForm(); var d=r.getDetalles().get(0);
        switch(campo) { case "medicamento" -> d.setMedicamentoId(null); case "dosis" -> d.setDosis(" ");
            case "frecuencia" -> d.setFrecuencia(null); case "duracion" -> d.setDuracion(""); default -> r.getDetalles().clear(); }
        assertEquals("Todos los campos de la receta son obligatorios excepto indicaciones especiales.",
            assertThrows(IllegalArgumentException.class, () -> recetaService.crearReceta(r, "doctor")).getMessage());
        verify(recetas, never()).save(any());
    }
    @Test void recetaPersisteFrecuenciaDuracionSinIndicacionesEspeciales() {
        evaluado(); Receta r=recetaService.crearReceta(recetaForm(),"doctor");
        assertEquals("Frecuencia: cada 8 horas. Duración: 7 dias",r.getDetalles().get(0).getIndicaciones());
        assertEquals(21,r.getDetalles().get(0).getCantidad()); assertEquals(EstadoReceta.PENDIENTE,r.getEstado());
        assertNotNull(r.getFechaVencimiento());
    }
    @Test void recetaConservaControlCantidad() {
        var r=recetaForm(); r.getDetalles().get(0).setCantidad(0);
        assertThrows(IllegalArgumentException.class, () -> recetaService.crearReceta(r,"doctor"));
    }
    @Test void recetaRechazaMedicoAjeno() {
        evaluado(); Medico otro=new Medico(); otro.setId(99L); consulta.setMedico(otro);
        assertThrows(IllegalArgumentException.class, () -> recetaService.crearReceta(recetaForm(),"doctor"));
    }
    @Test void recetaNoSeGeneraTrasFinalizarAtencion() {
        evaluado(); cita.setEstadoCita(estado("ATENCION_FINALIZADA"));
        assertThrows(IllegalArgumentException.class, () -> recetaService.crearReceta(recetaForm(),"doctor"));
    }
    OrdenLaboratorioFormDTO ordenForm() {
        OrdenLaboratorioFormDTO o=new OrdenLaboratorioFormDTO(); o.setCitaId(11L); o.setExamenesIds(List.of(5L,5L)); o.setNotas("En ayunas"); return o;
    }
    @Test void ordenConCatalogoPersisteParaLaboratorio() {
        evaluado(); CatalogoExamen e=new CatalogoExamen(); e.setNombre("Hemograma"); e.setActivo(true); e.setPrecio(new BigDecimal("25.00"));
        when(examenes.findById(5L)).thenReturn(Optional.of(e));
        OrdenLaboratorio o=ordenService.crearOrden(ordenForm(),"doctor");
        assertEquals("PENDIENTE",o.getEstado()); assertNotNull(o.getNumeroOrden()); assertEquals(1,o.getDetalles().size());
        assertEquals(new BigDecimal("25.00"),o.getMontoTotal()); assertEquals("En ayunas",o.getNotas()); assertSame(consulta,o.getConsultaMedica());
    }
    @Test void ordenRechazaConsultaEnCurso() {
        assertThrows(IllegalArgumentException.class, () -> ordenService.crearOrden(ordenForm(),"doctor"));
    }
    @Test void ordenRechazaMedicoAjeno() {
        evaluado(); Medico otro=new Medico(); otro.setId(99L); cita.setMedico(otro);
        assertThrows(IllegalArgumentException.class, () -> ordenService.crearOrden(ordenForm(),"doctor"));
    }
    @Test void ordenRechazaCatalogoInactivo() {
        evaluado(); CatalogoExamen e=new CatalogoExamen(); e.setActivo(false);
        when(examenes.findById(5L)).thenReturn(Optional.of(e));
        assertThrows(IllegalArgumentException.class, () -> ordenService.crearOrden(ordenForm(),"doctor"));
    }
    @Test void mensajesDeCierreUsanNumeroServidor() {
        ConsultaMedicaService cs=mock(ConsultaMedicaService.class);
        MedicoController c=new MedicoController(cs,mock(OrdenLaboratorioService.class),mock(RecetaService.class),mock(SeguimientoCitaService.class));
        var auth=new UsernamePasswordAuthenticationToken("doctor","ignored");
        var flash=new RedirectAttributesModelMap(); when(cs.marcarNoAsistio(11L,"doctor")).thenReturn("T-123");
        c.marcarNoAsistio(11L,auth,flash);
        assertEquals("Cita #T-123 marcada como No Asistió.",flash.getFlashAttributes().get("mensaje"));
        flash=new RedirectAttributesModelMap(); when(cs.finalizarAtencion(11L,"doctor")).thenReturn("T-123");
        c.finalizarAtencion(11L,auth,flash);
        assertEquals("Atención finalizada para cita #T-123.",flash.getFlashAttributes().get("mensaje"));
    }
}
