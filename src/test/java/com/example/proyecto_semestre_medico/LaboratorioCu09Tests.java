package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.LaboratorioController;
import com.example.proyecto_semestre_medico.controller.MedicoController;
import com.example.proyecto_semestre_medico.dto.*;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.OrdenLaboratorioService;
import com.example.proyecto_semestre_medico.service.impl.OrdenLaboratorioServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LaboratorioCu09Tests {
    OrdenLaboratorioRepository ordenes;
    OrdenLaboratorioDetalleRepository detalles;
    ResultadoLaboratorioRepository resultados;
    UsuarioRepository usuarios;
    MedicoRepository medicos;
    OrdenLaboratorioServiceImpl servicio;
    OrdenLaboratorio orden;
    OrdenLaboratorioDetalle detalle;
    CatalogoExamen examen;
    Usuario usuario;
    ResultadoLaboratorioFormDTO form;
    Map<Long, ResultadoLaboratorio> guardados;

    @BeforeEach void preparar() {
        ordenes = mock(OrdenLaboratorioRepository.class);
        detalles = mock(OrdenLaboratorioDetalleRepository.class);
        resultados = mock(ResultadoLaboratorioRepository.class);
        usuarios = mock(UsuarioRepository.class);
        medicos = mock(MedicoRepository.class);
        servicio = new OrdenLaboratorioServiceImpl(ordenes, mock(ConsultaMedicaRepository.class),
                mock(CitaRepository.class), mock(CatalogoExamenRepository.class), medicos,
                detalles, resultados, usuarios);
        orden = new OrdenLaboratorio(); orden.setId(1L); orden.setNumeroOrden("LAB-TEST");
        orden.setEstado("EN_PROCESO"); orden.setExterna(false); orden.setMontoTotal(new BigDecimal("75.00"));
        orden.setNotas("Orden desde consulta médica");
        Paciente paciente = mock(Paciente.class); when(paciente.getNombreCompleto()).thenReturn("Ana Paciente");
        orden.setPaciente(paciente);
        Medico medico = new Medico(); medico.setId(7L); Personal personal = mock(Personal.class);
        when(personal.getNombreCompleto()).thenReturn("Médico Demo"); medico.setPersonal(personal); orden.setMedico(medico);
        when(medicos.findByPersonalUsuarioNombreUsuario("doctor")).thenReturn(Optional.of(medico));
        examen = new CatalogoExamen(); examen.setId(3L); examen.setNombre("Leucocitos");
        examen.setRangoReferencia("4.0 - 10.0"); examen.setUnidadReferencia("10^9/L");
        detalle = new OrdenLaboratorioDetalle(); detalle.setId(2L); detalle.setOrdenLaboratorio(orden);
        detalle.setExamen(examen); detalle.setPrecio(new BigDecimal("75.00"));
        when(ordenes.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenes.buscarParaProcesar(1L)).thenReturn(Optional.of(orden));
        when(detalles.findById(2L)).thenReturn(Optional.of(detalle));
        when(detalles.findByOrdenLaboratorioId(1L)).thenReturn(List.of(detalle));
        usuario = new Usuario(); usuario.setNombreUsuario("labdemo");
        when(usuarios.findByNombreUsuarioIgnoreCase("labdemo")).thenReturn(Optional.of(usuario));
        guardados = new HashMap<>();
        when(resultados.findByOrdenDetalleId(anyLong())).thenAnswer(i -> Optional.ofNullable(guardados.get(i.getArgument(0))));
        when(resultados.save(any())).thenAnswer(i -> {
            ResultadoLaboratorio r = i.getArgument(0); guardados.put(r.getOrdenDetalle().getId(), r); return r;
        });
        form = new ResultadoLaboratorioFormDTO(); form.setOrdenId(1L); form.setDetalleId(2L);
        form.setValorResultado(" 12.0 "); form.setUnidad(" 10^9/L ");
        form.setFechaResultado(LocalDateTime.of(2026, 10, 21, 10, 30)); form.setNotas(" Revisar ");
    }

    @AfterEach void limpiarSesion() { SecurityContextHolder.clearContext(); }

    @Test void listaYAplicaFiltrosNormalizados() {
        when(ordenes.buscar("EN_PROCESO", "Ana", "Demo")).thenReturn(List.of(orden));
        var lista = servicio.listarOrdenes(" EN_PROCESO ", " Ana ", " Demo ");
        assertEquals(1, lista.size()); assertEquals("LAB-TEST", lista.get(0).getNumeroOrden());
        verify(ordenes).buscar("EN_PROCESO", "Ana", "Demo");
    }

    @Test void filtrosVaciosSeConviertenEnNull() {
        when(ordenes.buscar(null, null, null)).thenReturn(List.of());
        assertTrue(servicio.listarOrdenes(" ", "", null).isEmpty());
        verify(ordenes).buscar(null, null, null);
    }

    @Test void obtieneDetalleCompletoDesdeCatalogo() {
        var dto = servicio.obtenerDetalle(1L);
        assertEquals("Ana Paciente", dto.getPaciente()); assertEquals("Médico Demo", dto.getMedico());
        assertEquals(orden.getMontoTotal(), dto.getMontoTotal()); assertEquals(orden.getNotas(), dto.getNotas());
        assertEquals("EN_PROCESO", dto.getEstado()); assertEquals("LAB-TEST", dto.getNumeroOrden());
        var item = dto.getExamenes().get(0);
        assertEquals("Leucocitos", item.getNombre()); assertEquals(detalle.getPrecio(), item.getPrecio());
        assertEquals("4.0 - 10.0", item.getRangoReferencia()); assertFalse(item.getPublicado());
    }

    @Test void ordenInexistenteONulaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerDetalle(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerDetalle(99L));
    }

    @Test void pendienteNoRegistraNiPublicaYUsaMensajeLiteral() {
        orden.setEstado("PENDIENTE");
        String mensaje = "El pago debe estar completado antes de proceder con la toma de muestras.";
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarResultado(form, "labdemo")).getMessage());
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                () -> servicio.publicarResultado(1L, 2L, "labdemo")).getMessage());
        assertEquals("PENDIENTE", orden.getEstado()); verify(resultados, never()).save(any());
    }

    @Test void registraResultadoEnProcesoSinCompletarOrden() {
        servicio.guardarResultado(form, "labdemo");
        var r = guardados.get(2L);
        assertEquals("12.0", r.getValorResultado()); assertEquals("10^9/L", r.getUnidad());
        assertEquals(form.getFechaResultado(), r.getFechaResultado()); assertEquals("Revisar", r.getNotas());
        assertEquals("4.0 - 10.0", r.getRangoReferencia()); assertSame(usuario, r.getUsuarioRegistro());
        assertFalse(r.getPublicado()); assertEquals("EN_PROCESO", orden.getEstado());
        verify(ordenes, never()).save(any());
    }

    @Test void modificaResultadoNoPublicado() {
        servicio.guardarResultado(form, "labdemo"); form.setValorResultado("8.0");
        servicio.guardarResultado(form, "labdemo"); assertEquals("8.0", guardados.get(2L).getValorResultado());
    }

    @Test void rechazaExamenDeOtraOrdenEnRegistroYPublicacion() {
        OrdenLaboratorio otra = new OrdenLaboratorio(); otra.setId(9L); detalle.setOrdenLaboratorio(otra);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 2L, "labdemo"));
        verify(resultados, never()).save(any());
    }

    @Test void rechazaExamenInexistente() {
        form.setDetalleId(99L);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 99L, "labdemo"));
    }

    @Test void requiereIdentificadoresDeOrdenYExamen() {
        form.setOrdenId(null);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(null, 2L, "labdemo"));
        form.setOrdenId(1L); form.setDetalleId(null);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
    }

    @ParameterizedTest @ValueSource(strings={"valorVacio", "valorLargo", "unidadVacia", "unidadLarga", "fecha", "notasLargas"})
    void validaCamposEnBackend(String campo) {
        switch (campo) {
            case "valorVacio" -> form.setValorResultado(" ");
            case "valorLargo" -> form.setValorResultado("x".repeat(501));
            case "unidadVacia" -> form.setUnidad(" ");
            case "unidadLarga" -> form.setUnidad("x".repeat(51));
            case "fecha" -> form.setFechaResultado(null);
            case "notasLargas" -> form.setNotas("x".repeat(2001));
        }
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
        verify(resultados, never()).save(any());
    }

    @Test void fueraDeRangoEsManualYPersisteConRangoHistorico() {
        form.setFueraRango(true); servicio.guardarResultado(form, "labdemo");
        examen.setRangoReferencia("nuevo rango");
        var dto = servicio.obtenerDetalle(1L).getExamenes().get(0);
        assertTrue(dto.getFueraRango()); assertEquals("4.0 - 10.0", dto.getRangoReferencia());
        servicio.publicarResultado(1L, 2L, "labdemo");
        assertTrue(servicio.obtenerDetalle(1L).getExamenes().get(0).getFueraRango());
    }

    @Test void fueraDeRangoSinCatalogoNoInventaReferencia() {
        examen.setRangoReferencia(null); form.setFueraRango(true);
        servicio.guardarResultado(form, "labdemo");
        assertNull(servicio.obtenerDetalle(1L).getExamenes().get(0).getRangoReferencia());
    }

    @Test void noCalculaAutomaticamenteFueraDeRango() {
        form.setFueraRango(false); servicio.guardarResultado(form, "labdemo");
        assertFalse(guardados.get(2L).getFueraRango());
    }

    @Test void noPublicaSinResultado() {
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 2L, "labdemo"));
        verify(resultados, never()).save(any()); assertEquals("EN_PROCESO", orden.getEstado());
    }

    @Test void noPublicaResultadoIncompletoPreexistente() {
        servicio.guardarResultado(form, "labdemo"); guardados.get(2L).setUnidad(" ");
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 2L, "labdemo"));
        assertFalse(guardados.get(2L).getPublicado()); assertEquals("EN_PROCESO", orden.getEstado());
    }

    OrdenLaboratorioDetalle segundoExamen() {
        var segundo = new OrdenLaboratorioDetalle(); segundo.setId(4L); segundo.setOrdenLaboratorio(orden);
        segundo.setExamen(examen); segundo.setPrecio(BigDecimal.TEN);
        when(detalles.findById(4L)).thenReturn(Optional.of(segundo));
        when(detalles.findByOrdenLaboratorioId(1L)).thenReturn(List.of(detalle, segundo));
        return segundo;
    }

    @Test void publicaIndividualSinCompletarSiFaltaOtroResultado() {
        segundoExamen(); servicio.guardarResultado(form, "labdemo"); servicio.publicarResultado(1L, 2L, "labdemo");
        assertTrue(guardados.get(2L).getPublicado()); assertNotNull(guardados.get(2L).getFechaPublicacion());
        assertSame(usuario, guardados.get(2L).getUsuarioPublicacion()); assertEquals("EN_PROCESO", orden.getEstado());
        verify(ordenes, never()).save(any());
    }

    @Test void soloUltimaPublicacionCompletaYConservaResultados() {
        segundoExamen(); servicio.guardarResultado(form, "labdemo");
        form.setDetalleId(4L); servicio.guardarResultado(form, "labdemo");
        servicio.publicarResultado(1L, 2L, "labdemo"); assertEquals("EN_PROCESO", orden.getEstado());
        servicio.publicarResultado(1L, 4L, "labdemo"); assertEquals("COMPLETADA", orden.getEstado());
        assertEquals(2, guardados.size()); assertTrue(guardados.values().stream().allMatch(ResultadoLaboratorio::getPublicado));
        verify(ordenes).save(orden);
    }

    @Test void publicadoNoPermiteEditarNiPublicarDeNuevo() {
        segundoExamen(); servicio.guardarResultado(form, "labdemo"); servicio.publicarResultado(1L, 2L, "labdemo");
        clearInvocations(resultados); form.setValorResultado("8.0");
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 2L, "labdemo"));
        assertEquals("12.0", guardados.get(2L).getValorResultado()); verify(resultados, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"COMPLETADA", "CANCELADA", "DESCONOCIDO"})
    void noProcesaEstadosIncompatibles(String estado) {
        orden.setEstado(estado);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 2L, "labdemo"));
        assertEquals(estado, orden.getEstado()); verify(resultados, never()).save(any());
    }

    @Test void externaConservaIndicadorYNoPermiteProcesamientoInterno() {
        orden.setExterna(true);
        assertTrue(servicio.obtenerDetalle(1L).getExterna());
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "labdemo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 2L, "labdemo"));
        assertTrue(orden.getExterna()); verify(resultados, never()).save(any());
    }

    @Test void usuarioInexistenteNoRegistraNiPublica() {
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarResultado(form, "ausente"));
        servicio.guardarResultado(form, "labdemo"); clearInvocations(resultados);
        assertThrows(IllegalArgumentException.class, () -> servicio.publicarResultado(1L, 2L, "ausente"));
        assertFalse(guardados.get(2L).getPublicado()); verify(resultados, never()).save(any());
    }

    @Test void controllerUsaSesionYMensajesLiterales() {
        var mockServicio = mock(OrdenLaboratorioService.class);
        var controller = new LaboratorioController(mockServicio);
        var auth = new UsernamePasswordAuthenticationToken("labdemo", "irrelevante");
        var flash = new RedirectAttributesModelMap();
        var binding = new BeanPropertyBindingResult(form, "resultadoForm");
        assertEquals("redirect:/laboratorio/1", controller.guardarResultado(form, binding, auth, flash));
        verify(mockServicio).guardarResultado(form, "labdemo");
        assertEquals("Resultado guardado exitosamente.", flash.getFlashAttributes().get("mensaje"));
        controller.publicarResultado(2L, 1L, auth, flash);
        verify(mockServicio).publicarResultado(1L, 2L, "labdemo");
        assertEquals("Resultado publicado exitosamente.", flash.getFlashAttributes().get("mensaje"));
    }

    @Test void controllerNoGuardaAnteErrorDeConversion() {
        var mockServicio = mock(OrdenLaboratorioService.class);
        var binding = new BeanPropertyBindingResult(form, "resultadoForm");
        binding.addError(new FieldError("resultadoForm", "fechaResultado", "Fecha inválida"));
        var flash = new RedirectAttributesModelMap();
        new LaboratorioController(mockServicio).guardarResultado(form, binding, null, flash);
        assertNotNull(flash.getFlashAttributes().get("error")); verifyNoInteractions(mockServicio);
    }

    String renderizarDetalle() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var motor = new SpringTemplateEngine(); motor.setTemplateResolver(resolver);
        var servlet = new MockServletContext();
        var request = new MockHttpServletRequest(servlet); request.setRequestURI("/laboratorio/1");
        var exchange = JakartaServletWebApplication.buildApplication(servlet)
                .buildExchange(request, new MockHttpServletResponse());
        var contexto = new WebContext(exchange, Locale.forLanguageTag("es-GT"));
        contexto.setVariable("orden", servicio.obtenerDetalle(1L));
        return motor.process("laboratorio/detalle", contexto);
    }

    @Test void pantallaMuestraAlertaManualYRangoSinBloquearPublicacion() {
        form.setFueraRango(true); servicio.guardarResultado(form, "labdemo");
        String html = renderizarDetalle();
        assertTrue(html.contains("Fuera de rango (4.0 - 10.0)"));
        assertTrue(html.contains("Los resultados están fuera del rango de referencia normal. Requiere revisión."));
        assertTrue(html.contains("/laboratorio/resultado/publicar/2"));
        servicio.publicarResultado(1L, 2L, "labdemo");
        html = renderizarDetalle();
        assertTrue(html.contains("Publicado")); assertTrue(html.contains("Fuera de rango (4.0 - 10.0)"));
        assertFalse(html.contains("action=\"/laboratorio/resultado/publicar/2\""));
        assertFalse(html.contains("action=\"/laboratorio/resultado/guardar\""));
    }

    @ParameterizedTest @ValueSource(strings={"PENDIENTE", "EXTERNA", "COMPLETADA"})
    void pantallaNoOfreceProcesarOrdenBloqueada(String caso) {
        if (caso.equals("EXTERNA")) orden.setExterna(true); else orden.setEstado(caso);
        String html = renderizarDetalle();
        assertFalse(html.contains("action=\"/laboratorio/resultado/guardar\""));
        assertFalse(html.contains("action=\"/laboratorio/resultado/publicar/2\""));
        if (caso.equals("EXTERNA")) assertTrue(html.contains("Externa"));
        if (caso.equals("PENDIENTE")) assertTrue(html.contains(
                "El pago debe estar completado antes de proceder con la toma de muestras."));
    }

    @Configuration @EnableMethodSecurity
    static class SeguridadPrueba { }

    @Test void medicoTratanteConsultaSoloPublicadosConDatosCompletos() {
        segundoExamen(); servicio.guardarResultado(form, "labdemo");
        servicio.publicarResultado(1L, 2L, "labdemo");
        form.setDetalleId(4L); form.setValorResultado("BORRADOR PRIVADO");
        servicio.guardarResultado(form, "labdemo");
        var dto = servicio.obtenerResultadosPublicados(1L, "doctor");
        assertEquals("LAB-TEST", dto.getNumeroOrden()); assertEquals("Ana Paciente", dto.getPaciente());
        assertEquals(1, dto.getExamenes().size());
        var resultado = dto.getExamenes().get(0);
        assertEquals("Leucocitos", resultado.getNombre()); assertEquals("12.0", resultado.getValorResultado());
        assertEquals("10^9/L", resultado.getUnidad()); assertEquals(form.getFechaResultado(), resultado.getFechaResultado());
        assertEquals("Revisar", resultado.getNotasResultado()); assertTrue(resultado.getPublicado());
        assertEquals("EN_PROCESO", orden.getEstado());
    }

    @Test void medicoAjenoNoPuedeConsultarAunqueConozcaId() {
        var otro = new Medico(); otro.setId(99L);
        when(medicos.findByPersonalUsuarioNombreUsuario("otro")).thenReturn(Optional.of(otro));
        assertThrows(AccessDeniedException.class, () -> servicio.obtenerResultadosPublicados(1L, "otro"));
        verifyNoInteractions(resultados);
    }

    @Test void usuarioSinPerfilMedicoNoPuedeConsultar() {
        assertThrows(AccessDeniedException.class, () -> servicio.obtenerResultadosPublicados(1L, "ausente"));
        assertThrows(AccessDeniedException.class, () -> servicio.listarOrdenesDelMedico("ausente"));
    }

    @Test void listadoMedicoUsaSoloSusOrdenes() {
        when(ordenes.findByMedicoIdOrderByFechaCreacionDesc(7L)).thenReturn(List.of(orden));
        assertEquals("LAB-TEST", servicio.listarOrdenesDelMedico("doctor").get(0).getNumeroOrden());
        verify(ordenes).findByMedicoIdOrderByFechaCreacionDesc(7L);
        verify(ordenes, never()).buscar(any(), any(), any());
    }

    String renderizarVistaMedica(OrdenLaboratorioDetalleDTO dto) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var motor = new SpringTemplateEngine(); motor.setTemplateResolver(resolver);
        var servlet = new MockServletContext();
        var request = new MockHttpServletRequest(servlet);
        request.setRequestURI("/medico/resultados-laboratorio/1");
        var exchange = JakartaServletWebApplication.buildApplication(servlet)
                .buildExchange(request, new MockHttpServletResponse());
        var contexto = new WebContext(exchange, Locale.forLanguageTag("es-GT"));
        contexto.setVariable("orden", dto);
        return motor.process("medico/resultados-laboratorio", contexto);
    }

    @Test void visorMedicoMuestraFueraDeRangoHistoricoYRegresaAlModuloMedico() {
        form.setFueraRango(true); servicio.guardarResultado(form, "labdemo");
        servicio.publicarResultado(1L, 2L, "labdemo"); examen.setRangoReferencia("nuevo rango");
        var dto = servicio.obtenerResultadosPublicados(1L, "doctor");
        assertTrue(dto.getExamenes().get(0).getFueraRango());
        String html = renderizarVistaMedica(dto);
        assertTrue(html.contains("Fuera de rango (4.0 - 10.0)"));
        assertTrue(html.contains("Los resultados están fuera del rango de referencia normal. Requiere revisión."));
        assertTrue(html.contains("Publicado")); assertTrue(html.contains("12.0"));
        assertTrue(html.contains("href=\"/medico\""));
        assertTrue(html.contains("href=\"/medico/resultados-laboratorio\""));
        assertFalse(html.contains("href=\"/\"")); assertFalse(html.contains("<form"));
    }

    @Test void borradorNoLlegaAlModeloNiSeRenderizaComoDefinitivo() {
        form.setValorResultado("BORRADOR PRIVADO"); form.setNotas("NOTA PRIVADA");
        servicio.guardarResultado(form, "labdemo");
        var dto = servicio.obtenerResultadosPublicados(1L, "doctor");
        assertTrue(dto.getExamenes().isEmpty());
        String html = renderizarVistaMedica(dto);
        assertFalse(html.contains("BORRADOR PRIVADO")); assertFalse(html.contains("NOTA PRIVADA"));
        assertFalse(html.contains("class=\"publicado\""));
        assertTrue(html.contains("Esta orden todavía no tiene resultados publicados disponibles."));
    }

    @Test void endpointsYServicioExigenRolMedicoYUsuarioDeSesion() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.register(SeguridadPrueba.class);
            contexto.registerBean("servicioCu09", OrdenLaboratorioServiceImpl.class, () -> servicio);
            contexto.registerBean(MedicoController.class, () -> new MedicoController(
                    mock(com.example.proyecto_semestre_medico.service.ConsultaMedicaService.class),
                    contexto.getBean(OrdenLaboratorioService.class),
                    mock(com.example.proyecto_semestre_medico.service.RecetaService.class),
                    mock(com.example.proyecto_semestre_medico.service.SeguimientoCitaService.class)));
            contexto.refresh();
            var protegido = contexto.getBean(OrdenLaboratorioService.class);
            var controller = contexto.getBean(MedicoController.class);
            var auth = new UsernamePasswordAuthenticationToken("doctor", "irrelevante",
                    List.of(new SimpleGrantedAuthority("ROLE_MEDICO")));
            assertThrows(AuthenticationCredentialsNotFoundException.class,
                    () -> controller.resultadosLaboratorioOrden(1L, auth, new ExtendedModelMap()));
            for (String rol : List.of("LABORATORISTA", "CAJERO", "PACIENTE")) {
                autenticar(rol);
                assertThrows(AccessDeniedException.class, () -> protegido.obtenerResultadosPublicados(1L, "labdemo"));
                assertThrows(AccessDeniedException.class, () -> protegido.listarOrdenesDelMedico("labdemo"));
                assertThrows(AccessDeniedException.class,
                        () -> controller.resultadosLaboratorioOrden(1L, auth, new ExtendedModelMap()));
                assertThrows(AccessDeniedException.class,
                        () -> controller.resultadosLaboratorio(auth, new ExtendedModelMap()));
            }
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThrows(AccessDeniedException.class, () -> protegido.obtenerResultadosPublicados(1L, "otro"));
            assertThrows(AccessDeniedException.class, () -> protegido.listarOrdenesDelMedico("otro"));
            var model = new ExtendedModelMap();
            assertEquals("medico/resultados-laboratorio", controller.resultadosLaboratorioOrden(1L, auth, model));
            assertTrue(((OrdenLaboratorioDetalleDTO) model.get("orden")).getExamenes().isEmpty());
            when(ordenes.findByMedicoIdOrderByFechaCreacionDesc(7L)).thenReturn(List.of(orden));
            assertEquals("medico/resultados-laboratorio", controller.resultadosLaboratorio(auth, model));
            assertEquals(1, ((List<?>) model.get("ordenes")).size());
        }
    }

    void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "labdemo", "irrelevante", List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    @Test void seguridadRealDeMetodosRechazaAnonimoOtrosRolesYSuplantacion() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.register(SeguridadPrueba.class);
            contexto.registerBean("servicioCu09", OrdenLaboratorioServiceImpl.class, () -> servicio);
            contexto.registerBean(LaboratorioController.class, () -> new LaboratorioController(servicio));
            contexto.refresh();
            var protegido = contexto.getBean(OrdenLaboratorioService.class);
            var controller = contexto.getBean(LaboratorioController.class);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> protegido.obtenerDetalle(1L));
            for (String rol : List.of("MEDICO", "CAJERO", "PACIENTE")) {
                autenticar(rol);
                assertThrows(AccessDeniedException.class, () -> protegido.obtenerDetalle(1L));
                assertThrows(AccessDeniedException.class, () -> protegido.listarOrdenes(null, null, null));
                assertThrows(AccessDeniedException.class, () -> protegido.guardarResultado(form, "labdemo"));
                assertThrows(AccessDeniedException.class, () -> protegido.publicarResultado(1L, 2L, "labdemo"));
                assertThrows(AccessDeniedException.class, () -> controller.listar(null, null, null, new ExtendedModelMap()));
            }
            autenticar("LABORATORISTA");
            assertThrows(AccessDeniedException.class, () -> protegido.guardarResultado(form, "otro"));
            assertThrows(AccessDeniedException.class, () -> protegido.publicarResultado(1L, 2L, "otro"));
            assertEquals("LAB-TEST", protegido.obtenerDetalle(1L).getNumeroOrden());
            protegido.guardarResultado(form, "labdemo"); protegido.publicarResultado(1L, 2L, "labdemo");
            assertEquals("COMPLETADA", orden.getEstado());
        }
    }
}
