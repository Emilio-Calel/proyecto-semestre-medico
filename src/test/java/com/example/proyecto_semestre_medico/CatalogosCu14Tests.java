package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.*;
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

// H2 efímera: no carga configuración del HIS, PostgreSQL, correo ni jobs.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogosCu14Tests {
    enum Catalogo {
        ESPECIALIDAD(Especialidad.class, EspecialidadService.class, EspecialidadController.class, "especialidades", "especialidad", "especialidades", 200),
        SUCURSAL(Sucursal.class, SucursalService.class, SucursalController.class, "sucursales", "sucursal", "sucursales", 100),
        ESTADO(EstadoCita.class, EstadoCitaService.class, EstadoCitaController.class, "estados-cita", "estadoCita", "estados", 50),
        LABORATORIO(Laboratorio.class, LaboratorioCatalogoService.class, LaboratorioCatalogoController.class, "laboratorios", "laboratorio", "laboratorios", 200),
        EXAMEN(CatalogoExamen.class, CatalogoExamenService.class, CatalogoExamenController.class, "examenes-laboratorio", "examen", "examenes", 200),
        MEDICAMENTO(Medicamento.class, MedicamentoService.class, MedicamentoController.class, "medicamentos", "medicamento", "medicamentos", 200),
        ROL(Rol.class, RolService.class, RolController.class, "roles", "rol", "roles", 200);
        final Class<?> entidad, servicio, controller;
        final String ruta, atributo, lista;
        final int maximo;
        Catalogo(Class<?> e, Class<?> s, Class<?> c, String r, String a, String l, int m) {
            entidad=e; servicio=s; controller=c; ruta="/admin/"+r; atributo=a; lista=l; maximo=m;
        }
    }
    AnnotationConfigApplicationContext contexto;
    EntityManager em; JdbcTemplate jdbc; TransactionTemplate tx; CacheManager caches;
    Long laboratorioId, sucursalId, medicamentoId, usuarioId;

    @Configuration @EnableTransactionManagement @EnableMethodSecurity @EnableCaching
    @Import({ControlCatalogo.class, EspecialidadServiceImpl.class, SucursalServiceImpl.class, EstadoCitaServiceImpl.class,
        RolServiceImpl.class, LaboratorioCatalogoServiceImpl.class, CatalogoExamenServiceImpl.class, MedicamentoServiceImpl.class,
        InventarioMedicamentoServiceImpl.class, MovimientoInventarioServiceImpl.class, CitaCatalogoServiceImpl.class,
        SucursalEspecialidadServiceImpl.class, EspecialidadController.class, SucursalController.class, EstadoCitaController.class,
        RolController.class, LaboratorioCatalogoController.class, CatalogoExamenController.class, MedicamentoController.class,
        InventarioMedicamentoController.class, MovimientoInventarioController.class, CatalogosController.class})
    static class Configuracion {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:cu14_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", ""); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource fuente) {
            var f = new LocalContainerEntityManagerFactoryBean(); f.setDataSource(fuente);
            f.setPackagesToScan("com.example.proyecto_semestre_medico.model"); f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop")); return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f) { return new JpaTransactionManager(f); }
        static <T, R extends JpaRepository<T, Long>> JpaRepositoryFactoryBean<R, T, Long> repo(Class<R> tipo, EntityManagerFactory f) {
            var r = new JpaRepositoryFactoryBean<R, T, Long>(tipo);
            r.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(f)); return r;
        }
        @Bean JpaRepositoryFactoryBean<SucursalRepository, Sucursal, Long> repoSucursal(EntityManagerFactory f) { return repo(SucursalRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<EspecialidadRepository, Especialidad, Long> repoEspecialidad(EntityManagerFactory f) { return repo(EspecialidadRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<EstadoCitaRepository, EstadoCita, Long> repoEstadoCita(EntityManagerFactory f) { return repo(EstadoCitaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<RolRepository, Rol, Long> repoRol(EntityManagerFactory f) { return repo(RolRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<LaboratorioRepository, Laboratorio, Long> repoLaboratorio(EntityManagerFactory f) { return repo(LaboratorioRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<CatalogoExamenRepository, CatalogoExamen, Long> repoCatalogoExamen(EntityManagerFactory f) { return repo(CatalogoExamenRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<MedicamentoRepository, Medicamento, Long> repoMedicamento(EntityManagerFactory f) { return repo(MedicamentoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<InventarioMedicamentoRepository, InventarioMedicamento, Long> repoInventarioMedicamento(EntityManagerFactory f) { return repo(InventarioMedicamentoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<MovimientoInventarioMedicamentoRepository, MovimientoInventarioMedicamento, Long> repoMovimientoInventarioMedicamento(EntityManagerFactory f) { return repo(MovimientoInventarioMedicamentoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<SucursalEspecialidadRepository, SucursalEspecialidad, Long> repoSucursalEspecialidad(EntityManagerFactory f) { return repo(SucursalEspecialidadRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<UsuarioRepository, Usuario, Long> repoUsuario(EntityManagerFactory f) { return repo(UsuarioRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<AuditoriaRepository, Auditoria, Long> repoAuditoria(EntityManagerFactory f) { return repo(AuditoriaRepository.class, f); }
        @Bean MedicoRepository medicos() { return mock(MedicoRepository.class); }
        @Bean PruebaController prueba(RolService roles) { return new PruebaController(roles,mock(PermisoService.class)); }
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager(); }
    }
    static void autenticar(String nombre, String... permisos) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(nombre, "prueba",
                Arrays.stream(permisos).map(SimpleGrantedAuthority::new).toList()));
    }
    static void admin() { autenticar("admin", "ROLE_ADMINISTRADOR", "CATALOG_VIEW", "CATALOG_CREATE", "CATALOG_UPDATE", "CATALOG_DISABLE"); }
    @BeforeAll void abrir() {
        contexto=new AnnotationConfigApplicationContext(Configuracion.class);
        em=SharedEntityManagerCreator.createSharedEntityManager(contexto.getBean(EntityManagerFactory.class));
        jdbc=new JdbcTemplate(contexto.getBean(DataSource.class)); tx=new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        caches=contexto.getBean(CacheManager.class);
    }
    @BeforeEach void preparar() {
        admin(); jdbc.execute("alter table auditoria drop constraint if exists fallo_auditoria");
        for(String tabla: List.of("auditoria", "movimiento_inventario_medicamento", "inventario_medicamento", "sucursal_especialidad", "catalogo_examen",
                "medicamento", "laboratorio", "especialidad", "sucursal", "estado_cita", "usuario", "rol_permiso", "rol", "permiso")) jdbc.update("delete from " + tabla);
        caches.getCacheNames().forEach(n->caches.getCache(n).clear());
        tx.executeWithoutResult(s->{
            Rol rol=new Rol(); rol.setNombre("Administrador"); rol.setCodigo("ADMINISTRADOR"); em.persist(rol);
            Usuario u=new Usuario(); u.setNombreUsuario("admin"); u.setPasswordHash("prueba"); u.setRol(rol); em.persist(u); usuarioId=u.getId();
            Laboratorio l=new Laboratorio(); l.setNombre("Laboratorio base"); em.persist(l); laboratorioId=l.getId();
            Sucursal suc=new Sucursal(); suc.setNombre("Sucursal base"); em.persist(suc); sucursalId=suc.getId();
            Medicamento m=(Medicamento)nuevo(Catalogo.MEDICAMENTO,"Medicamento base"); em.persist(m); medicamentoId=m.getId();
        });
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @AfterAll void cerrar() { if(contexto!=null) contexto.close(); }
    Object nuevo(Catalogo c, String nombre) {
        try {
            Object x=c.entidad.getConstructor().newInstance(); poner(x,"nombre",nombre); poner(x,"descripcion","Descripción de prueba");
            var b=new org.springframework.beans.BeanWrapperImpl(x);
            if(b.isWritableProperty("codigo")) b.setPropertyValue("codigo", "PRUEBA_"+Math.abs(nombre.hashCode()));
            if(b.isWritableProperty("precio")) b.setPropertyValue("precio",new java.math.BigDecimal("12.50"));
            if(c==Catalogo.MEDICAMENTO) poner(x,"unidad","Tableta");
            return x;
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    void poner(Object x,String campo,Object valor) { new org.springframework.beans.BeanWrapperImpl(x).setPropertyValue(campo,valor); }
    Object dato(Object x,String campo) { return new org.springframework.beans.BeanWrapperImpl(x).getPropertyValue(campo); }
    Long id(Object x) { return (Long)dato(x,"id"); }
    Object llamar(Object bean,String metodo,Class<?>[] tipos,Object... args) {
        try { return bean.getClass().getMethod(metodo,tipos).invoke(bean,args); }
        catch(java.lang.reflect.InvocationTargetException e) { if(e.getCause() instanceof RuntimeException r) throw r; throw new IllegalStateException(e.getCause()); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
    Object guardar(Catalogo c,Object x) { return guardar(c,x,"admin"); }
    Object guardar(Catalogo c,Object x,String usuario) {
        return c==Catalogo.EXAMEN ? llamar(contexto.getBean(c.servicio),"guardar",new Class[]{c.entidad,Long.class,String.class},x,laboratorioId,usuario)
                : llamar(contexto.getBean(c.servicio),"guardar",new Class[]{c.entidad,String.class},x,usuario);
    }
    Object obtener(Catalogo c,Long id) { return llamar(contexto.getBean(c.servicio),"buscarPorId",new Class[]{Long.class},id); }
    void accion(Catalogo c,String metodo,Long id) { llamar(contexto.getBean(c.servicio),metodo,new Class[]{Long.class,String.class},id,"admin"); }
    Map<String,Object> listar(Catalogo c,String q,int pagina) {
        var model=new ExtendedModelMap(); llamar(contexto.getBean(c.controller),"listar",new Class[]{String.class,int.class,org.springframework.ui.Model.class},q,pagina,model); return model;
    }
    int auditorias() { return jdbc.queryForObject("select count(*) from auditoria",Integer.class); }
    void fallaAuditoria() {
        Long ultimo=jdbc.queryForObject("select coalesce(max(id),0) from auditoria",Long.class);
        jdbc.execute("alter table auditoria add constraint fallo_auditoria check(id <= "+ultimo+")");
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void crearEditarDesactivarReactivarConAuditoriaYEstadoServidor(Catalogo c) {
        var x=nuevo(c,"  Registro  "); poner(x,"activo",false); var creado=guardar(c,x);
        assertEquals("Registro",dato(creado,"nombre")); assertEquals(true,dato(creado,"activo"));
        var cambio=nuevo(c,"Registro editado"); poner(cambio,"id",id(creado)); poner(cambio,"activo",false);
        var editado=guardar(c,cambio); assertEquals(true,dato(editado,"activo"));
        accion(c,"desactivar",id(creado)); assertEquals(false,dato(obtener(c,id(creado)),"activo"));
        accion(c,"reactivar",id(creado)); assertEquals(true,dato(obtener(c,id(creado)),"activo"));
        assertEquals(4,auditorias());
        assertEquals(4,jdbc.queryForObject("select count(*) from auditoria where usuario_id=?",Integer.class,usuarioId));
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void nombreObligatorioYLongitudBackend(Catalogo c) {
        for(String n: Arrays.asList(null,"","   ")) {
            var x=nuevo(c,"Prueba"); poner(x,"nombre",n);
            assertEquals("El nombre es obligatorio.",assertThrows(IllegalArgumentException.class,()->guardar(c,x)).getMessage());
        }
        var x=nuevo(c,"x".repeat(c.maximo+1)); assertThrows(IllegalArgumentException.class,()->guardar(c,x)); assertEquals(0,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void duplicadoNormalizadoRechazadoMensajeLiteral(Catalogo c) {
        guardar(c,nuevo(c,"Registro"));
        assertEquals("Ya existe un registro con el nombre registro en este catálogo.",
                assertThrows(IllegalArgumentException.class,()->guardar(c,nuevo(c," registro "))).getMessage());
        guardar(c,nuevo(c,"Otro")); assertEquals(2,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void altaDeInactivoReutilizaId(Catalogo c) {
        var x=guardar(c,nuevo(c,"Registro")); accion(c,"desactivar",id(x));
        var reactivado=guardar(c,nuevo(c,"Registro")); assertEquals(id(x),id(reactivado));
        assertEquals(true,dato(reactivado,"activo")); assertEquals(3,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void auditorNoManipulableYPermisosPorOperacion(Catalogo c) {
        var creado=guardar(c,nuevo(c,"Registro"));
        assertThrows(AccessDeniedException.class,()->guardar(c,nuevo(c,"Otro"),"intruso"));
        autenticar("admin","ROLE_ADMINISTRADOR","CATALOG_CREATE");
        var editado=nuevo(c,"Editado"); poner(editado,"id",id(creado));
        assertThrows(AccessDeniedException.class,()->guardar(c,editado));
        assertThrows(AccessDeniedException.class,()->accion(c,"desactivar",id(creado)));
        autenticar("admin","ROLE_ADMINISTRADOR","CATALOG_UPDATE");
        assertThrows(AccessDeniedException.class,()->guardar(c,nuevo(c,"Nuevo")));
        autenticar("admin","ROLE_MEDICO","CATALOG_VIEW","CATALOG_CREATE","CATALOG_UPDATE","CATALOG_DISABLE");
        assertThrows(AccessDeniedException.class,()->guardar(c,nuevo(c,"Nuevo")));
        assertThrows(AccessDeniedException.class,()->listar(c,"",0));
        assertEquals(1,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void falloAuditoriaReviertePersistencia(Catalogo c) {
        fallaAuditoria(); assertThrows(RuntimeException.class,()->guardar(c,nuevo(c,"Rollback")));
        assertEquals(0L,tx.execute(s->em.createQuery("select count(c) from "+c.entidad.getSimpleName()+" c where c.nombre='Rollback'",Long.class).getSingleResult()).longValue());
        assertEquals(0,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void dosCreacionesConcurrentesSoloPersistenUnActivo(Catalogo c) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var inicio=new CountDownLatch(1);
        Callable<String> tarea=()-> { admin(); inicio.await(); try { guardar(c,nuevo(c,"Concurrente")); return "OK"; }
            catch(IllegalArgumentException e) { return e.getMessage(); } finally { SecurityContextHolder.clearContext(); } };
        try { var a=pool.submit(tarea); var b=pool.submit(tarea); inicio.countDown();
            var resultados=List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(1,resultados.stream().filter("OK"::equals).count());
            assertEquals(1,resultados.stream().filter("Ya existe un registro con el nombre Concurrente en este catálogo."::equals).count());
            assertEquals(1,auditorias());
        } finally { pool.shutdownNow(); }
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void listadoFiltradoPaginadoYVacio(Catalogo c) {
        for(int i=0;i<12;i++) guardar(c,nuevo(c,"Filtro "+String.format("%02d",i)));
        assertEquals(10,((List<?>)listar(c,"Filtro",0).get(c.lista)).size());
        assertEquals(2,((List<?>)listar(c,"Filtro",1).get(c.lista)).size());
        var vacio=listar(c,"Inexistente",0); assertEquals(List.of(),vacio.get(c.lista));
        assertTrue(renderizar(c.ruta.substring(1)+"/lista",vacio).contains("No se encontraron resultados."));
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(value=Catalogo.class,names={"ESPECIALIDAD","MEDICAMENTO"})
    void descripcionObligatoriaConLimite(Catalogo c) {
        for(String descripcion: Arrays.asList(null," ","x".repeat(501))) {
            var x=nuevo(c,"Descripción"); poner(x,"descripcion",descripcion);
            assertThrows(IllegalArgumentException.class,()->guardar(c,x));
        }
    }
    @ParameterizedTest @ValueSource(strings={"1234567","123456789","1234ABCD","1234-678"," 12345678","12345678 ","        "})
    void telefonoInvalidoLiteral(String telefono) {
        var x=nuevo(Catalogo.SUCURSAL,"Sucursal"); poner(x,"telefono",telefono);
        assertEquals("El teléfono debe tener exactamente 8 dígitos.",assertThrows(IllegalArgumentException.class,()->guardar(Catalogo.SUCURSAL,x)).getMessage());
    }
    @Test void sucursalTelefonoOpcionalYDireccionDescripcionLimitadas() {
        for(String telefono: Arrays.asList(null,"","12345678")) {
            var x=nuevo(Catalogo.SUCURSAL,"Sede "+telefono); poner(x,"telefono",telefono); guardar(Catalogo.SUCURSAL,x);
        }
        var x=nuevo(Catalogo.SUCURSAL,"Inválida"); poner(x,"direccion","x".repeat(501));
        assertThrows(IllegalArgumentException.class,()->guardar(Catalogo.SUCURSAL,x));
        poner(x,"direccion",null); poner(x,"descripcion","x".repeat(251));
        assertThrows(IllegalArgumentException.class,()->guardar(Catalogo.SUCURSAL,x));
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(value=Catalogo.class,names={"MEDICAMENTO","EXAMEN"})
    void precioPositivoEscalaYCapacidadDiezDos(Catalogo c) {
        for(String valor: Arrays.asList(null,"0","-1","1.001","100000000.00")) {
            var x=nuevo(c,"Precio"); poner(x,"precio",valor==null?null:new java.math.BigDecimal(valor));
            var error=assertThrows(IllegalArgumentException.class,()->guardar(c,x));
            if(valor==null || valor.equals("0") || valor.equals("-1"))
                assertEquals(c==Catalogo.EXAMEN?"El precio base debe ser mayor a 0.":"El precio debe ser mayor a 0.",error.getMessage());
        }
        var x=nuevo(c,"Máximo"); poner(x,"precio",new java.math.BigDecimal("99999999.99")); guardar(c,x);
    }
    @Test void examenLaboratorioObligatorioExistenteActivoYRangoOpcional() {
        var servicio=contexto.getBean(CatalogoExamenService.class);
        var x=(CatalogoExamen)nuevo(Catalogo.EXAMEN,"Examen");
        assertEquals("Debe seleccionar un laboratorio.",assertThrows(IllegalArgumentException.class,()->servicio.guardar(x,null,"admin")).getMessage());
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(x,-1L,"admin"));
        accion(Catalogo.LABORATORIO,"desactivar",laboratorioId);
        assertTrue(servicio.listarLaboratoriosActivos().isEmpty());
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(x,laboratorioId,"admin"));
        accion(Catalogo.LABORATORIO,"reactivar",laboratorioId);
        var guardado=servicio.guardar(x,laboratorioId,"admin"); assertNull(guardado.getUnidadReferencia()); assertNull(guardado.getRangoReferencia());
        guardado.setRangoReferencia("4 - 10"); guardado.setUnidadReferencia("mg"); guardar(Catalogo.EXAMEN,guardado);
        accion(Catalogo.EXAMEN,"desactivar",guardado.getId());
        assertTrue(contexto.getBean(CatalogoExamenRepository.class).findByActivoTrueOrderByNombreAsc().isEmpty());
        assertEquals("4 - 10",((CatalogoExamen)obtener(Catalogo.EXAMEN,guardado.getId())).getRangoReferencia());
    }
    @Test void medicamentoUnidadMinimoYControladoSeConservan() {
        var x=(Medicamento)nuevo(Catalogo.MEDICAMENTO,"Controlado");
        for(String unidad: Arrays.asList(null," ","x".repeat(51))) { x.setUnidad(unidad); assertThrows(IllegalArgumentException.class,()->guardar(Catalogo.MEDICAMENTO,x)); }
        x.setUnidad("Tableta"); x.setStockMinimo(-1); assertThrows(IllegalArgumentException.class,()->guardar(Catalogo.MEDICAMENTO,x));
        x.setStockMinimo(5); x.setEsControlado(true); var creado=(Medicamento)guardar(Catalogo.MEDICAMENTO,x);
        assertTrue(creado.getEsControlado()); assertEquals(5,creado.getStockMinimo());
        accion(Catalogo.MEDICAMENTO,"desactivar",creado.getId());
        assertTrue(contexto.getBean(MedicamentoService.class).listarActivos().stream().noneMatch(m->m.getId().equals(creado.getId())));
        assertTrue(((Medicamento)obtener(Catalogo.MEDICAMENTO,creado.getId())).getEsControlado());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(value=Catalogo.class,names={"ROL","ESTADO"})
    void codigoTecnicoNoSeModificaAlEditar(Catalogo c) {
        var original=guardar(c,nuevo(c,"Original")); var cambio=nuevo(c,"Editado"); poner(cambio,"id",id(original)); poner(cambio,"codigo","MANIPULADO");
        assertEquals(dato(original,"codigo"),dato(guardar(c,cambio),"codigo"));
    }
    @Test void permisosRolConservadosYRolEstructuralNoSeDesactiva() {
        var creado=(Rol)guardar(Catalogo.ROL,nuevo(Catalogo.ROL,"Auxiliar"));
        tx.executeWithoutResult(s->{
            Permiso p=new Permiso(); p.setCodigo("CATALOG_VIEW"); p.setDescripcion("Ver catálogo"); em.persist(p);
            em.find(Rol.class,creado.getId()).getPermisos().add(p);
        });
        var cambio=(Rol)nuevo(Catalogo.ROL,"Auxiliar editado"); cambio.setId(creado.getId()); guardar(Catalogo.ROL,cambio);
        assertEquals(1,jdbc.queryForObject("select count(*) from rol_permiso where rol_id=?",Integer.class,creado.getId()));
        Long adminId=jdbc.queryForObject("select id from rol where codigo='ADMINISTRADOR'",Long.class);
        assertThrows(IllegalArgumentException.class,()->accion(Catalogo.ROL,"desactivar",adminId));
        tx.executeWithoutResult(s->{ var u=new Usuario(); u.setNombreUsuario("auxiliar"); u.setPasswordHash("prueba"); u.setRol(em.find(Rol.class,creado.getId())); em.persist(u); });
        assertThrows(IllegalArgumentException.class,()->accion(Catalogo.ROL,"desactivar",creado.getId()));
    }
    @Test void especialidadSucursalYCambioDeRelacionSeReflejanEnCu03DespuesDeCommit() {
        var esp=(Especialidad)guardar(Catalogo.ESPECIALIDAD,nuevo(Catalogo.ESPECIALIDAD,"Consulta"));
        var relaciones=contexto.getBean(SucursalEspecialidadService.class); var catalogos=contexto.getBean(CitaCatalogoService.class);
        var relacion=relaciones.asignar(sucursalId,esp.getId(),"admin");
        assertEquals(1,catalogos.listarEspecialidades(sucursalId).size()); catalogos.listarSucursales();
        caches.getCache("ajena").put("clave","conservar");
        tx.executeWithoutResult(s->{
            accion(Catalogo.ESPECIALIDAD,"desactivar",esp.getId());
            assertNotNull(caches.getCache("citaEspecialidadesPorSucursal").get(sucursalId));
        });
        assertTrue(catalogos.listarEspecialidades(sucursalId).isEmpty());
        accion(Catalogo.ESPECIALIDAD,"reactivar",esp.getId()); assertEquals(1,catalogos.listarEspecialidades(sucursalId).size());
        accion(Catalogo.SUCURSAL,"desactivar",sucursalId); assertTrue(catalogos.listarSucursales().isEmpty());
        assertTrue(catalogos.listarEspecialidades(sucursalId).isEmpty());
        accion(Catalogo.SUCURSAL,"reactivar",sucursalId); assertEquals(1,catalogos.listarEspecialidades(sucursalId).size());
        relaciones.desactivar(relacion.getId(),"admin"); assertTrue(catalogos.listarEspecialidades(sucursalId).isEmpty());
        assertEquals(relacion.getId(),relaciones.asignar(sucursalId,esp.getId(),"admin").getId());
        assertEquals(1,catalogos.listarEspecialidades(sucursalId).size()); assertEquals("conservar",caches.getCache("ajena").get("clave").get());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(value=Catalogo.class,names={"ESPECIALIDAD","SUCURSAL","ESTADO","ROL"})
    void cacheSeInvalidaTrasCommitYSeConservaEnRollback(Catalogo c) {
        String cache=switch(c) { case ESPECIALIDAD->"especialidadesActivas";case SUCURSAL->"sucursalesActivas";case ESTADO->"estadosCitaActivos";default->"rolesActivos"; };
        caches.getCache(cache).put("prueba","anterior");
        tx.executeWithoutResult(s->{guardar(c,nuevo(c,"Creado")); assertNotNull(caches.getCache(cache).get("prueba"));});
        assertNull(caches.getCache(cache).get("prueba")); caches.getCache(cache).put("prueba","anterior"); fallaAuditoria();
        assertThrows(RuntimeException.class,()->guardar(c,nuevo(c,"Rollback")));
        assertNotNull(caches.getCache(cache).get("prueba"));
    }
    InventarioMedicamentoService inventarios() { return contexto.getBean(InventarioMedicamentoService.class); }
    @Test void inventarioInicialYAjusteDeReactivacionGeneranBitacoraConservandoHistorial() {
        var i=inventarios().guardar(medicamentoId,sucursalId,7,"admin");
        assertEquals(7,inventarios().buscarPorId(i.getId()).getStockActual());
        assertEquals(1,jdbc.queryForObject("select count(*) from movimiento_inventario_medicamento",Integer.class));
        inventarios().desactivar(i.getId(),"admin"); var r=inventarios().guardar(medicamentoId,sucursalId,4,"admin"); assertEquals(i.getId(),r.getId());
        assertEquals(4,inventarios().buscarPorId(i.getId()).getStockActual());
        assertEquals(2,jdbc.queryForObject("select count(*) from movimiento_inventario_medicamento",Integer.class));
        assertEquals(7,jdbc.queryForObject("select stock_anterior from movimiento_inventario_medicamento where tipo_movimiento='AJUSTE_NEGATIVO'",Integer.class));
        assertThrows(IllegalArgumentException.class,()->inventarios().guardar(medicamentoId,sucursalId,1,"admin"));
        assertEquals(1,jdbc.queryForObject("select count(*) from inventario_medicamento",Integer.class));
        accion(Catalogo.MEDICAMENTO,"desactivar",medicamentoId);
        assertEquals(medicamentoId,tx.execute(s->em.find(InventarioMedicamento.class,i.getId()).getMedicamento().getId()));
    }
    @Test void inventarioValidaFksActivasYStockNuncaNegativo() {
        for(Integer stock: Arrays.asList(null,-1)) assertThrows(IllegalArgumentException.class,()->inventarios().guardar(medicamentoId,sucursalId,stock,"admin"));
        assertThrows(IllegalArgumentException.class,()->inventarios().guardar(-1L,sucursalId,1,"admin"));
        assertThrows(IllegalArgumentException.class,()->inventarios().guardar(medicamentoId,-1L,1,"admin"));
        accion(Catalogo.MEDICAMENTO,"desactivar",medicamentoId);
        assertThrows(IllegalArgumentException.class,()->inventarios().guardar(medicamentoId,sucursalId,1,"admin"));
        accion(Catalogo.MEDICAMENTO,"reactivar",medicamentoId); accion(Catalogo.SUCURSAL,"desactivar",sucursalId);
        assertThrows(IllegalArgumentException.class,()->inventarios().guardar(medicamentoId,sucursalId,1,"admin"));
        assertEquals(0,jdbc.queryForObject("select count(*) from inventario_medicamento",Integer.class));
    }
    @Test void inventarioRollbackRevierteExistenciaBitacoraYRegistro() {
        fallaAuditoria(); assertThrows(RuntimeException.class,()->inventarios().guardar(medicamentoId,sucursalId,8,"admin"));
        assertEquals(0,jdbc.queryForObject("select count(*) from inventario_medicamento",Integer.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from movimiento_inventario_medicamento",Integer.class));
        assertEquals(0,auditorias());
    }
    @Test void inventarioStockBajoSeMuestraYMovimientosSiguenConsultables() {
        tx.executeWithoutResult(s->em.find(Medicamento.class,medicamentoId).setStockMinimo(5));
        inventarios().guardar(medicamentoId,sucursalId,3,"admin");
        tx.executeWithoutResult(s->{ var modelo=new ExtendedModelMap(); contexto.getBean(InventarioMedicamentoController.class).listar("",0,modelo);
            assertTrue(renderizar("admin/inventario-medicamentos/lista",modelo).contains("Stock bajo")); });
        assertEquals(1,contexto.getBean(MovimientoInventarioService.class).listarTodos().size());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void formularioMvcProtegeCamposYMensajesLiterales(Catalogo c) throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(contexto.getBean(c.controller)).build();
        var request=post(c.ruta+"/guardar").principal(()->"admin").param("nombre","Registro MVC").param("descripcion","Descripción MVC")
                .param("codigo","MVC").param("precio","12.50").param("unidad","Tableta").param("laboratorioId",laboratorioId.toString())
                .param("activo","false").param("fechaCreacion","2000-01-01T00:00:00").param("permisos[0].codigo","CATALOG_DISABLE")
                .param("nombreUsuario","intruso").param("laboratorio.id","-1");
        mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(c.ruta))
                .andExpect(flash().attribute("mensaje","El registro Registro MVC ha sido creado exitosamente."));
        Object creado=tx.execute(s->em.createQuery("select c from "+c.entidad.getSimpleName()+" c where c.nombre='Registro MVC'",c.entidad).getSingleResult());
        assertEquals(true,dato(creado,"activo"));
        var resultado=mvc.perform(get(c.ruta+"/editar/"+id(creado))).andExpect(status().isOk()).andReturn();
        assertEquals("Registro MVC",dato(resultado.getModelAndView().getModel().get(c==Catalogo.ESTADO?"estado":c.atributo),"nombre"));
        mvc.perform(post(c.ruta+"/desactivar/"+id(creado)).principal(()->"admin"))
                .andExpect(flash().attribute("mensaje","El registro Registro MVC ha sido eliminado correctamente."));
        assertEquals(false,dato(obtener(c,id(creado)),"activo"));
        assertEquals(2,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void cancelarNavegacionYConfirmacionEnVista(Catalogo c) throws Exception {
        String formulario=Files.readString(Path.of("src/main/resources/templates"+c.ruta+"/formulario.html"));
        assertTrue(formulario.contains("Cancelar")); assertTrue(formulario.contains("@{"+c.ruta+"}"));
        guardar(c,nuevo(c,"Registro"));
        tx.executeWithoutResult(s->{String html=renderizar(c.ruta.substring(1)+"/lista",listar(c,"",0));
            assertTrue(html.contains("¿Está seguro que desea eliminar el registro ")); assertTrue(html.contains("Esta acción no se puede deshacer."));
            assertTrue(html.contains("href=\"/admin/catalogos\"")); assertFalse(html.contains("href=\"/\"")); });
        assertEquals(1,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void edicionMvcMensajeLiteralYValidacionCorregible(Catalogo c) throws Exception {
        var creado=guardar(c,nuevo(c,"Registro"));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(contexto.getBean(c.controller)).build();
        var request=post(c.ruta+"/guardar").principal(()->"admin").param("id",id(creado).toString()).param("nombre","Editado")
                .param("descripcion","Descripción MVC").param("codigo",String.valueOf(new org.springframework.beans.BeanWrapperImpl(creado).isReadableProperty("codigo")?dato(creado,"codigo"):"MVC"))
                .param("precio","12.50").param("unidad","Tableta").param("laboratorioId",laboratorioId.toString());
        mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("mensaje","El registro Editado ha sido actualizado correctamente."));
        mvc.perform(post(c.ruta+"/guardar").principal(()->"admin").param("nombre"," ").param("codigo","MVC"))
                .andExpect(status().isOk()).andExpect(view().name(c.ruta.substring(1)+"/formulario"))
                .andExpect(model().attribute("error","El nombre es obligatorio."));
        assertEquals(2,auditorias());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Catalogo.class)
    void creacionYReactivacionConcurrentesNoDuplicanHistoricos(Catalogo c) throws Exception {
        var original=guardar(c,nuevo(c,"Concurrente")); accion(c,"desactivar",id(original));
        var pool=Executors.newFixedThreadPool(2); var inicio=new CountDownLatch(1);
        Callable<Boolean> tarea=()->{ admin(); inicio.await(); try { guardar(c,nuevo(c,"Concurrente")); return true; }
            catch(IllegalArgumentException e) { return false; } finally { SecurityContextHolder.clearContext(); } };
        try { var a=pool.submit(tarea); var b=pool.submit(tarea); inicio.countDown();
            assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(true,dato(obtener(c,id(original)),"activo")); assertEquals(3,auditorias());
        } finally {pool.shutdownNow();}
    }
    @Test void inventarioConcurrenteNoDuplicaCombinacionNiMovimientos() throws Exception {
        var pool=Executors.newFixedThreadPool(2); var inicio=new CountDownLatch(1);
        Callable<Boolean> tarea=()->{admin();inicio.await();try {inventarios().guardar(medicamentoId,sucursalId,10,"admin");return true;}
            catch(IllegalArgumentException e){return false;}finally{SecurityContextHolder.clearContext();}};
        try {var a=pool.submit(tarea);var b=pool.submit(tarea);inicio.countDown();assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));}
        finally{pool.shutdownNow();}
        assertEquals(1,jdbc.queryForObject("select count(*) from inventario_medicamento",Integer.class));
        assertEquals(1,jdbc.queryForObject("select count(*) from movimiento_inventario_medicamento",Integer.class));
    }
    com.example.proyecto_semestre_medico.dto.MovimientoInventarioDTO ajuste(Long inventarioId,int cantidad) {
        var dto=new com.example.proyecto_semestre_medico.dto.MovimientoInventarioDTO(); dto.setInventarioId(inventarioId);
        dto.setCantidad(cantidad);dto.setTipoMovimiento(TipoMovimientoInventario.AJUSTE_NEGATIVO);dto.setMotivo("Ajuste de prueba de concurrencia");return dto;
    }
    @Test void dosMovimientosSobreMismaExistenciaNoPierdenActualizaciones() throws Exception {
        var inv=inventarios().guardar(medicamentoId,sucursalId,10,"admin");var pool=Executors.newFixedThreadPool(2);var leidos=new CyclicBarrier(2);
        Callable<Boolean> tarea=()->{admin();try {
            tx.executeWithoutResult(s->{em.find(InventarioMedicamento.class,inv.getId());try{leidos.await(10,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException(e);}
                contexto.getBean(MovimientoInventarioService.class).registrarMovimiento(ajuste(inv.getId(),3),"admin");}); return true;
        } catch(org.springframework.dao.OptimisticLockingFailureException e){return false;} finally{SecurityContextHolder.clearContext();}};
        try {var a=pool.submit(tarea);var b=pool.submit(tarea);assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));}
        finally{pool.shutdownNow();}
        assertEquals(7,inventarios().buscarPorId(inv.getId()).getStockActual());
        assertEquals(2,jdbc.queryForObject("select count(*) from movimiento_inventario_medicamento",Integer.class));
    }
    @Test void movimientoRechazaStockNegativoOverflowYAuditorAjeno() {
        var inv=inventarios().guardar(medicamentoId,sucursalId,1,"admin");var movimientos=contexto.getBean(MovimientoInventarioService.class);
        assertThrows(IllegalArgumentException.class,()->movimientos.registrarMovimiento(ajuste(inv.getId(),2),"admin"));
        assertThrows(AccessDeniedException.class,()->movimientos.registrarMovimiento(ajuste(inv.getId(),1),"otro"));
        var enorme=ajuste(inv.getId(),Integer.MAX_VALUE);enorme.setTipoMovimiento(TipoMovimientoInventario.AJUSTE_POSITIVO);
        assertThrows(IllegalArgumentException.class,()->movimientos.registrarMovimiento(enorme,"admin"));
        assertEquals(1,inventarios().buscarPorId(inv.getId()).getStockActual());
    }
    @Test void administradorSinPermisoDeActualizarNoModificaStockPorRutaOperativa() {
        var inv=inventarios().guardar(medicamentoId,sucursalId,10,"admin");
        autenticar("admin","ROLE_ADMINISTRADOR","CATALOG_VIEW");
        assertThrows(AccessDeniedException.class,()->contexto.getBean(MovimientoInventarioController.class)
                .guardar(ajuste(inv.getId(),1),new org.springframework.validation.BeanPropertyBindingResult(ajuste(inv.getId(),1),"movimiento"),()->"admin",new ExtendedModelMap(),new RedirectAttributesModelMap()));
        assertEquals(10,inventarios().buscarPorId(inv.getId()).getStockActual());
    }
    @Test void catalogosCentralizadosConPermisosYTodosLosAccesos() {
        assertEquals("admin/catalogos",contexto.getBean(CatalogosController.class).catalogos());
        String html=renderizar("admin/catalogos",Map.of());
        for(Catalogo c:Catalogo.values())assertTrue(html.contains("href=\""+c.ruta+"\""));
        assertTrue(html.contains("/admin/inventario-medicamentos"));assertTrue(html.contains("/admin/sucursal-especialidad"));
        autenticar("admin","ROLE_ADMINISTRADOR");assertThrows(AccessDeniedException.class,()->contexto.getBean(CatalogosController.class).catalogos());
        autenticar("admin","ROLE_MEDICO","CATALOG_VIEW");assertThrows(AccessDeniedException.class,()->contexto.getBean(CatalogosController.class).catalogos());
    }
    @Test void sucursalInactivaNoOfreceMedicosPorIdManipulado() {
        var esp=(Especialidad)guardar(Catalogo.ESPECIALIDAD,nuevo(Catalogo.ESPECIALIDAD,"General"));
        contexto.getBean(SucursalEspecialidadService.class).asignar(sucursalId,esp.getId(),"admin");
        accion(Catalogo.SUCURSAL,"desactivar",sucursalId);
        assertThrows(IllegalArgumentException.class,()->contexto.getBean(CitaCatalogoService.class).listarMedicos(sucursalId,esp.getId()));
        verifyNoInteractions(contexto.getBean(MedicoRepository.class));
    }
    @Test void rutasAntiguasDeCatalogosNoEvitanPermisos() {
        var controller=contexto.getBean(PruebaController.class);
        assertFalse(controller.listarRoles().isEmpty()); controller.listarPermisos();
        autenticar("admin","ROLE_MEDICO","CATALOG_VIEW");
        assertThrows(AccessDeniedException.class,controller::listarRoles);
        assertThrows(AccessDeniedException.class,controller::listarPermisos);
        autenticar("admin","ROLE_ADMINISTRADOR");
        assertThrows(AccessDeniedException.class,controller::listarRoles);
        assertThrows(AccessDeniedException.class,controller::listarPermisos);
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.security.core.AuthenticationException.class,controller::listarRoles);
        assertThrows(org.springframework.security.core.AuthenticationException.class,controller::listarPermisos);
    }
    String renderizar(String vista, Map<String, Object> variables) {
        var servlet = new org.springframework.mock.web.MockServletContext();
        var request = new org.springframework.mock.web.MockHttpServletRequest(servlet); request.setRequestURI("/admin/catalogos");
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
