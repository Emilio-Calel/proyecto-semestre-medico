package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.SucursalEspecialidadController;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Contexto aislado: solo H2 efímera; no carga application.properties ni la BD del HIS.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SucursalEspecialidadCu13Tests {
    static final String DUPLICADO = "Esta combinación de sede y especialidad ya existe en el sistema.";
    static final String LISTA = "/admin/sucursal-especialidad";
    AnnotationConfigApplicationContext contexto;
    SucursalEspecialidadService servicio;
    CitaCatalogoService catalogos;
    SucursalEspecialidadController controller;
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    EntityManager em;
    CacheManager caches;
    Long sucursalId, otraSucursalId, inactivaId, especialidadId, otraEspecialidadId, especialidadInactivaId, usuarioId;

    @Configuration @EnableTransactionManagement @EnableMethodSecurity @EnableCaching
    static class Configuracion {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:cu13_" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource fuente) {
            var f = new LocalContainerEntityManagerFactoryBean(); f.setDataSource(fuente);
            f.setPackagesToScan("com.example.proyecto_semestre_medico.model");
            f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop")); return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f) { return new JpaTransactionManager(f); }
        static <T, R extends JpaRepository<T, Long>> JpaRepositoryFactoryBean<R, T, Long> repo(Class<R> tipo, EntityManagerFactory f) {
            var r = new JpaRepositoryFactoryBean<R, T, Long>(tipo);
            r.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(f)); return r;
        }
        @Bean JpaRepositoryFactoryBean<SucursalRepository, Sucursal, Long> sucursales(EntityManagerFactory f) { return repo(SucursalRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<EspecialidadRepository, Especialidad, Long> especialidades(EntityManagerFactory f) { return repo(EspecialidadRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<SucursalEspecialidadRepository, SucursalEspecialidad, Long> relaciones(EntityManagerFactory f) { return repo(SucursalEspecialidadRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<UsuarioRepository, Usuario, Long> usuarios(EntityManagerFactory f) { return repo(UsuarioRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<AuditoriaRepository, Auditoria, Long> auditorias(EntityManagerFactory f) { return repo(AuditoriaRepository.class, f); }
        @Bean MedicoRepository medicos() { return mock(MedicoRepository.class); }
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("citaEspecialidadesPorSucursal", "citaMedicosPorSucursalEspecialidad", "citaSucursalesActivas", "ajena");
        }
        @Bean SucursalEspecialidadService servicio(SucursalEspecialidadRepository r, SucursalRepository s,
                EspecialidadRepository e, UsuarioRepository u, AuditoriaRepository a, CacheManager c) {
            return new SucursalEspecialidadServiceImpl(r, s, e, u, a, c);
        }
        @Bean CitaCatalogoService catalogos(SucursalRepository s, SucursalEspecialidadRepository r, MedicoRepository m) {
            return new CitaCatalogoServiceImpl(s, r, m);
        }
        @Bean SucursalEspecialidadController controller(SucursalEspecialidadService s) { return new SucursalEspecialidadController(s); }
    }

    static void autenticar(String usuario, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario, "prueba",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
    static void admin() { autenticar("admin", "ROLE_ADMINISTRADOR", "CATALOG_VIEW", "CATALOG_CREATE", "CATALOG_DISABLE"); }
    @BeforeAll void abrir() {
        contexto = new AnnotationConfigApplicationContext(Configuracion.class);
        servicio = contexto.getBean(SucursalEspecialidadService.class); catalogos = contexto.getBean(CitaCatalogoService.class);
        controller = contexto.getBean(SucursalEspecialidadController.class); caches = contexto.getBean(CacheManager.class);
        jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
        tx = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        em = SharedEntityManagerCreator.createSharedEntityManager(contexto.getBean(EntityManagerFactory.class));
    }
    @BeforeEach void preparar() {
        admin();
        jdbc.execute("alter table auditoria drop constraint if exists fallo_auditoria");
        for (String tabla : List.of("auditoria", "sucursal_especialidad", "usuario", "rol", "especialidad", "sucursal")) jdbc.update("delete from " + tabla);
        caches.getCacheNames().forEach(n -> caches.getCache(n).clear());
        tx.executeWithoutResult(s -> {
            Rol rol = new Rol(); rol.setCodigo("ADMINISTRADOR"); rol.setNombre("Administrador"); em.persist(rol);
            Usuario u = new Usuario(); u.setNombreUsuario("admin"); u.setRol(rol); u.setPasswordHash("solo-prueba"); em.persist(u); usuarioId = u.getId();
            Sucursal a = sucursal("Central", true); sucursalId = a.getId();
            otraSucursalId = sucursal("Norte", true).getId(); inactivaId = sucursal("Cerrada", false).getId();
            especialidadId = especialidad("Cardiología", true).getId(); otraEspecialidadId = especialidad("General", true).getId();
            especialidadInactivaId = especialidad("Inactiva", false).getId();
        });
    }
    Sucursal sucursal(String nombre, boolean activo) { var s = new Sucursal(); s.setNombre(nombre); s.setActivo(activo); em.persist(s); return s; }
    Especialidad especialidad(String nombre, boolean activo) { var e = new Especialidad(); e.setNombre(nombre); e.setDescripcion("CU-13"); e.setActivo(activo); em.persist(e); return e; }
    SucursalEspecialidad asignar() { return servicio.asignar(sucursalId, especialidadId, "admin"); }
    int relaciones() { return jdbc.queryForObject("select count(*) from sucursal_especialidad", Integer.class); }
    int auditorias() { return jdbc.queryForObject("select count(*) from auditoria", Integer.class); }
    boolean activo(Long id) { return jdbc.queryForObject("select activo from sucursal_especialidad where id=?", Boolean.class, id); }
    void falloAuditoria() {
        Long ultimoId = jdbc.queryForObject("select coalesce(max(id),0) from auditoria", Long.class);
        jdbc.execute("alter table auditoria add constraint fallo_auditoria check (id <= " + ultimoId + ")");
    }
    void rechaza(Long s, Long e, String mensaje) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, () -> servicio.asignar(s, e, "admin")).getMessage());
        assertEquals(0, relaciones()); assertEquals(0, auditorias());
    }
    @AfterEach void limpiarSeguridad() { SecurityContextHolder.clearContext(); }
    @AfterAll void cerrar() { if (contexto != null) contexto.close(); }

    @Test void administradorAbreListadoVacioConAsignarDisponible() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get(LISTA)).andExpect(status().isOk()).andExpect(view().name("admin/sucursal-especialidad/lista"))
                .andExpect(model().attribute("asignaciones", List.of())).andExpect(model().attribute("totalElementos", 0L));
        String html = renderizar("admin/sucursal-especialidad/lista", modeloLista());
        assertTrue(html.contains("No se encontraron resultados.")); assertTrue(html.contains("Asignar Especialidad"));
    }
    @ParameterizedTest @ValueSource(strings = {"MEDICO", "PACIENTE", "CAJERO", "FARMACEUTICO"})
    void noAdministradorNiConPermisosCatalogoPuedeAcceder(String rol) {
        autenticar("otro", "ROLE_" + rol, "CATALOG_VIEW", "CATALOG_CREATE", "CATALOG_DISABLE");
        assertThrows(AccessDeniedException.class, () -> controller.listar(null, 0, new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class, () -> controller.nueva(new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class, () -> controller.guardar(sucursalId, especialidadId, () -> "otro", new ExtendedModelMap(), new RedirectAttributesModelMap()));
        assertThrows(AccessDeniedException.class, () -> controller.desactivar(1L, () -> "otro", new RedirectAttributesModelMap()));
        assertThrows(AccessDeniedException.class, () -> servicio.listar(null, 0));
        assertThrows(AccessDeniedException.class, () -> servicio.asignar(sucursalId, especialidadId, "otro"));
        assertThrows(AccessDeniedException.class, () -> servicio.desactivar(1L, "otro"));
        assertEquals(0, relaciones());
    }
    @Test void administradorSinPermisosGranularesNoLosEvita() {
        autenticar("admin", "ROLE_ADMINISTRADOR");
        assertThrows(AccessDeniedException.class, () -> controller.listar(null, 0, new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class, () -> controller.nueva(new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class, this::asignar);
        assertThrows(AccessDeniedException.class, () -> servicio.desactivar(1L, "admin"));
    }
    @Test void anonimoRechazado() {
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.security.core.AuthenticationException.class, () -> controller.listar(null, 0, new ExtendedModelMap()));
    }
    @Test void dropdownsSoloIncluyenCatalogosActivos() {
        var modelo = new ExtendedModelMap(); assertEquals("admin/sucursal-especialidad/nueva", controller.nueva(modelo));
        assertEquals(List.of(sucursalId, otraSucursalId), servicio.listarSucursalesActivas().stream().map(Sucursal::getId).toList());
        assertEquals(List.of(especialidadId, otraEspecialidadId), servicio.listarEspecialidadesActivas().stream().map(Especialidad::getId).toList());
        String html = renderizar("admin/sucursal-especialidad/nueva", modelo);
        assertTrue(html.contains("Central")); assertTrue(html.contains("Cardiología"));
        assertFalse(html.contains(">Cerrada<")); assertFalse(html.contains(">Inactiva<"));
    }
    @Test void sedeObligatoria() { rechaza(null, especialidadId, "Debe seleccionar una sede."); }
    @Test void especialidadObligatoria() { rechaza(sucursalId, null, "Debe seleccionar una especialidad."); }
    @Test void sucursalInexistente() { rechaza(-1L, especialidadId, "La sede seleccionada no existe."); }
    @Test void sucursalInactiva() { rechaza(inactivaId, especialidadId, "La sede seleccionada está inactiva."); }
    @Test void especialidadInexistente() { rechaza(sucursalId, -1L, "La especialidad seleccionada no existe."); }
    @Test void especialidadInactiva() { rechaza(sucursalId, especialidadInactivaId, "La especialidad seleccionada está inactiva."); }
    @Test void asignacionSePersisteActivaYAuditaUsuarioAutenticado() {
        var r = asignar(); assertEquals(1, relaciones()); assertTrue(activo(r.getId()));
        assertEquals(usuarioId, jdbc.queryForObject("select usuario_id from auditoria", Long.class));
        assertEquals("CREAR_SUCURSAL_ESPECIALIDAD", jdbc.queryForObject("select accion from auditoria", String.class));
        assertEquals(r.getId().toString(), jdbc.queryForObject("select entidad_id from auditoria", String.class));
    }
    @Test void duplicadoActivoUsaMensajeLiteral() {
        asignar(); assertEquals(DUPLICADO, assertThrows(IllegalArgumentException.class, this::asignar).getMessage());
        assertEquals(1, relaciones()); assertEquals(1, auditorias());
    }
    @Test void combinacionesDiferentesPermitidas() {
        asignar(); servicio.asignar(otraSucursalId, especialidadId, "admin"); servicio.asignar(sucursalId, otraEspecialidadId, "admin");
        assertEquals(3, relaciones());
    }
    @Test void exitoLiteralYEstadoIgnoraFormularioManipulado() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post(LISTA + "/guardar").principal(() -> "admin").param("sucursalId", sucursalId.toString())
                        .param("especialidadId", especialidadId.toString()).param("activo", "false").param("nombreUsuario", "otro"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(LISTA))
                .andExpect(flash().attribute("mensaje", "Especialidad asignada a la sede correctamente"));
        assertEquals(1, jdbc.queryForObject("select count(*) from sucursal_especialidad where activo=true", Integer.class));
        assertEquals(usuarioId, jdbc.queryForObject("select usuario_id from auditoria", Long.class));
    }
    @Test void camposFaltantesConservanFormularioYMensajesBackend() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post(LISTA + "/guardar").principal(() -> "admin").param("especialidadId", especialidadId.toString()))
                .andExpect(status().isOk()).andExpect(view().name("admin/sucursal-especialidad/nueva"))
                .andExpect(model().attribute("error", "Debe seleccionar una sede."))
                .andExpect(model().attribute("especialidadSeleccionada", especialidadId));
        mvc.perform(post(LISTA + "/guardar").principal(() -> "admin").param("sucursalId", sucursalId.toString()))
                .andExpect(model().attribute("error", "Debe seleccionar una especialidad."))
                .andExpect(model().attribute("sucursalSeleccionada", sucursalId));
        assertEquals(0, relaciones()); assertEquals(0, auditorias());
    }
    @Test void idsNoNumericosNoSeProcesan() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post(LISTA + "/guardar").principal(() -> "admin").param("sucursalId", "manipulado"))
                .andExpect(status().isBadRequest()); assertEquals(0, relaciones());
    }
    @Test void usuarioDeAuditoriaNoPuedeSuplantarse() {
        assertThrows(AccessDeniedException.class, () -> servicio.asignar(sucursalId, especialidadId, "otro"));
        assertEquals(0, relaciones());
    }
    @Test void administradorNoPersistidoNoProduceAuditoriaAnonima() {
        autenticar("inexistente", "ROLE_ADMINISTRADOR", "CATALOG_CREATE");
        assertThrows(IllegalArgumentException.class, () -> servicio.asignar(sucursalId, especialidadId, "inexistente"));
        assertEquals(0, relaciones()); assertEquals(0, auditorias());
    }
    @Test void eliminarEsLogicoYAuditado() {
        var r = asignar(); servicio.desactivar(r.getId(), "admin");
        assertEquals(1, relaciones()); assertFalse(activo(r.getId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria where accion='DESACTIVAR_SUCURSAL_ESPECIALIDAD' and usuario_id=?", Integer.class, usuarioId));
        assertTrue(servicio.listar(null, 0).getContent().stream().anyMatch(x -> !x.getActivo()));
    }
    @Test void reactivarConservaIdentidadFechaEHistorial() {
        var r = asignar(); var fecha = jdbc.queryForObject("select fecha_creacion from sucursal_especialidad", java.sql.Timestamp.class);
        servicio.desactivar(r.getId(), "admin"); var reactivada = asignar();
        assertEquals(r.getId(), reactivada.getId()); assertTrue(activo(r.getId())); assertEquals(1, relaciones());
        assertEquals(fecha, jdbc.queryForObject("select fecha_creacion from sucursal_especialidad", java.sql.Timestamp.class));
        assertEquals(3, auditorias());
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria where accion='REACTIVAR_SUCURSAL_ESPECIALIDAD' and usuario_id=?", Integer.class, usuarioId));
    }
    @Test void desactivarInexistenteOYaInactivaNoAuditaOtraOperacion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.desactivar(-1L, "admin"));
        assertThrows(IllegalArgumentException.class, () -> servicio.desactivar(null, "admin"));
        var r = asignar(); servicio.desactivar(r.getId(), "admin");
        assertThrows(IllegalArgumentException.class, () -> servicio.desactivar(r.getId(), "admin")); assertEquals(2, auditorias());
    }
    @Test void rollbackDeCreacionSiAuditoriaFalla() {
        catalogos.listarEspecialidades(sucursalId); falloAuditoria();
        assertThrows(RuntimeException.class, this::asignar); assertEquals(0, relaciones()); assertEquals(0, auditorias());
        assertNotNull(caches.getCache("citaEspecialidadesPorSucursal").get(sucursalId));
    }
    @Test void rollbackDeDesactivacionSiAuditoriaFalla() {
        var r = asignar(); falloAuditoria(); assertThrows(RuntimeException.class, () -> servicio.desactivar(r.getId(), "admin"));
        assertTrue(activo(r.getId())); assertEquals(1, auditorias());
    }
    @Test void rollbackDeReactivacionSiAuditoriaFalla() {
        var r = asignar(); servicio.desactivar(r.getId(), "admin"); falloAuditoria();
        assertThrows(RuntimeException.class, this::asignar); assertFalse(activo(r.getId())); assertEquals(2, auditorias());
    }
    @Test void cu03SoloOfreceAsignacionActivaDeLaSucursal() {
        asignar(); servicio.asignar(otraSucursalId, otraEspecialidadId, "admin");
        assertEquals(List.of(especialidadId), catalogos.listarEspecialidades(sucursalId).stream().map(Especialidad::getId).toList());
        assertEquals(List.of(otraEspecialidadId), catalogos.listarEspecialidades(otraSucursalId).stream().map(Especialidad::getId).toList());
    }
    @Test void cu03NoOfreceEspecialidadInactiva() {
        asignar(); jdbc.update("update especialidad set activo=false where id=?", especialidadId);
        assertTrue(catalogos.listarEspecialidades(sucursalId).isEmpty());
    }
    @Test void cu03NoOfreceSucursalInactivaNiSusEspecialidades() {
        asignar(); jdbc.update("update sucursal set activo=false where id=?", sucursalId);
        assertTrue(catalogos.listarEspecialidades(sucursalId).isEmpty());
        assertFalse(catalogos.listarSucursales().stream().anyMatch(s -> s.getId().equals(sucursalId)));
    }
    @Test void cacheCu03SeActualizaAlAsignarDesactivarYReactivar() {
        assertTrue(catalogos.listarEspecialidades(sucursalId).isEmpty());
        caches.getCache("ajena").put("dato", "conservar");
        caches.getCache("citaMedicosPorSucursalEspecialidad").put("anterior", List.of());
        var r = asignar();
        assertEquals(1, catalogos.listarEspecialidades(sucursalId).size());
        assertNull(caches.getCache("citaMedicosPorSucursalEspecialidad").get("anterior"));
        servicio.desactivar(r.getId(), "admin"); assertTrue(catalogos.listarEspecialidades(sucursalId).isEmpty());
        asignar(); assertEquals(1, catalogos.listarEspecialidades(sucursalId).size());
        assertEquals("conservar", caches.getCache("ajena").get("dato", String.class));
    }
    @Test void invalidacionOcurreTrasCommitNoDuranteTransaccion() {
        catalogos.listarEspecialidades(sucursalId);
        tx.executeWithoutResult(s -> {
            asignar(); assertNotNull(caches.getCache("citaEspecialidadesPorSucursal").get(sucursalId));
        });
        assertNull(caches.getCache("citaEspecialidadesPorSucursal").get(sucursalId));
        assertEquals(1, catalogos.listarEspecialidades(sucursalId).size());
    }
    @Test void filtroIdPaginacionYRelacionesLegibles() {
        var r = asignar();
        tx.executeWithoutResult(s -> {
            for (int i = 0; i < 11; i++) {
                var e = especialidad("Especialidad " + i, true);
                var se = new SucursalEspecialidad(); se.setSucursal(em.getReference(Sucursal.class, sucursalId)); se.setEspecialidad(e); em.persist(se);
            }
        });
        var primera = servicio.listar(null, 0); var segunda = servicio.listar(null, 1);
        assertEquals(12, primera.getTotalElements()); assertEquals(2, primera.getTotalPages());
        assertEquals(10, primera.getContent().size()); assertEquals(2, segunda.getContent().size());
        assertEquals("Central", primera.getContent().get(0).getSucursal().getNombre());
        var filtrada = servicio.listar(r.getId(), 0); assertEquals(1, filtrada.getTotalElements());
        assertEquals(especialidadId, filtrada.getContent().get(0).getEspecialidad().getId());
        assertTrue(servicio.listar(-1L, 0).isEmpty()); assertEquals(0, servicio.listar(null, -1).getNumber());
        var modelo = modeloLista(); modelo.put("paginaActual", 0); modelo.put("totalPaginas", 2); modelo.put("totalElementos", 12L);
        assertTrue(renderizar("admin/sucursal-especialidad/lista", modelo).contains("page=1"));
    }
    @Test void dosSolicitudesConcurrentesSoloCreanUnaRelacionYUnaAuditoria() throws Exception {
        concurrencia(false);
    }
    @Test void dosReactivacionesConcurrentesNoDuplicanHistorial() throws Exception {
        var r = asignar(); servicio.desactivar(r.getId(), "admin"); concurrencia(true);
    }
    void concurrencia(boolean reactivacion) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2); var inicio = new CountDownLatch(1);
        try {
            Callable<String> tarea = () -> {
                admin(); inicio.await();
                try { asignar(); return "OK"; }
                catch (IllegalArgumentException e) { return e.getMessage(); }
                finally { SecurityContextHolder.clearContext(); }
            };
            var a = pool.submit(tarea); var b = pool.submit(tarea); inicio.countDown();
            var resultados = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(1, resultados.stream().filter("OK"::equals).count()); assertEquals(1, resultados.stream().filter(DUPLICADO::equals).count());
            assertEquals(1, relaciones()); assertEquals(reactivacion ? 3 : 1, auditorias());
            assertEquals(1, jdbc.queryForObject("select count(*) from sucursal_especialidad where activo=true", Integer.class));
        } finally { pool.shutdownNow(); }
    }
    @Test void unicidadBdImpideInsertarDuplicadoInclusoInactivo() {
        var r = asignar(); servicio.desactivar(r.getId(), "admin");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("insert into sucursal_especialidad(sucursal_id,especialidad_id,activo,fecha_creacion) values(?,?,true,current_timestamp)", sucursalId, especialidadId));
        assertEquals(1, relaciones());
    }
    @Test void cancelarYVolverAlListadoNoPersisten() throws Exception {
        var modelo = new ExtendedModelMap(); controller.nueva(modelo);
        String html = renderizar("admin/sucursal-especialidad/nueva", modelo);
        assertTrue(html.matches("(?s).*href=\"/admin/sucursal-especialidad\"[^>]*>\\s*Cancelar\\s*</a>.*"));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get(LISTA)).andExpect(status().isOk()); assertEquals(0, relaciones()); assertEquals(0, auditorias());
    }
    @Test void dashboardEnlazaModuloYEliminacionPideConfirmacionLiteral() throws Exception {
        String dashboard = Files.readString(Path.of("src/main/resources/templates/index.html"));
        assertTrue(dashboard.contains("sec:authorize=\"hasRole('ADMINISTRADOR') and hasAuthority('CATALOG_VIEW')\""));
        assertTrue(dashboard.contains("th:href=\"@{/admin/sucursal-especialidad}\""));
        assertTrue(dashboard.contains("Especialidades por Sede"));
        asignar(); String html = renderizar("admin/sucursal-especialidad/lista", modeloLista());
        assertTrue(html.contains("¿Confirmar eliminación?")); assertTrue(html.contains("Eliminar"));
        assertTrue(html.contains("href=\"/dashboard\"")); assertFalse(html.contains("href=\"/\""));
    }
    Map<String, Object> modeloLista() {
        var modelo = new ExtendedModelMap(); controller.listar(null, 0, modelo); return modelo;
    }
    String renderizar(String vista, Map<String, Object> variables) {
        var servlet = new org.springframework.mock.web.MockServletContext();
        var request = new org.springframework.mock.web.MockHttpServletRequest(servlet); request.setRequestURI(LISTA);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        try (var web = new org.springframework.web.context.support.StaticWebApplicationContext()) {
            web.setServletContext(servlet); web.refresh();
            servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, web);
            var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
            var motor = new org.thymeleaf.spring6.SpringTemplateEngine(); motor.setTemplateResolver(resolver);
            var exchange = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet).buildExchange(request, response);
            var contextoVista = new org.thymeleaf.context.WebContext(exchange, Locale.forLanguageTag("es-GT")); contextoVista.setVariables(variables);
            contextoVista.setVariable("thymeleafRequestContext", new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(
                    new org.springframework.web.servlet.support.RequestContext(request, response, servlet, variables), request));
            return org.springframework.web.util.HtmlUtils.htmlUnescape(motor.process(vista, contextoVista));
        }
    }
}
