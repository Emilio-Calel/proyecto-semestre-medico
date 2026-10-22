package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.CajaLaboratorioController;
import com.example.proyecto_semestre_medico.dto.PagoLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.PagoLaboratorioServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.mock.web.*;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CajaLaboratorioCu10Tests {
    OrdenLaboratorioRepository ordenes;
    PagoLaboratorioRepository pagos;
    UsuarioRepository usuarios;
    AuditoriaRepository auditorias;
    DatosSensiblesService datos;
    PagoLaboratorioServiceImpl servicio;
    OrdenLaboratorio orden;
    PagoLaboratorioFormDTO form;
    Usuario cajero;
    CajaLaboratorioController controller;
    static final String NO_ENCONTRADAS = "No se encontraron órdenes de laboratorio pendientes de pago. Verifique el DPI o número de orden e intente de nuevo.";
    static final String RECHAZO = "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.";

    @BeforeEach void preparar() {
        ordenes = mock(OrdenLaboratorioRepository.class); pagos = mock(PagoLaboratorioRepository.class);
        usuarios = mock(UsuarioRepository.class); auditorias = mock(AuditoriaRepository.class);
        datos = mock(DatosSensiblesService.class);
        servicio = new PagoLaboratorioServiceImpl(ordenes, pagos, usuarios, auditorias, datos);
        controller = new CajaLaboratorioController(servicio);
        orden = new OrdenLaboratorio(); orden.setId(1L); orden.setNumeroOrden("LAB-DEMO");
        orden.setEstado("PENDIENTE"); orden.setExterna(false); orden.setMontoTotal(new BigDecimal("75.00"));
        orden.setFechaCreacion(LocalDateTime.of(2026, 10, 21, 10, 30));
        Paciente paciente = new Paciente(); paciente.setNombreCompleto("Ana Paciente"); paciente.setDpiCifrado("cifrado");
        orden.setPaciente(paciente);
        CatalogoExamen examen = new CatalogoExamen(); examen.setNombre("Hemograma");
        OrdenLaboratorioDetalle detalle = new OrdenLaboratorioDetalle(); detalle.setExamen(examen);
        detalle.setPrecio(new BigDecimal("75.00")); orden.getDetalles().add(detalle);
        cajero = new Usuario(); cajero.setNombreUsuario("cajero"); cajero.setId(9L);
        when(usuarios.findByNombreUsuarioIgnoreCase("cajero")).thenReturn(Optional.of(cajero));
        when(ordenes.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenes.buscarParaProcesar(1L)).thenReturn(Optional.of(orden));
        when(ordenes.findByNumeroOrden("LAB-DEMO")).thenReturn(Optional.of(orden));
        when(datos.generarHashBusqueda("1234567890123")).thenReturn("hash");
        when(datos.descifrar("cifrado")).thenReturn("1234567890123");
        when(pagos.save(any())).thenAnswer(i -> {
            PagoLaboratorio pago = i.getArgument(0); pago.setId(11L); pago.prePersist(); return pago;
        });
        form = new PagoLaboratorioFormDTO(); form.setOrdenId(1L); form.setMetodoPago("EFECTIVO");
        form.setMontoRecibido(new BigDecimal("75.00"));
    }

    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void buscaDpiValidoYExponeResumenCompleto() {
        when(ordenes.findByPacienteDpiHashAndEstadoAndExternaFalseOrderByFechaCreacionDesc("hash", "PENDIENTE"))
                .thenReturn(List.of(orden));
        var resultado = servicio.buscarPorDpi(" 1234567890123 ").get(0);
        assertEquals("LAB-DEMO", resultado.getNumeroOrden()); assertEquals("Ana Paciente", resultado.getPaciente());
        assertEquals("1234567890123", resultado.getDpi()); assertEquals(1, resultado.getCantidadExamenes());
        assertEquals(orden.getFechaCreacion(), resultado.getFechaCreacion());
        assertEquals(new BigDecimal("75.00"), resultado.getMontoTotal());
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings={" ", "123", "12345678901234", "123456789012A", "123456 890123", "１２３４５６７８９０１２３"})
    void rechazaDpiInvalido(String dpi) {
        assertThrows(IllegalArgumentException.class, () -> servicio.buscarPorDpi(dpi));
        verifyNoInteractions(ordenes, pagos, datos);
    }

    @Test void buscaNumeroRealNormalizandoEspaciosYMayusculas() {
        assertEquals("LAB-DEMO", servicio.buscarPorNumeroOrden(" lab-demo ").get(0).getNumeroOrden());
        verify(ordenes).findByNumeroOrden("LAB-DEMO");
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
    void numeroDeOrdenObligatorio(String numero) {
        assertThrows(IllegalArgumentException.class, () -> servicio.buscarPorNumeroOrden(numero));
    }

    @Test void noEncontradosUsaTextoLiteralEnAmbasBusquedas() {
        for (String tipo : List.of("DPI", "ORDEN")) {
            var modelo = new ExtendedModelMap();
            assertEquals("caja/laboratorio/index", controller.buscar(tipo, "1234567890123", modelo));
            assertEquals(NO_ENCONTRADAS, modelo.get("mensajeBusqueda"));
        }
    }

    @ParameterizedTest @ValueSource(strings={"EN_PROCESO", "COMPLETADA", "CANCELADA", "OTRO"})
    void estadosIncompatiblesNoSeMuestranNiSeCobran(String estado) {
        orden.setEstado(estado);
        when(ordenes.findByPacienteDpiHashAndEstadoAndExternaFalseOrderByFechaCreacionDesc("hash", "PENDIENTE"))
                .thenReturn(List.of(orden));
        assertTrue(servicio.buscarPorDpi("1234567890123").isEmpty());
        assertTrue(servicio.buscarPorNumeroOrden("LAB-DEMO").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerOrdenParaPago(1L));
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        verify(pagos, never()).save(any()); assertEquals(estado, orden.getEstado());
    }

    @Test void pagoAprobadoPrevioBloqueaBusquedaSeleccionYConfirmacion() {
        when(pagos.existsByOrdenLaboratorioIdAndEstado(1L, "APROBADO")).thenReturn(true);
        when(ordenes.findByPacienteDpiHashAndEstadoAndExternaFalseOrderByFechaCreacionDesc("hash", "PENDIENTE"))
                .thenReturn(List.of(orden));
        assertTrue(servicio.buscarPorDpi("1234567890123").isEmpty());
        assertTrue(servicio.buscarPorNumeroOrden("LAB-DEMO").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerOrdenParaPago(1L));
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        verify(pagos, never()).save(any());
    }

    @Test void externaNoSeCobra() {
        orden.setExterna(true);
        assertTrue(servicio.buscarPorNumeroOrden("LAB-DEMO").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        verify(pagos, never()).save(any());
    }

    @Test void ordenInexistenteONulaNoSeCobra() {
        form.setOrdenId(99L);
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        form.setOrdenId(null);
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", null));
        verify(pagos, never()).save(any());
    }

    @Test void efectivoExactoPersistePagoAuditoriaYTransicion() {
        var pago = servicio.procesarPago("cajero", form);
        assertEquals(new BigDecimal("75.00"), pago.getMonto()); assertEquals(new BigDecimal("0.00"), pago.getCambio());
        assertSame(cajero, pago.getUsuarioCajero()); assertSame(orden, pago.getOrdenLaboratorio());
        assertEquals("APROBADO", pago.getEstado()); assertEquals("EN_PROCESO", orden.getEstado());
        assertTrue(pago.getNumeroTransaccion().startsWith("LABPAY-")); assertNotNull(pago.getFechaPago());
        verify(pagos).save(pago); verify(ordenes).save(orden); verify(auditorias).save(any());
        var secuencia = inOrder(ordenes, pagos);
        secuencia.verify(ordenes).buscarParaProcesar(1L);
        secuencia.verify(pagos).existsByOrdenLaboratorioIdAndEstado(1L, "APROBADO");
        secuencia.verify(pagos).save(pago); secuencia.verify(ordenes).save(orden);
    }

    @Test void efectivoConCambio() {
        form.setMontoRecibido(new BigDecimal("100"));
        assertEquals(new BigDecimal("25.00"), servicio.procesarPago("cajero", form).getCambio());
    }

    @Test void efectivoInsuficienteUsaMensajeLiteral() {
        form.setMontoRecibido(new BigDecimal("50"));
        assertEquals("El monto recibido (Q50.00) es menor al monto a cobrar (Q75.00)",
                assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form)).getMessage());
        assertEquals("PENDIENTE", orden.getEstado()); verify(pagos, never()).save(any());
    }

    @ParameterizedTest @NullSource @ValueSource(strings={"-1", "74.999", "100000000"})
    void efectivoInvalidoNoSeRegistra(String valor) {
        form.setMontoRecibido(valor == null ? null : new BigDecimal(valor));
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        verify(pagos, never()).save(any());
    }

    @ParameterizedTest @NullSource @ValueSource(strings={"-1", "75.001", "100000000"})
    void totalPersistidoInvalidoNoSeCobra(String valor) {
        orden.setMontoTotal(valor == null ? null : new BigDecimal(valor));
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        verify(pagos, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"VISA", "MASTERCARD", "DEBITO"})
    void tarjetasValidasSeApruebanConSoloUltimosCuatro(String metodo) {
        form.setMetodoPago(metodo); form.setUltimos4("1234");
        var pago = servicio.procesarPago("cajero", form);
        assertEquals(metodo, pago.getMetodoPago()); assertEquals("1234", pago.getUltimos4());
        assertNull(pago.getMontoRecibido()); assertNull(pago.getCambio());
        assertEquals("APROBADO", pago.getEstado()); assertEquals("EN_PROCESO", orden.getEstado());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"123", "12345", "12AB", "1234567890123456"})
    void tarjetaInvalidaUsaMensajeLiteral(String ultimos) {
        form.setMetodoPago("VISA"); form.setUltimos4(ultimos);
        assertEquals("Ingrese los últimos 4 dígitos de la tarjeta.",
                assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form)).getMessage());
        verify(pagos, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"VISA", "MASTERCARD", "DEBITO"})
    void tarjetaRechazadaNoPersisteYPermiteReintento(String metodo) {
        form.setMetodoPago(metodo); form.setUltimos4("0000");
        assertEquals(RECHAZO, assertThrows(IllegalArgumentException.class,
                () -> servicio.procesarPago("cajero", form)).getMessage());
        assertEquals("PENDIENTE", orden.getEstado()); verify(pagos, never()).save(any());
        verify(ordenes, never()).save(any()); verifyNoInteractions(auditorias);
        form.setMetodoPago("EFECTIVO"); assertEquals("APROBADO", servicio.procesarPago("cajero", form).getEstado());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"TRANSFERENCIA", "OTRO"})
    void metodoNoDefinidoSeRechaza(String metodo) {
        form.setMetodoPago(metodo);
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        verify(pagos, never()).save(any());
    }

    @Test void seleccionSinConfirmarNoCreaTransaccionNiCambiaEstado() {
        servicio.obtenerOrdenParaPago(1L);
        assertEquals("PENDIENTE", orden.getEstado()); verify(pagos, never()).save(any());
    }

    @Test void dobleEnvioNoGeneraSegundoCobro() {
        servicio.procesarPago("cajero", form);
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", form));
        verify(pagos, times(1)).save(any());
    }

    @Test void montoPacienteYUsuarioInyectadosNoAlteranDatosDelServidor() {
        var binder = new WebDataBinder(form, "pagoForm"); controller.limitarCamposPago(binder);
        binder.bind(new MutablePropertyValues(Map.of("monto", "0.01", "montoTotal", "0.01",
                "paciente", "OTRO", "nombreUsuarioCajero", "otro", "estado", "APROBADO")));
        var modelo = new ExtendedModelMap();
        assertEquals("caja/laboratorio/comprobante", controller.procesar(form, binder.getBindingResult(),
                new UsernamePasswordAuthenticationToken("cajero", "irrelevante"), modelo));
        var pago = (PagoLaboratorio) modelo.get("pago");
        assertEquals(new BigDecimal("75.00"), pago.getMonto()); assertSame(cajero, pago.getUsuarioCajero());
        assertSame(orden, pago.getOrdenLaboratorio()); assertEquals(5, binder.getBindingResult().getSuppressedFields().length);
    }

    @Test void conversionInvalidaNoLlegaAlServicioDePago() {
        var mockServicio = mock(PagoLaboratorioService.class);
        var prueba = new CajaLaboratorioController(mockServicio);
        var resumen = servicio.obtenerOrdenParaPago(1L);
        when(mockServicio.obtenerOrdenParaPago(1L)).thenReturn(resumen);
        var binder = new WebDataBinder(form, "pagoForm");
        binder.bind(new MutablePropertyValues(Map.of("montoRecibido", "abc")));
        var modelo = new ExtendedModelMap();
        assertEquals("caja/laboratorio/pago", prueba.procesar(form, binder.getBindingResult(), null, modelo));
        verify(mockServicio, never()).procesarPago(any(), any()); assertNotNull(modelo.get("error"));
    }

    @Test void errorDeOrdenRegresaABusquedaSinVistaConOrdenNula() {
        orden.setEstado("COMPLETADA");
        var modelo = new ExtendedModelMap();
        assertEquals("caja/laboratorio/index", controller.mostrarPago(1L, modelo));
        var binder = new WebDataBinder(form, "pagoForm");
        assertEquals("caja/laboratorio/index", controller.procesar(form, binder.getBindingResult(),
                new UsernamePasswordAuthenticationToken("cajero", "irrelevante"), modelo));
    }

    String renderizar(String template, Map<String, Object> variables) {
        var resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/");
        resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var motor = new SpringTemplateEngine(); motor.setTemplateResolver(resolver);
        var servlet = new MockServletContext(); var request = new MockHttpServletRequest(servlet);
        request.setRequestURI("/caja/laboratorio");
        var exchange = JakartaServletWebApplication.buildApplication(servlet).buildExchange(request, new MockHttpServletResponse());
        var contexto = new WebContext(exchange, Locale.forLanguageTag("es-GT")); contexto.setVariables(variables);
        return motor.process(template, contexto);
    }

    @Test void comprobanteCumpleReglaGlobalYTextoDeExito() {
        var pago = servicio.procesarPago("cajero", form);
        var html = renderizar("caja/laboratorio/comprobante", Map.of("pago", pago, "orden", orden));
        assertTrue(html.contains(pago.getNumeroTransaccion())); assertTrue(html.contains("Ana Paciente"));
        assertTrue(html.contains("Q75.00")); assertTrue(html.contains("EFECTIVO"));
        assertTrue(html.contains("Sucursal")); assertTrue(html.contains("Laboratorio"));
        assertTrue(html.contains("Hemograma")); assertTrue(html.contains("LAB-DEMO"));
        assertTrue(html.contains(String.valueOf(pago.getFechaPago().getYear())));
        assertTrue(html.contains("window.print()")); assertTrue(html.contains("Nuevo Cobro"));
        assertTrue(html.contains("href=\"/caja/laboratorio\""));
        assertTrue(html.contains("¡Pago de laboratorio registrado exitosamente! Paciente: Ana Paciente. La orden ha sido actualizada a estado ‘En proceso’."));
    }

    @Test void pantallaBusquedaTieneTextosYNavegacionInterna() {
        var html = renderizar("caja/laboratorio/index", Map.of("ordenes", List.of()));
        assertTrue(html.contains(NO_ENCONTRADAS)); assertTrue(html.contains("Cobro de Laboratorio en Caja"));
        assertTrue(html.contains("Por DPI")); assertTrue(html.contains("Por No. Orden"));
        assertTrue(html.contains("href=\"/caja\"")); assertFalse(html.contains("href=\"/\""));
    }

    @Configuration @EnableMethodSecurity static class SeguridadPrueba { }

    @Test void servicioYControllerRequierenCajeroYSesionCoincidente() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.register(SeguridadPrueba.class);
            contexto.registerBean(PagoLaboratorioServiceImpl.class, () -> servicio);
            contexto.registerBean(CajaLaboratorioController.class, () -> new CajaLaboratorioController(contexto.getBean(PagoLaboratorioService.class)));
            contexto.refresh();
            var protegido = contexto.getBean(PagoLaboratorioService.class);
            var controlador = contexto.getBean(CajaLaboratorioController.class);
            assertThrows(AuthenticationCredentialsNotFoundException.class, controlador::inicio);
            for (String rol : List.of("MEDICO", "LABORATORISTA", "PACIENTE")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("cajero", "irrelevante",
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
                assertThrows(AccessDeniedException.class, controlador::inicio);
                assertThrows(AccessDeniedException.class, () -> protegido.procesarPago("cajero", form));
                assertThrows(AccessDeniedException.class, () -> protegido.buscarPorDpi("1234567890123"));
                assertThrows(AccessDeniedException.class, () -> protegido.obtenerOrdenParaPago(1L));
            }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("cajero", "irrelevante",
                    List.of(new SimpleGrantedAuthority("ROLE_CAJERO"))));
            assertThrows(AccessDeniedException.class, () -> protegido.procesarPago("otro", form));
            assertEquals("caja/laboratorio/index", controlador.inicio());
            assertEquals("APROBADO", protegido.procesarPago("cajero", form).getEstado());
        }
    }
}
