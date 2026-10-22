package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.FarmaciaController;
import com.example.proyecto_semestre_medico.dto.*;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.*;
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
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.mock.web.*;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FarmaciaCu11Tests {
    RecetaRepository recetas;
    RecetaDetalleRepository lineas;
    MedicamentoRepository medicamentos;
    InventarioMedicamentoRepository inventarios;
    SucursalRepository sucursales;
    UsuarioRepository usuarios;
    AuditoriaRepository auditorias;
    NotificacionInternaRepository notificaciones;
    DespachoMedicamentoRepository despachos;
    MovimientoInventarioMedicamentoRepository movimientos;
    DespachoMedicamentoServiceImpl servicio;
    RecetaServiceImpl recetaServicio;
    FarmaciaController controller;
    Receta receta;
    RecetaDetalle linea;
    Medicamento original, alternativo;
    InventarioMedicamento inventario, inventarioAlternativo;
    Sucursal sucursal;
    Usuario farmacia, doctor;
    DespachoMedicamentoDTO form;
    DespachoDetalleDTO item;
    static final String INVALIDA = "La receta es inválida. Verifique que el medicamento exista, la dosis sea correcta y la receta no sea anterior a 7 días.";

    @BeforeEach void preparar() {
        recetas = mock(RecetaRepository.class); lineas = mock(RecetaDetalleRepository.class);
        medicamentos = mock(MedicamentoRepository.class); inventarios = mock(InventarioMedicamentoRepository.class);
        sucursales = mock(SucursalRepository.class); usuarios = mock(UsuarioRepository.class);
        auditorias = mock(AuditoriaRepository.class); notificaciones = mock(NotificacionInternaRepository.class);
        despachos = mock(DespachoMedicamentoRepository.class); movimientos = mock(MovimientoInventarioMedicamentoRepository.class);
        var movimientosServicio = new MovimientoInventarioServiceImpl(movimientos, inventarios, usuarios, auditorias);
        servicio = new DespachoMedicamentoServiceImpl(despachos, recetas, lineas, sucursales, medicamentos,
                inventarios, usuarios, auditorias, movimientosServicio, notificaciones);
        recetaServicio = new RecetaServiceImpl(recetas, mock(ConsultaMedicaRepository.class), medicamentos,
                usuarios, auditorias, mock(MedicoRepository.class));
        controller = new FarmaciaController(recetaServicio, servicio, sucursales, medicamentos, inventarios);
        farmacia = new Usuario(); farmacia.setNombreUsuario("farmacia");
        doctor = new Usuario(); doctor.setNombreUsuario("doctor");
        Personal personal = new Personal(); personal.setUsuario(doctor); personal.setNombreCompleto("Médico Demo");
        Medico medico = new Medico(); medico.setPersonal(personal);
        Paciente paciente = new Paciente(); paciente.setNombreCompleto("Ana Paciente");
        Cita cita = new Cita(); cita.setPaciente(paciente); cita.setNumeroCita("CITA-DEMO");
        ConsultaMedica consulta = new ConsultaMedica(); consulta.setId(8L); consulta.setMedico(medico); consulta.setCita(cita);
        receta = new Receta(); receta.setId(1L); receta.setConsultaMedica(consulta);
        receta.setFechaEmision(LocalDateTime.now().minusDays(2)); receta.setFechaVencimiento(LocalDateTime.now().plusDays(5));
        receta.setObservaciones("Tomar con agua");
        original = medicamento(3L, "Original", "10.00"); alternativo = medicamento(4L, "Alternativo", "7.50");
        sucursal = new Sucursal(); sucursal.setId(5L); sucursal.setNombre("Central");
        inventario = inventario(original, 5); inventarioAlternativo = inventario(alternativo, 8);
        linea = new RecetaDetalle(); linea.setId(2L); linea.setReceta(receta); linea.setMedicamento(original);
        linea.setCantidad(5); linea.setDosis("Una tableta cada ocho horas"); receta.setDetalles(List.of(linea));
        when(recetas.findById(1L)).thenReturn(Optional.of(receta));
        when(recetas.buscarParaDespacho(1L)).thenReturn(Optional.of(receta));
        when(recetas.save(any())).thenAnswer(i -> i.getArgument(0));
        when(lineas.findById(2L)).thenReturn(Optional.of(linea));
        when(lineas.findByRecetaIdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of(linea));
        when(medicamentos.findById(3L)).thenReturn(Optional.of(original));
        when(medicamentos.findById(4L)).thenReturn(Optional.of(alternativo));
        when(medicamentos.existsById(3L)).thenReturn(true);
        when(medicamentos.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(original, alternativo));
        when(sucursales.findById(5L)).thenReturn(Optional.of(sucursal));
        when(sucursales.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(sucursal));
        when(inventarios.findByMedicamentoIdAndSucursalIdAndActivoTrue(3L, 5L)).thenReturn(Optional.of(inventario));
        when(inventarios.findByMedicamentoIdAndSucursalIdAndActivoTrue(4L, 5L)).thenReturn(Optional.of(inventarioAlternativo));
        when(inventarios.findBySucursalIdAndActivoTrueOrderByMedicamentoNombreAsc(5L)).thenReturn(List.of(inventario, inventarioAlternativo));
        when(usuarios.findByNombreUsuarioIgnoreCase("farmacia")).thenReturn(Optional.of(farmacia));
        when(movimientos.save(any())).thenAnswer(i -> {
            MovimientoInventarioMedicamento m = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(m, "id", 20L); return m;
        });
        when(despachos.save(any())).thenAnswer(i -> {
            DespachoMedicamento d = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(d, "id", 30L); return d;
        });
        form = new DespachoMedicamentoDTO(); form.setRecetaId(1L); form.setSucursalId(5L);
        item = new DespachoDetalleDTO(); item.setRecetaDetalleId(2L); item.setCantidad(2);
        item.setCantidadDespachadaPrevia(0); item.setMedicamentoDespachadoId(3L); form.setDetalles(List.of(item));
    }

    Medicamento medicamento(long id, String nombre, String precio) {
        Medicamento med = new Medicamento(); med.setId(id); med.setNombre(nombre); med.setPrecio(new BigDecimal(precio));
        med.setUnidad("Tabletas"); med.setStockMinimo(2); return med;
    }

    InventarioMedicamento inventario(Medicamento med, int stock) {
        var inv = new InventarioMedicamento(); inv.setId(med.getId()); inv.setMedicamento(med);
        inv.setSucursal(sucursal); inv.setStockActual(stock); return inv;
    }

    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void busquedaPorIdRecetaYConsultaConservaSoloActivasDelRepositorio() {
        when(recetas.buscarActivasFarmacia(1L, null)).thenReturn(List.of(receta));
        when(recetas.buscarActivasFarmacia(null, 8L)).thenReturn(List.of(receta));
        assertEquals(List.of(receta), recetaServicio.buscarActivasFarmacia(1L, null));
        assertEquals(List.of(receta), recetaServicio.buscarActivasFarmacia(null, 8L));
        var model = new ExtendedModelMap(); controller.listarRecetas(null, 1L, null, model);
        assertEquals(List.of(receta), model.get("recetas"));
        assertThrows(IllegalArgumentException.class, () -> recetaServicio.buscarActivasFarmacia(-1L, null));
    }

    @Test void recetaVigenteMuestraDosisPrecioYStockDeSucursal() {
        var dto = servicio.obtenerDetalleParaDespacho(1L, 5L).get(0);
        assertEquals("Original", dto.getMedicamento()); assertEquals(linea.getDosis(), dto.getDosis());
        assertEquals(5, dto.getCantidadRecetada()); assertEquals(5, dto.getStockDisponible());
        assertTrue(dto.isInventarioRegistrado()); assertEquals(new BigDecimal("10.00"), dto.getPrecioUnitario());
        assertEquals(2, receta.getDiasTranscurridos());
    }

    @Test void recetaAntesDeCumplirSieteDiasSigueVigente() {
        receta.setFechaEmision(LocalDateTime.now().minusDays(7).plusMinutes(1));
        receta.setFechaVencimiento(receta.getFechaEmision().plusDays(7));
        assertFalse(receta.isVencidaParaDespacho()); assertFalse(servicio.obtenerDetalleParaDespacho(1L, 5L).isEmpty());
    }

    @Test void vigenciaMostradaNoPuedeExtenderSieteDiasDesdeEmision() {
        receta.setFechaVencimiento(LocalDateTime.now().plusYears(1));
        assertEquals(receta.getFechaEmision().plusDays(7), receta.getVigenciaDespacho());
        receta.setFechaVencimiento(receta.getFechaEmision().plusDays(3));
        assertEquals(receta.getFechaVencimiento(), receta.getVigenciaDespacho());
        var model = new ExtendedModelMap(); controller.listarRecetas(null, -1L, null, model);
        assertNotNull(model.get("error")); assertEquals(List.of(), model.get("recetas"));
    }

    @Test void recetaMayorASieteDiasRechazaPreparacionYConfirmacionConTextoLiteral() {
        receta.setFechaEmision(LocalDateTime.now().minusDays(8));
        receta.setFechaVencimiento(LocalDateTime.now().plusYears(1));
        String esperado = "Receta Vencida. La receta #1 fue emitida hace 8 días y ya no es válida para despacho.";
        assertEquals(esperado, assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerDetalleParaDespacho(1L, 5L)).getMessage());
        assertEquals(esperado, assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarDespacho(form, "farmacia")).getMessage());
        verifyNoInteractions(movimientos, despachos);
        var flash = new RedirectAttributesModelMap();
        controller.abrirDespacho(1L, null, new ExtendedModelMap(), flash);
        assertEquals(esperado, flash.getFlashAttributes().get("error"));
    }

    @Test void lecturaQueActualizaVencimientoComparteBloqueoConDespacho() {
        receta.setFechaEmision(LocalDateTime.now().minusDays(8));
        recetaServicio.buscarPorId(1L);
        verify(recetas).buscarParaDespacho(1L);
        verify(recetas, never()).findById(anyLong());
        assertEquals(EstadoReceta.VENCIDA, receta.getEstado());
    }

    @ParameterizedTest @ValueSource(strings={"inactivo", "inexistente", "sinDosis", "dosisLarga", "sinCantidad"})
    void recetaInvalidaSeRechazaConMensajeLiteral(String caso) {
        switch (caso) {
            case "inactivo" -> original.setActivo(false);
            case "inexistente" -> when(medicamentos.existsById(3L)).thenReturn(false);
            case "sinDosis" -> linea.setDosis(" ");
            case "dosisLarga" -> linea.setDosis("x".repeat(201));
            case "sinCantidad" -> linea.setCantidad(null);
        }
        assertEquals(INVALIDA, assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarDespacho(form, "farmacia")).getMessage());
        verifyNoInteractions(movimientos, despachos);
    }

    @Test void inventarioInexistenteSeInformaYNoSeInventaStock() {
        when(inventarios.findByMedicamentoIdAndSucursalIdAndActivoTrue(3L, 5L)).thenReturn(Optional.empty());
        var dto = servicio.obtenerDetalleParaDespacho(1L, 5L).get(0);
        assertFalse(dto.isInventarioRegistrado()); assertEquals(0, dto.getStockDisponible());
        assertEquals("Sin inventario registrado", assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarDespacho(form, "farmacia")).getMessage());
        verifyNoInteractions(movimientos, despachos);
    }

    @Test void stockInsuficienteNoDescuentaNiRegistra() {
        inventario.setStockActual(1);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        assertEquals(1, inventario.getStockActual()); verifyNoInteractions(movimientos, despachos);
    }

    @ParameterizedTest @NullSource @ValueSource(ints={-1, 0, 6})
    void cantidadInvalidaONingunaSeleccionNoGeneraDespacho(Integer cantidad) {
        item.setCantidad(cantidad);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        verifyNoInteractions(movimientos, despachos);
    }

    @Test void detalleAjenoODuplicadoEsRechazado() {
        Receta ajena = new Receta(); ajena.setId(99L); linea.setReceta(ajena);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        linea.setReceta(receta); form.setDetalles(List.of(item, item));
        // Validar repetición antes de descontar evita un segundo ítem para la misma línea.
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        verify(despachos, never()).save(any());
    }

    @Test void despachoParcialCalculaTotalRealDescuentaYPersisteTrazabilidad() {
        var d = servicio.registrarDespacho(form, "farmacia");
        assertEquals(new BigDecimal("20.00"), d.getMontoTotal()); assertSame(farmacia, d.getUsuario());
        assertSame(receta, d.getReceta()); assertSame(sucursal, d.getSucursal());
        assertEquals(3, inventario.getStockActual()); assertEquals(2, linea.getCantidadDespachada());
        assertEquals(EstadoReceta.PARCIAL, receta.getEstado()); assertEquals(1, d.getDetalles().size());
        assertSame(original, d.getDetalles().get(0).getMedicamentoDespachado());
        verify(despachos).save(d); verify(movimientos).save(any()); verify(auditorias, times(2)).save(any());
    }

    @Test void despachoCompletoCierraRecetaYBloqueaRepeticion() {
        item.setCantidad(5); servicio.registrarDespacho(form, "farmacia");
        assertEquals(EstadoReceta.DESPACHADA, receta.getEstado());
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        verify(despachos, times(1)).save(any());
    }

    @Test void mismoFormularioParcialNoSePuedeVolverADespachar() {
        servicio.registrarDespacho(form, "farmacia");
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        assertEquals(3, inventario.getStockActual()); verify(despachos, times(1)).save(any());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"corto", "x"})
    void sustitucionRequiereRazonCompatible(String motivo) {
        item.setMedicamentoDespachadoId(4L); item.setSustituir(true); item.setMotivoSustitucion(motivo);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        verifyNoInteractions(movimientos, despachos);
    }

    @Test void sustitucionConservaOriginalAlternativaMotivoYNotificaAlMedico() {
        item.setMedicamentoDespachadoId(4L); item.setSustituir(true); item.setMotivoSustitucion("Presentación disponible");
        var d = servicio.registrarDespacho(form, "farmacia");
        var entregado = d.getDetalles().get(0);
        assertSame(original, entregado.getRecetaDetalle().getMedicamento());
        assertSame(alternativo, entregado.getMedicamentoDespachado()); assertTrue(entregado.getSustituido());
        assertEquals("Presentación disponible", entregado.getMotivoSustitucion());
        assertEquals(5, inventario.getStockActual()); assertEquals(6, inventarioAlternativo.getStockActual());
        assertEquals(new BigDecimal("15.00"), d.getMontoTotal());
        verify(notificaciones).save(argThat(n -> n.getUsuarioDestino() == doctor && n.getMensaje().equals(
                "Medicamento Original sustituido por Alternativo. El médico tratante será notificado de la sustitución.")));
    }

    @Test void alternativoSinStockOInactivoNoSeEntrega() {
        item.setMedicamentoDespachadoId(4L); item.setSustituir(true); item.setMotivoSustitucion("Presentación disponible");
        inventarioAlternativo.setStockActual(1);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        inventarioAlternativo.setStockActual(8); alternativo.setActivo(false);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        verifyNoInteractions(movimientos, despachos, notificaciones);
    }

    @Test void originalSinInventarioPuedeSustituirsePorAlternativaDisponible() {
        when(inventarios.findByMedicamentoIdAndSucursalIdAndActivoTrue(3L, 5L)).thenReturn(Optional.empty());
        item.setSustituir(true); item.setMedicamentoDespachadoId(4L); item.setMotivoSustitucion("Original sin existencias");
        assertEquals(new BigDecimal("15.00"), servicio.registrarDespacho(form, "farmacia").getMontoTotal());
        assertEquals(6, inventarioAlternativo.getStockActual());
    }

    @Test void stockMinimoMuestraAlertaDetalladaPosterior() {
        item.setCantidad(3); var d = servicio.registrarDespacho(form, "farmacia");
        assertTrue(d.getAvisos().contains("ALERTA: El medicamento Original ha alcanzado el nivel de stock mínimo (2 unidades restantes). Se recomienda generar orden de reabastecimiento."));
    }

    @Test void pacienteNoAdquiereNoCreaDespachoNiCambiaInventario() {
        var flash = new RedirectAttributesModelMap();
        controller.marcarComoNoAdquirida(1L, new UsernamePasswordAuthenticationToken("farmacia", "irrelevante"), flash);
        assertEquals("Se ha registrado que el paciente Ana Paciente no adquirió los medicamentos recetados en farmacia interna. Receta: 1.",
                flash.getFlashAttributes().get("mensaje"));
        assertEquals(EstadoReceta.NO_ADQUIRIDA, receta.getEstado()); assertEquals(5, inventario.getStockActual());
        verifyNoInteractions(despachos, movimientos);
        verify(auditorias).save(argThat(a -> a.getDetalle().equals("Se ha registrado que el paciente Ana Paciente no adquirió los medicamentos recetados en farmacia interna. Receta: 1.")));
    }

    @Test void precioTotalDosisYUsuarioDelFormularioSeIgnoran() {
        var binder = new WebDataBinder(form, "despachoForm"); controller.camposDespacho(binder);
        binder.bind(new MutablePropertyValues(Map.of("montoTotal", "0.01", "detalles[0].precioUnitario", "0.01",
                "usuario", "otro", "detalles[0].dosis", "dosis manipulada")));
        var flash = new RedirectAttributesModelMap();
        assertEquals("redirect:/farmacia/recetas/1/historial", controller.registrarDespacho(form,
                binder.getBindingResult(), new UsernamePasswordAuthenticationToken("farmacia", "irrelevante"), flash));
        assertEquals("Despacho registrado exitosamente. 1 medicamento(s) despachado(s). Total: Q20.00.", flash.getFlashAttributes().get("mensaje"));
        assertEquals(4, binder.getBindingResult().getSuppressedFields().length);
    }

    @Test void cantidadesDecimalesNoSeAceptanEnBinding() {
        var binder = new WebDataBinder(form, "despachoForm");
        binder.bind(new MutablePropertyValues(Map.of("detalles[0].cantidad", "2.5")));
        var flash = new RedirectAttributesModelMap();
        controller.registrarDespacho(form, binder.getBindingResult(), null, flash);
        assertNotNull(flash.getFlashAttributes().get("error")); verifyNoInteractions(despachos, movimientos);
    }

    String renderizar(String vista, Map<String, Object> variables) {
        var resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/");
        resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var motor = new SpringTemplateEngine(); motor.setTemplateResolver(resolver);
        var servlet = new MockServletContext(); var request = new MockHttpServletRequest(servlet);
        var response = new MockHttpServletResponse();
        try (var contextoSpring = new StaticWebApplicationContext()) {
            contextoSpring.setServletContext(servlet); contextoSpring.refresh();
            servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, contextoSpring);
            var exchange = JakartaServletWebApplication.buildApplication(servlet).buildExchange(request, response);
            var contexto = new WebContext(exchange, Locale.forLanguageTag("es-GT")); contexto.setVariables(variables);
            contexto.setVariable("thymeleafRequestContext", new SpringWebMvcThymeleafRequestContext(
                    new RequestContext(request, response, servlet, variables), request));
            return motor.process(vista, contexto);
        }
    }

    @Test void busquedaMuestraVencidaRojaAccionDeshabilitadaYRetornoInterno() {
        receta.setFechaEmision(LocalDateTime.now().minusDays(8));
        String html = renderizar("farmacia/recetas/lista", Map.of("recetas", List.of(receta), "estados", EstadoReceta.values()));
        assertTrue(html.contains("Vencida")); assertTrue(html.contains("disabled"));
        assertTrue(html.contains("ID de Receta")); assertTrue(html.contains("ID de Consulta"));
        assertTrue(html.contains("href=\"/dashboard\"")); assertFalse(html.contains("href=\"/\""));
    }

    @Test void preparacionMuestraStockMinimoYCamposDeSustitucion() {
        inventario.setStockActual(2);
        var model = new ExtendedModelMap(); controller.abrirDespacho(1L, 5L, model, new RedirectAttributesModelMap());
        String html = renderizar("farmacia/recetas/despacho", model);
        assertTrue(html.contains("Original: Stock bajo — disponible: 2 (mínimo: 2)."));
        assertTrue(html.contains("El stock del medicamento Original ha alcanzado el nivel mínimo. Se requiere reorden."));
        assertTrue(html.contains("Razón de sustitución")); assertTrue(html.contains("Confirmar Despacho"));
        assertTrue(html.contains("Total del Despacho:")); assertFalse(html.contains("href=\"/\""));
    }

    @Test void historialMuestraResumenSinComprobanteFinanciero() {
        var despacho = servicio.registrarDespacho(form, "farmacia");
        String html = renderizar("farmacia/recetas/historial", Map.of("receta", receta, "despachos", List.of(despacho)));
        assertTrue(html.contains("Ana Paciente")); assertTrue(html.contains("Original"));
        assertTrue(html.contains("Total del Despacho: Q20.00."));
        assertFalse(html.contains("Transacción bancaria")); assertFalse(html.contains("href=\"/\""));
    }

    @Configuration @EnableMethodSecurity static class Seguridad { }

    @Test void seguridadExigeFarmaciaOAdministradorYSesionCoincidente() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.register(Seguridad.class);
            contexto.registerBean(DespachoMedicamentoServiceImpl.class, () -> servicio);
            contexto.registerBean(RecetaServiceImpl.class, () -> recetaServicio);
            contexto.registerBean(FarmaciaController.class, () -> controller); contexto.refresh();
            var protegido = contexto.getBean(DespachoMedicamentoService.class);
            var recetasProtegidas = contexto.getBean(RecetaService.class);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> protegido.registrarDespacho(form, "farmacia"));
            for (String rol : List.of("CAJERO", "MEDICO", "PACIENTE")) {
                autenticar(rol);
                assertThrows(AccessDeniedException.class, () -> protegido.registrarDespacho(form, "farmacia"));
                assertThrows(AccessDeniedException.class, () -> recetasProtegidas.buscarActivasFarmacia(null, null));
                assertThrows(AccessDeniedException.class, () -> contexto.getBean(FarmaciaController.class).verReceta(1L, new ExtendedModelMap()));
            }
            autenticar("FARMACEUTICO");
            assertThrows(AccessDeniedException.class, () -> protegido.registrarDespacho(form, "otro"));
            assertThrows(AccessDeniedException.class, () -> recetasProtegidas.marcarComoNoAdquirida(1L, "otro"));
            assertNotNull(protegido.obtenerDetalleParaDespacho(1L, 5L));
            autenticar("ADMINISTRADOR"); assertNotNull(protegido.obtenerDetalleParaDespacho(1L, 5L));
        }
    }

    void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("farmacia", "irrelevante",
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
