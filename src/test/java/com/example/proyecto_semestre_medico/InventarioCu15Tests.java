package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.MovimientoInventarioController;
import com.example.proyecto_semestre_medico.dto.*;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.MovimientoInventarioServiceImpl;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.ExtendedModelMap;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** H2 aislada: no inicia el HIS, PostgreSQL, correo ni tareas programadas. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventarioCu15Tests {
    @Configuration @EnableTransactionManagement @EnableMethodSecurity
    @Import({MovimientoInventarioServiceImpl.class, MovimientoInventarioController.class})
    static class Configuracion {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:cu15_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", ""); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource fuente) {
            var f = new LocalContainerEntityManagerFactoryBean(); f.setDataSource(fuente);
            f.setPackagesToScan("com.example.proyecto_semestre_medico.model"); f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop")); return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f) { return new JpaTransactionManager(f); }
        static <T,R extends JpaRepository<T,Long>> JpaRepositoryFactoryBean<R,T,Long> repo(Class<R> tipo, EntityManagerFactory f) {
            var r = new JpaRepositoryFactoryBean<R,T,Long>(tipo); r.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(f)); return r;
        }
        @Bean JpaRepositoryFactoryBean<InventarioMedicamentoRepository,InventarioMedicamento,Long> inventarios(EntityManagerFactory f) { return repo(InventarioMedicamentoRepository.class,f); }
        @Bean JpaRepositoryFactoryBean<MovimientoInventarioMedicamentoRepository,MovimientoInventarioMedicamento,Long> movimientos(EntityManagerFactory f) { return repo(MovimientoInventarioMedicamentoRepository.class,f); }
        @Bean JpaRepositoryFactoryBean<UsuarioRepository,Usuario,Long> usuarios(EntityManagerFactory f) { return repo(UsuarioRepository.class,f); }
        @Bean JpaRepositoryFactoryBean<AuditoriaRepository,Auditoria,Long> auditorias(EntityManagerFactory f) { return repo(AuditoriaRepository.class,f); }
        @Bean InventarioMedicamentoService catalogo() { return mock(InventarioMedicamentoService.class); }
    }
    AnnotationConfigApplicationContext contexto;
    EntityManager em; JdbcTemplate jdbc; TransactionTemplate tx;
    MovimientoInventarioService servicio; MovimientoInventarioController controlador; MockMvc mvc;
    Long inventarioId, medicamentoId, sucursalId, usuarioId;
    @BeforeAll void abrir() {
        contexto = new AnnotationConfigApplicationContext(Configuracion.class);
        em = SharedEntityManagerCreator.createSharedEntityManager(contexto.getBean(EntityManagerFactory.class));
        jdbc = new JdbcTemplate(contexto.getBean(DataSource.class)); tx = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        servicio = contexto.getBean(MovimientoInventarioService.class); controlador = contexto.getBean(MovimientoInventarioController.class);
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controlador).build();
    }
    static void autenticar(String nombre, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(nombre,"prueba",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
    static void farmacia() { autenticar("farmacia","ROLE_FARMACEUTICO"); }
    @BeforeEach void preparar() {
        farmacia();
        for (String tabla : List.of("auditoria","movimiento_inventario_medicamento","inventario_medicamento"))
            jdbc.execute("alter table " + tabla + " drop constraint if exists fallo_cu15");
        for (String tabla : List.of("auditoria","movimiento_inventario_medicamento","inventario_medicamento","medicamento","sucursal","usuario","rol_permiso","rol"))
            jdbc.update("delete from " + tabla);
        tx.executeWithoutResult(s -> {
            for (String codigo : List.of("FARMACEUTICO","ADMINISTRADOR")) {
                var rol = new Rol(); rol.setNombre(codigo); rol.setCodigo(codigo); em.persist(rol);
                var u = new Usuario(); u.setNombreUsuario(codigo.equals("FARMACEUTICO") ? "farmacia" : "admin"); u.setPasswordHash("prueba"); u.setRol(rol); em.persist(u);
                if (codigo.equals("FARMACEUTICO")) usuarioId = u.getId();
            }
            var suc = new Sucursal(); suc.setNombre("Central"); em.persist(suc); sucursalId = suc.getId();
            var med = new Medicamento(); med.setNombre("Medicamento prueba"); med.setDescripcion("Descripción"); med.setUnidad("Tableta");
            med.setPrecio(new BigDecimal("2.50")); med.setStockMinimo(5); em.persist(med); medicamentoId = med.getId();
            var inv = new InventarioMedicamento(); inv.setMedicamento(med); inv.setSucursal(suc); inv.setStockActual(10); em.persist(inv); inventarioId = inv.getId();
        });
        when(contexto.getBean(InventarioMedicamentoService.class).listarTodos()).thenAnswer(a -> tx.execute(s -> em.createQuery(
                "select i from InventarioMedicamento i join fetch i.medicamento join fetch i.sucursal",InventarioMedicamento.class).getResultList()));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @AfterAll void cerrar() { contexto.close(); }
    MovimientoInventarioDTO datos(TipoMovimientoInventario tipo, Integer cantidad) {
        var d = new MovimientoInventarioDTO(); d.setInventarioId(inventarioId); d.setTipoMovimiento(tipo); d.setCantidad(cantidad);
        d.setCostoUnitario(new BigDecimal("2.50")); d.setMotivo("Justificación de prueba"); return d;
    }
    MovimientoInventarioMedicamento guardar(TipoMovimientoInventario tipo, Integer cantidad) { return servicio.registrarMovimiento(datos(tipo,cantidad),"farmacia"); }
    int stock() { return jdbc.queryForObject("select stock_actual from inventario_medicamento where id=?",Integer.class,inventarioId); }
    int contar(String tabla) { return jdbc.queryForObject("select count(*) from " + tabla,Integer.class); }
    String error(MovimientoInventarioDTO d) { return assertThrows(IllegalArgumentException.class,()->servicio.registrarMovimiento(d,"farmacia")).getMessage(); }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder formulario(String tipo, String cantidad) {
        return post("/farmacia/inventario/guardar").principal(()->"farmacia").param("inventarioId",inventarioId.toString())
                .param("tipoMovimiento",tipo).param("cantidad",cantidad).param("costoUnitario","2.50").param("motivo","Justificación de prueba");
    }

    @ParameterizedTest @ValueSource(strings={"farmacia","admin"})
    void actoresAutorizadosAcceden(String nombre) throws Exception {
        if (nombre.equals("admin")) autenticar("admin","ROLE_ADMINISTRADOR","CATALOG_VIEW","CATALOG_UPDATE");
        mvc.perform(get("/farmacia/inventario")).andExpect(status().isOk()).andExpect(view().name("farmacia/inventario/lista"));
        mvc.perform(get("/farmacia/inventario/nuevo")).andExpect(status().isOk());
    }
    @ParameterizedTest @ValueSource(strings={"MEDICO","PACIENTE","CAJERO"})
    void rolesAjenosRechazados(String rol) {
        autenticar("farmacia","ROLE_"+rol);
        assertThrows(AccessDeniedException.class,()->controlador.nuevo(null,new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class,()->servicio.buscar(new FiltroMovimientoInventarioDTO(),0));
        assertThrows(AccessDeniedException.class,()->guardar(TipoMovimientoInventario.VENTA,1));
    }
    @ParameterizedTest @ValueSource(strings={"usuario","rol"})
    void usuarioORolInactivoNoPuedeLeerNiOperar(String tabla) {
        jdbc.update("update " + tabla + " set activo=false");
        assertThrows(AccessDeniedException.class,()->servicio.validarAcceso());
        assertThrows(AccessDeniedException.class,()->guardar(TipoMovimientoInventario.COMPRA,1));
        assertThrows(Exception.class,()->mvc.perform(get("/farmacia/inventario/nuevo")));
    }
    @Test void anonimoAuditorFalsoYPermisosAdministrativos() {
        assertThrows(AccessDeniedException.class,()->servicio.registrarMovimiento(datos(TipoMovimientoInventario.COMPRA,1),"admin"));
        assertThrows(AccessDeniedException.class,()->servicio.registrarMovimiento(datos(TipoMovimientoInventario.COMPRA,1),null));
        SecurityContextHolder.clearContext(); assertThrows(AccessDeniedException.class,servicio::validarAcceso);
        autenticar("admin","ROLE_ADMINISTRADOR","CATALOG_VIEW");
        assertThrows(AccessDeniedException.class,()->controlador.nuevo(null,new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class,()->controlador.desactivar(1L,()->"admin",new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap()));
    }
    @ParameterizedTest @EnumSource(value=TipoMovimientoInventario.class,names="DESPACHO",mode=EnumSource.Mode.EXCLUDE)
    void direccionesCostosUsuarioFechaStockYAuditoria(TipoMovimientoInventario tipo) {
        LocalDateTime antes=LocalDateTime.now().minusSeconds(1); var m=guardar(tipo,2);
        boolean entrada=Set.of(TipoMovimientoInventario.COMPRA,TipoMovimientoInventario.DEVOLUCION,TipoMovimientoInventario.AJUSTE_POSITIVO).contains(tipo);
        assertEquals(entrada?12:8,stock()); assertEquals(10,m.getStockAnterior()); assertEquals(stock(),m.getStockNuevo());
        assertEquals(usuarioId,m.getUsuario().getId()); assertTrue(m.getFechaMovimiento().isAfter(antes)); assertTrue(m.getActivo());
        assertEquals(tipo==TipoMovimientoInventario.COMPRA?new BigDecimal("2.50"):null,m.getCostoUnitario());
        assertEquals(1,contar("movimiento_inventario_medicamento")); assertEquals(1,contar("auditoria"));
        assertEquals(usuarioId,jdbc.queryForObject("select usuario_id from auditoria",Long.class));
    }
    @ParameterizedTest @NullSource @ValueSource(ints={0,-1})
    void cantidadInvalida(Integer cantidad) {
        assertEquals("La cantidad debe ser un número entero positivo.",error(datos(TipoMovimientoInventario.COMPRA,cantidad))); assertEquals(10,stock());
    }
    @ParameterizedTest @ValueSource(strings={"1.5","2147483648","no-numero"})
    void cantidadManipuladaPorHttpConservaFormulario(String cantidad) throws Exception {
        mvc.perform(formulario("COMPRA",cantidad)).andExpect(status().isOk()).andExpect(view().name("farmacia/inventario/nuevo"))
                .andExpect(model().attribute("error","La cantidad debe ser un número entero positivo."));
        assertEquals(10,stock()); assertEquals(0,contar("movimiento_inventario_medicamento"));
    }
    @Test void seleccionObligatoriaInventarioInexistenteYTipo() {
        var d=datos(TipoMovimientoInventario.COMPRA,1); d.setInventarioId(null);
        assertEquals("Debe seleccionar un medicamento. Debe seleccionar una sucursal.",error(d));
        d.setInventarioId(-1L); assertEquals("Inventario no encontrado.",error(d));
        d.setInventarioId(inventarioId); d.setTipoMovimiento(null); assertEquals("Debe seleccionar el tipo de movimiento.",error(d));
        assertEquals(0,contar("movimiento_inventario_medicamento"));
    }
    @ParameterizedTest @ValueSource(strings={"DESPACHO","TRANSFERENCIA","TRANSFERENCIA_ENTRE_SUCURSALES","INVENTADO"})
    void tipoManualProhibidoPorHttp(String tipo) throws Exception {
        mvc.perform(formulario(tipo,"1")).andExpect(status().isOk()).andExpect(view().name("farmacia/inventario/nuevo")).andExpect(model().attributeExists("error"));
        assertEquals(10,stock()); assertEquals(0,contar("movimiento_inventario_medicamento"));
    }
    @ParameterizedTest @CsvSource({"NULL,El costo unitario es obligatorio para compras","0,El costo unitario debe ser mayor a 0","-1,El costo unitario debe ser mayor a 0","1.001,El costo unitario debe tener máximo 2 decimales"})
    void costoCompraMensajesLiterales(String costo,String mensaje) {
        var d=datos(TipoMovimientoInventario.COMPRA,1); d.setCostoUnitario(costo.equals("NULL")?null:new BigDecimal(costo)); assertEquals(mensaje,error(d));
    }
    @Test void capacidadMonetariaYOverflowStock() {
        var d=datos(TipoMovimientoInventario.COMPRA,1); d.setCostoUnitario(new BigDecimal("100000000.00")); error(d);
        d.setCostoUnitario(new BigDecimal("99999999.99")); servicio.registrarMovimiento(d,"farmacia"); assertEquals(11,stock());
        d.setCantidad(Integer.MAX_VALUE); error(d); assertEquals(11,stock());
    }
    @ParameterizedTest @EnumSource(value=TipoMovimientoInventario.class,names={"AJUSTE_POSITIVO","AJUSTE_NEGATIVO"})
    void motivosObligatoriosDiezAQuinientos(TipoMovimientoInventario tipo) {
        for (String motivo:Arrays.asList(null,"  ","123456789","x".repeat(501))) {
            var d=datos(tipo,1); d.setMotivo(motivo); assertEquals("El motivo debe contener entre 10 y 500 caracteres.",error(d));
        }
        var d=datos(tipo,1); d.setMotivo("  1234567890  "); d.setCostoUnitario(null);
        assertEquals("1234567890",servicio.registrarMovimiento(d,"farmacia").getMotivo());
        d.setMotivo("x".repeat(500)); assertEquals(500,servicio.registrarMovimiento(d,"farmacia").getMotivo().length());
    }
    @ParameterizedTest @EnumSource(value=TipoMovimientoInventario.class,names={"DEVOLUCION","RECLAMO"})
    void devolucionesYReclamosExigenMotivoSinMinimoDeDiez(TipoMovimientoInventario tipo) {
        for (String motivo:Arrays.asList(null,"", "   ")) {
            var d=datos(tipo,1); d.setMotivo(motivo);
            assertEquals(tipo == TipoMovimientoInventario.DEVOLUCION ? "Debe indicar el motivo de la devolución."
                    : "Debe indicar el motivo del reclamo.",error(d));
        }
        var d=datos(tipo,1); d.setMotivo("  Error  ");
        assertEquals("Error",servicio.registrarMovimiento(d,"farmacia").getMotivo());
        d.setMotivo("x".repeat(500)); assertEquals(500,servicio.registrarMovimiento(d,"farmacia").getMotivo().length());
        d.setMotivo("x".repeat(501)); error(d);
    }
    @Test void ventaNoRequiereNiGuardaNotasOcultas() {
        var d=datos(TipoMovimientoInventario.VENTA,1); d.setMotivo(null); d.setCostoUnitario(null);
        assertNull(servicio.registrarMovimiento(d,"farmacia").getMotivo());
        d.setMotivo("x".repeat(501)); assertNull(servicio.registrarMovimiento(d,"farmacia").getMotivo());
    }
    @Test void compraNotasReferenciaOpcionalesYLimites() {
        var d=datos(TipoMovimientoInventario.COMPRA,1); d.setMotivo(null); d.setReferencia(null); servicio.registrarMovimiento(d,"farmacia");
        d.setMotivo("Nota"); d.setReferencia("  Factura 1  "); assertEquals("Factura 1",servicio.registrarMovimiento(d,"farmacia").getReferencia());
        d.setMotivo("x".repeat(501)); error(d); d.setMotivo(null); d.setReferencia("x".repeat(101)); error(d);
    }
    @ParameterizedTest @EnumSource(value=TipoMovimientoInventario.class,names={"VENTA","RECLAMO","AJUSTE_NEGATIVO"})
    void salidasSinStockRechazadas(TipoMovimientoInventario tipo) {
        assertEquals("Stock insuficiente. El stock actual es 10 unidades.",error(datos(tipo,11)));
        assertEquals(10,stock()); assertEquals(0,contar("auditoria"));
        guardar(tipo,10); assertEquals(0,stock());
    }
    @ParameterizedTest @ValueSource(strings={"inventario_medicamento","medicamento","sucursal"})
    void inactivosRechazadosYNoOfrecidos(String tabla) throws Exception {
        jdbc.update("update "+tabla+" set activo=false"); error(datos(TipoMovimientoInventario.COMPRA,1));
        var respuesta=mvc.perform(get("/farmacia/inventario/nuevo")).andExpect(status().isOk()).andReturn();
        assertTrue(((List<?>)respuesta.getModelAndView().getModel().get("inventarios")).isEmpty()); assertEquals(10,stock());
    }
    @Test void camposSensiblesIgnoradosYAlertaNoImpideSalida() throws Exception {
        mvc.perform(formulario("VENTA","8").param("stockAnterior","999").param("stockNuevo","999")
                .param("usuario","admin").param("nombreUsuario","admin").param("activo","false").param("version","999")
                .param("fechaMovimiento","2000-01-01T00:00:00").param("medicamentoId","-1").param("sucursalId","-1"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/farmacia/inventario"))
                .andExpect(flash().attribute("alertaStock","Medicamento prueba: Stock bajo — disponible: 2 (mínimo: 5)"));
        var m=servicio.listarTodos().get(0); assertEquals(10,m.getStockAnterior()); assertEquals(2,m.getStockNuevo()); assertEquals(2,stock());
        assertEquals(usuarioId,m.getUsuario().getId()); assertTrue(m.getActivo()); assertEquals(LocalDate.now(),m.getFechaMovimiento().toLocalDate());
    }
    @ParameterizedTest @CsvSource({"auditoria,false","movimiento_inventario_medicamento,false","inventario_medicamento,false",
            "auditoria,true","movimiento_inventario_medicamento,true","inventario_medicamento,true"})
    void falloPersistenciaRevierteMovimientoStockYAuditoria(String tabla, boolean automatico) {
        String condicion=tabla.equals("inventario_medicamento")?"stock_actual=10":"1=0";
        jdbc.execute("alter table "+tabla+" add constraint fallo_cu15 check ("+condicion+")");
        assertThrows(RuntimeException.class,()->{
            if (automatico) servicio.registrarDespachoAutomatico(medicamentoId,sucursalId,1,"RECETA-1",null,"farmacia");
            else guardar(TipoMovimientoInventario.VENTA,1);
        });
        assertEquals(10,stock()); assertEquals(0,contar("movimiento_inventario_medicamento")); assertEquals(0,contar("auditoria"));
    }
    @ParameterizedTest @ValueSource(strings={"MANUAL","DESPACHO","MIXTO"})
    void dosSalidasConcurrentesNoGastanMismoStock(String modo) throws Exception {
        var barrera=new CyclicBarrier(2); var pool=Executors.newFixedThreadPool(2);
        var turno=new java.util.concurrent.atomic.AtomicInteger();
        Callable<Boolean> operacion=()->{ farmacia(); try {
            boolean automatico=modo.equals("DESPACHO") || (modo.equals("MIXTO") && turno.getAndIncrement()==0);
            tx.executeWithoutResult(s->{ em.find(InventarioMedicamento.class,inventarioId);
                try { barrera.await(10,TimeUnit.SECONDS); } catch(Exception e) { throw new IllegalStateException(e); }
                if (automatico) servicio.registrarDespachoAutomatico(medicamentoId,sucursalId,8,"RECETA-1",null,"farmacia");
                else guardar(TipoMovimientoInventario.VENTA,8);
            }); return true;
        } catch(org.springframework.dao.OptimisticLockingFailureException e) { return false; }
        finally { SecurityContextHolder.clearContext(); } };
        try { var a=pool.submit(operacion); var b=pool.submit(operacion); assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS)); }
        finally { pool.shutdownNow(); }
        assertEquals(2,stock()); assertEquals(1,contar("movimiento_inventario_medicamento")); assertEquals(1,contar("auditoria"));
    }
    @Test void desactivarReactivarNoReescribeHistoricoNiStock() {
        var m=guardar(TipoMovimientoInventario.VENTA,2); var fecha=m.getFechaMovimiento();
        servicio.desactivar(m.getId(),"farmacia"); assertEquals(8,stock()); assertFalse(servicio.buscarPorId(m.getId()).getActivo());
        assertThrows(IllegalArgumentException.class,()->servicio.desactivar(m.getId(),"farmacia"));
        servicio.reactivar(m.getId(),"farmacia"); assertEquals(8,stock()); var historico=servicio.buscarPorId(m.getId());
        assertTrue(historico.getActivo()); assertEquals(m.getStockAnterior(),historico.getStockAnterior()); assertEquals(m.getStockNuevo(),historico.getStockNuevo());
        assertEquals(m.getCantidad(),historico.getCantidad()); assertEquals(usuarioId,historico.getUsuario().getId());
        assertEquals(fecha.withNano(0),historico.getFechaMovimiento().withNano(0)); assertEquals(3,contar("auditoria"));
    }
    @Test void toggleFallaAuditoriaRevierteSoloBandera() {
        var m=guardar(TipoMovimientoInventario.COMPRA,1);
        jdbc.execute("alter table auditoria add constraint fallo_cu15 check (accion='REGISTRAR_MOVIMIENTO_INVENTARIO')");
        assertThrows(RuntimeException.class,()->servicio.desactivar(m.getId(),"farmacia"));
        assertTrue(servicio.buscarPorId(m.getId()).getActivo()); assertEquals(11,stock()); assertEquals(1,contar("auditoria"));
    }
    @Test void inexistenteYAuditorFalsoNoCambianRegistro() {
        var m=guardar(TipoMovimientoInventario.COMPRA,1);
        assertThrows(AccessDeniedException.class,()->servicio.desactivar(m.getId(),"admin"));
        assertThrows(AccessDeniedException.class,()->servicio.reactivar(m.getId(),"admin"));
        assertThrows(IllegalArgumentException.class,()->servicio.buscarPorId(-1L));
        assertThrows(IllegalArgumentException.class,()->servicio.desactivar(-1L,"farmacia")); assertEquals(11,stock());
    }
    @Test void resumenMensualIncluyeHistoricosInactivosYExcluyeOtroPeriodo() {
        var compra=guardar(TipoMovimientoInventario.COMPRA,3); guardar(TipoMovimientoInventario.VENTA,2);
        servicio.desactivar(compra.getId(),"farmacia");
        var fuera=guardar(TipoMovimientoInventario.COMPRA,4);
        jdbc.update("update movimiento_inventario_medicamento set fecha_movimiento=? where id=?",LocalDate.now().withDayOfMonth(1).minusDays(1).atStartOfDay(),fuera.getId());
        var r=servicio.obtenerResumenMensual(LocalDate.now().getYear(),LocalDate.now().getMonthValue());
        assertEquals(2,r.getTotalMovimientos()); assertEquals(3,r.getTotalEntradas()); assertEquals(2,r.getTotalSalidas()); assertEquals(new BigDecimal("7.50"),r.getValorTotalCompras());
        assertThrows(IllegalArgumentException.class,()->servicio.obtenerResumenMensual(2026,13));
    }
    @Test void resumenSieteTiposLimitesDelMesYVistaConDatosAuditables() throws Exception {
        var compra=guardar(TipoMovimientoInventario.COMPRA,3);
        guardar(TipoMovimientoInventario.DEVOLUCION,2); guardar(TipoMovimientoInventario.AJUSTE_POSITIVO,4);
        guardar(TipoMovimientoInventario.VENTA,1); guardar(TipoMovimientoInventario.RECLAMO,2);
        guardar(TipoMovimientoInventario.AJUSTE_NEGATIVO,3);
        var despacho=servicio.registrarDespachoAutomatico(medicamentoId,sucursalId,2,"RECETA-15","Despacho desde CU-11","farmacia");
        servicio.desactivar(compra.getId(),"farmacia");
        var mes=YearMonth.now(); var inicio=mes.atDay(1).atStartOfDay(); var siguiente=mes.plusMonths(1).atDay(1).atStartOfDay();
        jdbc.update("update movimiento_inventario_medicamento set fecha_movimiento=? where id=?",inicio,compra.getId());
        jdbc.update("update movimiento_inventario_medicamento set fecha_movimiento=? where id=?",siguiente.minusSeconds(1),despacho.getId());
        var fuera=guardar(TipoMovimientoInventario.COMPRA,1);
        jdbc.update("update movimiento_inventario_medicamento set fecha_movimiento=? where id=?",siguiente,fuera.getId());
        var vista=mvc.perform(get("/farmacia/inventario/resumen").param("anio",String.valueOf(mes.getYear()))
                .param("mes",String.valueOf(mes.getMonthValue()))).andExpect(status().isOk()).andReturn();
        var modelo=vista.getModelAndView().getModel(); var r=(ResumenMensualInventarioDTO)modelo.get("resumen");
        assertEquals(7,r.getTotalMovimientos()); assertEquals(9,r.getTotalEntradas()); assertEquals(8,r.getTotalSalidas());
        assertEquals(3,r.getCompras()); assertEquals(2,r.getDevoluciones()); assertEquals(4,r.getAjustesPositivos());
        assertEquals(1,r.getVentas()); assertEquals(2,r.getReclamos()); assertEquals(3,r.getAjustesNegativos()); assertEquals(2,r.getDespachos());
        assertEquals(new BigDecimal("7.50"),r.getValorTotalCompras()); assertSame(r.getMovimientos(),modelo.get("movimientos"));
        String html=renderizar("resumen",modelo);
        for(String dato:List.of("RECETA-15","Inactivo","farmacia","Costo unitario","Referencia","Estado","Detalle","Despachos"))
            assertTrue(html.contains(dato),dato);
        assertFalse(r.getMovimientos().stream().anyMatch(m->m.getId().equals(fuera.getId())));
    }
    @Test void resumenNoDesbordaAlAcumularUnidadesDeVariosMovimientos() {
        for(int i=0;i<2;i++) { guardar(TipoMovimientoInventario.COMPRA,1_500_000_000); guardar(TipoMovimientoInventario.VENTA,1_500_000_000); }
        var r=servicio.obtenerResumenMensual(LocalDate.now().getYear(),LocalDate.now().getMonthValue());
        assertEquals(3_000_000_000L,r.getTotalEntradas()); assertEquals(3_000_000_000L,r.getTotalSalidas());
        assertEquals(3_000_000_000L,r.getCompras()); assertEquals(3_000_000_000L,r.getVentas()); assertEquals(10,stock());
        assertEquals(new BigDecimal("7500000000.00"),r.getValorTotalCompras());
    }
    @Test void resumenVacioYPeriodoInvalidoRenderizanSinModificarStock() throws Exception {
        var vacio=mvc.perform(get("/farmacia/inventario/resumen")).andExpect(status().isOk()).andReturn();
        assertTrue(renderizar("resumen",vacio.getModelAndView().getModel()).contains("No existen movimientos para el período seleccionado."));
        var invalido=mvc.perform(get("/farmacia/inventario/resumen").param("mes","13")).andExpect(status().isOk()).andExpect(model().attributeExists("error")).andReturn();
        assertTrue(renderizar("resumen",invalido.getModelAndView().getModel()).contains("El mes debe estar entre 1 y 12."));
        assertEquals(10,stock());
    }
    @Test void despachoAutomaticoUsaUsuarioRealYSePuedeFiltrarYDesactivarSinReponer() {
        assertThrows(AccessDeniedException.class,()->servicio.registrarDespachoAutomatico(medicamentoId,sucursalId,1,"RECETA-1",null,"admin"));
        var m=servicio.registrarDespachoAutomatico(medicamentoId,sucursalId,1,"RECETA-1",null,"farmacia");
        var f=new FiltroMovimientoInventarioDTO(); f.setTipo("DESPACHO");
        assertEquals(m.getId(),servicio.buscar(f,0).getContent().get(0).getId()); assertEquals(usuarioId,m.getUsuario().getId());
        servicio.desactivar(m.getId(),"farmacia"); servicio.reactivar(m.getId(),"farmacia"); assertEquals(9,stock());
        assertEquals(10,servicio.buscarPorId(m.getId()).getStockAnterior()); assertEquals(9,servicio.buscarPorId(m.getId()).getStockNuevo());
    }
    @ParameterizedTest @ValueSource(strings={"tipo","medicamento","sucursal","referencia","usuario","desde","hasta","inventarioId","estado"})
    void filtrosEnBaseDeDatos(String campo) {
        var d=datos(TipoMovimientoInventario.COMPRA,1); d.setReferencia("Factura X"); var m=servicio.registrarMovimiento(d,"farmacia");
        var filtro=new FiltroMovimientoInventarioDTO(); var bean=new org.springframework.beans.BeanWrapperImpl(filtro);
        Object coincide=switch(campo) { case "tipo"->"COMPRA"; case "medicamento"->" PRUEBA "; case "sucursal"->"CENTRAL"; case "referencia"->"factura x"; case "usuario"->"farmacia"; case "estado"->"ACTIVO"; case "inventarioId"->inventarioId; default->LocalDate.now(); };
        bean.setPropertyValue(campo,coincide); assertEquals(1,servicio.buscar(filtro,0).getTotalElements());
        Object noCoincide=switch(campo) { case "tipo"->"VENTA"; case "estado"->"INACTIVO"; case "inventarioId"->-1L; case "desde"->LocalDate.now().plusDays(1); case "hasta"->LocalDate.now().minusDays(1); default->"Inexistente"; };
        bean.setPropertyValue(campo,noCoincide); assertEquals(0,servicio.buscar(filtro,0).getTotalElements());
        assertEquals(m.getId(),servicio.listarPorInventario(inventarioId).get(0).getId());
        assertEquals("Medicamento prueba",servicio.listarFiltrados(inventarioId,"COMPRA","ACTIVO").get(0).getInventario().getMedicamento().getNombre());
    }
    @Test void paginacionConservaFiltrosYDetalleRenderizaDatosPersistidos() throws Exception {
        for(int i=0;i<21;i++) { var d=datos(TipoMovimientoInventario.COMPRA,1); d.setReferencia("Factura & X"); servicio.registrarMovimiento(d,"farmacia"); }
        var resultado=mvc.perform(get("/farmacia/inventario").param("referencia","Factura & X")).andExpect(status().isOk()).andReturn();
        var modelo=resultado.getModelAndView().getModel(); assertEquals(20,((List<?>)modelo.get("movimientos")).size());
        String html=renderizar("lista",modelo); assertTrue(html.contains("Siguiente")); assertTrue(html.contains("referencia=Factura"));
        for(String columna:List.of("Tipo","Medicamento","Sucursal","Cantidad","Stock anterior","Stock nuevo","Costo unitario","Referencia","Usuario","Fecha","Ver")) assertTrue(html.contains(columna),columna);
        var f=new FiltroMovimientoInventarioDTO(); f.setReferencia("Factura & X"); assertEquals(1,servicio.buscar(f,1).getContent().size());
        var primero=servicio.listarTodos().get(20);
        var detalle=mvc.perform(get("/farmacia/inventario/detalle/"+primero.getId())).andExpect(status().isOk()).andReturn();
        String vista=renderizar("detalle",detalle.getModelAndView().getModel()); assertTrue(vista.contains("Factura & X")); assertTrue(vista.contains("Justificación de prueba"));
        assertEquals(10,((MovimientoInventarioMedicamento)detalle.getModelAndView().getModel().get("movimiento")).getStockAnterior());
        assertFalse(vista.contains("<form"));
    }
    @Test void filtrosInvalidosMuestranErrorYNoDevuelvenResultados() throws Exception {
        guardar(TipoMovimientoInventario.COMPRA,1);
        for (var consulta:List.of(get("/farmacia/inventario").param("desde","fecha-invalida"),
                get("/farmacia/inventario").param("desde","2026-10-23").param("hasta","2026-10-22"),
                get("/farmacia/inventario").param("tipo","TRANSFERENCIA_ENTRE_SUCURSALES"),
                get("/farmacia/inventario").param("estado","INVENTADO"))) {
            var r=mvc.perform(consulta).andExpect(status().isOk()).andExpect(model().attributeExists("error")).andReturn();
            assertTrue(((List<?>)r.getModelAndView().getModel().get("movimientos")).isEmpty());
            assertTrue(renderizar("lista",r.getModelAndView().getModel()).contains("No se encontraron resultados."));
        }
        assertEquals(11,stock()); assertEquals(1,contar("movimiento_inventario_medicamento"));
    }
    @Test void filtrosCombinadosTratanComodinesComoTextoLiteral() {
        var d=datos(TipoMovimientoInventario.COMPRA,1); d.setReferencia("Factura 100%_!"); servicio.registrarMovimiento(d,"farmacia");
        d.setReferencia("Factura 100XYZ"); servicio.registrarMovimiento(d,"farmacia");
        var f=new FiltroMovimientoInventarioDTO(); f.setReferencia("%_!"); f.setMedicamento("prueba");
        f.setSucursal("central"); f.setUsuario("farmacia"); f.setTipo("compra"); f.setEstado("ACTIVO");
        f.setDesde(LocalDate.now()); f.setHasta(LocalDate.now()); f.setInventarioId(inventarioId);
        assertEquals(1,servicio.buscar(f,0).getTotalElements());
        assertEquals("Factura 100%_!",servicio.buscar(f,0).getContent().get(0).getReferencia());
    }
    @Test void vacioCancelarSinPersistirYSinEndpointEliminar() throws Exception {
        var listado=mvc.perform(get("/farmacia/inventario")).andReturn(); assertTrue(renderizar("lista",listado.getModelAndView().getModel()).contains("No se encontraron resultados."));
        var nuevo=mvc.perform(get("/farmacia/inventario/nuevo")).andReturn(); String html=renderizar("nuevo",nuevo.getModelAndView().getModel());
        assertTrue(html.contains("Cancelar")); assertTrue(html.contains("href=\"/farmacia/inventario\"")); assertFalse(html.contains("value=\"DESPACHO\""));
        assertTrue(html.contains("data-stock=\"10\"")); assertTrue(html.contains("data-minimo=\"5\""));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/cu15-formulario.html"),html);
        mvc.perform(delete("/farmacia/inventario/1")).andExpect(status().isNotFound());
        assertEquals(0,contar("movimiento_inventario_medicamento")); assertEquals(10,stock());
    }
    String renderizar(String vista,Map<String,Object> variables) {
        var servlet=new org.springframework.mock.web.MockServletContext(); var request=new org.springframework.mock.web.MockHttpServletRequest(servlet);
        request.setRequestURI("/farmacia/inventario"); var response=new org.springframework.mock.web.MockHttpServletResponse();
        try(var web=new org.springframework.web.context.support.StaticWebApplicationContext()) {
            web.setServletContext(servlet); web.refresh(); servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,web);
            var resolver=new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver(); resolver.setPrefix("templates/farmacia/inventario/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
            var motor=new org.thymeleaf.spring6.SpringTemplateEngine(); motor.setTemplateResolver(resolver);
            var exchange=org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response);
            var ctx=new org.thymeleaf.context.WebContext(exchange,Locale.forLanguageTag("es-GT")); ctx.setVariables(variables);
            ctx.setVariable("thymeleafRequestContext",new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(
                    new org.springframework.web.servlet.support.RequestContext(request,response,servlet,variables),request));
            return org.springframework.web.util.HtmlUtils.htmlUnescape(motor.process(vista,ctx));
        }
    }
}
