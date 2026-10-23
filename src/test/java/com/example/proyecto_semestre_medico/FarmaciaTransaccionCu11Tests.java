package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.dto.*;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// Configuración exclusiva de test: no carga application.properties ni usa el HIS.
class FarmaciaTransaccionCu11Tests {
    AnnotationConfigApplicationContext contexto;
    DespachoMedicamentoService servicio;
    RecetaService recetasServicio;
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    EntityManager em;
    Long recetaId, detalleId, sucursalId, originalId, alternativoId, consultaId;

    @Configuration @EnableTransactionManagement @EnableMethodSecurity
    static class Configuracion {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:cu11_" + UUID.randomUUID()
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
        @Bean JpaRepositoryFactoryBean<RecetaRepository, Receta, Long> recetas(EntityManagerFactory f) { return repo(RecetaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<RecetaDetalleRepository, RecetaDetalle, Long> detalles(EntityManagerFactory f) { return repo(RecetaDetalleRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<DespachoMedicamentoRepository, DespachoMedicamento, Long> despachos(EntityManagerFactory f) { return repo(DespachoMedicamentoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<InventarioMedicamentoRepository, InventarioMedicamento, Long> inventarios(EntityManagerFactory f) { return repo(InventarioMedicamentoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<MedicamentoRepository, Medicamento, Long> medicamentos(EntityManagerFactory f) { return repo(MedicamentoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<SucursalRepository, Sucursal, Long> sucursales(EntityManagerFactory f) { return repo(SucursalRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<UsuarioRepository, Usuario, Long> usuarios(EntityManagerFactory f) { return repo(UsuarioRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<AuditoriaRepository, Auditoria, Long> auditorias(EntityManagerFactory f) { return repo(AuditoriaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<NotificacionInternaRepository, NotificacionInterna, Long> notificaciones(EntityManagerFactory f) { return repo(NotificacionInternaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<MovimientoInventarioMedicamentoRepository, MovimientoInventarioMedicamento, Long> movimientos(EntityManagerFactory f) { return repo(MovimientoInventarioMedicamentoRepository.class, f); }
        @Bean MovimientoInventarioService movimientoServicio(MovimientoInventarioMedicamentoRepository m,
                InventarioMedicamentoRepository i, UsuarioRepository u, AuditoriaRepository a) {
            return new MovimientoInventarioServiceImpl(m, i, u, a);
        }
        @Bean DespachoMedicamentoService servicio(DespachoMedicamentoRepository d, RecetaRepository r,
                RecetaDetalleRepository rd, SucursalRepository s, MedicamentoRepository m,
                InventarioMedicamentoRepository i, UsuarioRepository u, AuditoriaRepository a,
                MovimientoInventarioService ms, NotificacionInternaRepository n) {
            return new DespachoMedicamentoServiceImpl(d, r, rd, s, m, i, u, a, ms, n);
        }
        @Bean RecetaService recetaServicio(RecetaRepository r, MedicamentoRepository m, UsuarioRepository u, AuditoriaRepository a) {
            return new RecetaServiceImpl(r, mock(ConsultaMedicaRepository.class), m, u, a, mock(MedicoRepository.class));
        }
    }

    static void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "farmacia", "irrelevante", List.of(new SimpleGrantedAuthority("ROLE_FARMACEUTICO"))));
    }

    @BeforeEach void preparar() {
        contexto = new AnnotationConfigApplicationContext(Configuracion.class);
        servicio = contexto.getBean(DespachoMedicamentoService.class); recetasServicio = contexto.getBean(RecetaService.class);
        jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
        tx = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        em = SharedEntityManagerCreator.createSharedEntityManager(contexto.getBean(EntityManagerFactory.class));
        tx.executeWithoutResult(estado -> {
            Rol rol = new Rol(); rol.setCodigo("FARMACEUTICO"); rol.setNombre("Farmacia"); em.persist(rol);
            Usuario farmacia = new Usuario(); farmacia.setRol(rol); farmacia.setNombreUsuario("farmacia");
            farmacia.setPasswordHash("hash-de-prueba"); em.persist(farmacia);
            Rol rolMedico = new Rol(); rolMedico.setCodigo("MEDICO"); rolMedico.setNombre("Médico"); em.persist(rolMedico);
            Usuario doctor = new Usuario(); doctor.setRol(rolMedico); doctor.setNombreUsuario("doctor");
            doctor.setPasswordHash("hash-de-prueba"); em.persist(doctor);
            Sucursal sucursal = new Sucursal(); sucursal.setNombre("Sucursal de prueba"); em.persist(sucursal); sucursalId = sucursal.getId();
            Personal personal = new Personal(); personal.setUsuario(doctor); personal.setSucursal(sucursal);
            personal.setNombreCompleto("Médico Demo"); em.persist(personal);
            Especialidad especialidad = new Especialidad(); especialidad.setNombre("General"); especialidad.setDescripcion("CU-11"); em.persist(especialidad);
            Medico medico = new Medico(); medico.setPersonal(personal); medico.setEspecialidad(especialidad); em.persist(medico);
            Paciente paciente = new Paciente(); paciente.setNombreCompleto("Ana Paciente");
            paciente.setDpiCifrado("solo-prueba"); paciente.setDpiHash("b".repeat(64)); em.persist(paciente);
            EstadoCita ec = new EstadoCita(); ec.setCodigo("PENDIENTE_CIERRE"); ec.setNombre("Pendiente de cierre"); em.persist(ec);
            Cita cita = new Cita(); cita.setNumeroCita("CITA-TEST"); cita.setPaciente(paciente); cita.setMedico(medico);
            cita.setSucursal(sucursal); cita.setEstadoCita(ec); cita.setMotivoConsulta("Prueba CU-11");
            cita.setFechaHoraInicio(LocalDateTime.now()); cita.setFechaHoraFin(LocalDateTime.now().plusMinutes(30)); em.persist(cita);
            ConsultaMedica consulta = new ConsultaMedica(); consulta.setCita(cita); consulta.setMedico(medico);
            consulta.setEstado("FINALIZADA"); consulta.setMotivoVisita("Prueba CU-11"); em.persist(consulta); consultaId = consulta.getId();
            Medicamento original = medicamento("Original", "10.00"); em.persist(original); originalId = original.getId();
            Medicamento alternativo = medicamento("Alternativo", "7.50"); em.persist(alternativo); alternativoId = alternativo.getId();
            for (Medicamento med : List.of(original, alternativo)) {
                InventarioMedicamento inv = new InventarioMedicamento(); inv.setSucursal(sucursal); inv.setMedicamento(med);
                inv.setStockActual(med == original ? 5 : 8); em.persist(inv);
            }
            Receta receta = new Receta(); receta.setConsultaMedica(consulta);
            receta.setFechaEmision(LocalDateTime.now().minusDays(2)); receta.setFechaVencimiento(LocalDateTime.now().plusDays(5));
            RecetaDetalle detalle = new RecetaDetalle(); detalle.setReceta(receta); detalle.setMedicamento(original);
            detalle.setCantidad(5); detalle.setDosis("Una tableta cada ocho horas"); receta.getDetalles().add(detalle);
            em.persist(receta); em.flush(); recetaId = receta.getId(); detalleId = detalle.getId();
        });
        autenticar();
    }

    Medicamento medicamento(String nombre, String precio) {
        Medicamento med = new Medicamento(); med.setNombre(nombre); med.setDescripcion("Medicamento de prueba");
        med.setPrecio(new BigDecimal(precio)); med.setUnidad("Tabletas"); med.setStockMinimo(2); return med;
    }

    @AfterEach void cerrar() { SecurityContextHolder.clearContext(); if (contexto != null) contexto.close(); }

    DespachoMedicamentoDTO formulario() {
        var dto = new DespachoMedicamentoDTO(); dto.setRecetaId(recetaId); dto.setSucursalId(sucursalId);
        var item = new DespachoDetalleDTO(); item.setRecetaDetalleId(detalleId); item.setMedicamentoDespachadoId(originalId);
        item.setCantidad(2); item.setCantidadDespachadaPrevia(0); dto.setDetalles(List.of(item)); return dto;
    }
    int contar(String tabla) { return jdbc.queryForObject("select count(*) from " + tabla, Integer.class); }
    int stock(Long med) { return jdbc.queryForObject("select stock_actual from inventario_medicamento where medicamento_id=? and sucursal_id=?", Integer.class, med, sucursalId); }
    String estado() { return jdbc.queryForObject("select estado from receta where id=?", String.class, recetaId); }

    @Test void persisteDespachoDetallesMovimientoYRelacionConPacienteSinPago() {
        servicio.registrarDespacho(formulario(), "farmacia");
        assertEquals(3, stock(originalId)); assertEquals("PARCIAL", estado());
        assertEquals(1, contar("despacho_medicamento")); assertEquals(1, contar("despacho_medicamento_detalle"));
        assertEquals(1, contar("movimiento_inventario_medicamento")); assertEquals(0, contar("pago")); assertEquals(0, contar("pago_laboratorio"));
        var movimiento = contexto.getBean(MovimientoInventarioService.class).listarTodos().get(0);
        assertEquals(TipoMovimientoInventario.DESPACHO, movimiento.getTipoMovimiento());
        assertEquals(5, movimiento.getStockAnterior()); assertEquals(3, movimiento.getStockNuevo());
        assertEquals(2, movimiento.getCantidad()); assertEquals("farmacia", movimiento.getUsuario().getNombreUsuario());
        assertEquals("RECETA-" + recetaId, movimiento.getReferencia()); assertNotNull(movimiento.getFechaMovimiento());
        var historia = servicio.listarDespachosPorReceta(recetaId);
        assertEquals("Original", historia.get(0).getDetalles().get(0).getMedicamentoDespachado().getNombre());
        assertEquals(new BigDecimal("20.00"), historia.get(0).getMontoTotal());
        assertEquals("Ana Paciente", jdbc.queryForObject("select p.nombre_completo from despacho_medicamento d join receta r on r.id=d.receta_id join consulta_medica cm on cm.id=r.consulta_medica_id join cita c on c.id=cm.cita_id join paciente p on p.id=c.paciente_id", String.class));
    }

    @Test void errorAlGuardarDespachoRevierteStockRecetaDetallesYMovimientos() {
        jdbc.execute("alter table despacho_medicamento add constraint fallo_despacho check (monto_total <> 20.00)");
        assertThrows(RuntimeException.class, () -> servicio.registrarDespacho(formulario(), "farmacia"));
        assertEquals(5, stock(originalId)); assertEquals("PENDIENTE", estado());
        assertEquals(0, contar("despacho_medicamento")); assertEquals(0, contar("movimiento_inventario_medicamento"));
        assertEquals(0, contar("auditoria"));
        assertEquals(0, jdbc.queryForObject("select cantidad_despachada from receta_detalle where id=?", Integer.class, detalleId));
    }

    @Test void errorEnSegundoItemNoDejaPrimerItemDescontado() {
        Long segundaLinea = tx.execute(s -> {
            Receta receta = em.find(Receta.class, recetaId);
            RecetaDetalle detalle = new RecetaDetalle(); detalle.setReceta(receta); detalle.setMedicamento(em.find(Medicamento.class, alternativoId));
            detalle.setDosis("Una tableta diaria"); detalle.setCantidad(10); em.persist(detalle); return detalle.getId();
        });
        var form = formulario(); var segundo = new DespachoDetalleDTO(); segundo.setRecetaDetalleId(segundaLinea);
        segundo.setCantidad(9); segundo.setCantidadDespachadaPrevia(0); segundo.setMedicamentoDespachadoId(alternativoId);
        form.setDetalles(List.of(form.getDetalles().get(0), segundo));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarDespacho(form, "farmacia"));
        assertEquals(5, stock(originalId)); assertEquals(8, stock(alternativoId)); assertEquals("PENDIENTE", estado());
        assertEquals(0, contar("despacho_medicamento")); assertEquals(0, contar("movimiento_inventario_medicamento"));
    }

    @Test void formularioParcialSimultaneoSoloSeRegistraUnaVez() throws Exception {
        var ejecutor = Executors.newFixedThreadPool(2); var listos = new CountDownLatch(2); var inicio = new CountDownLatch(1);
        Callable<Boolean> despachar = () -> {
            autenticar(); listos.countDown();
            try {
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No inició la concurrencia");
                servicio.registrarDespacho(formulario(), "farmacia"); return true;
            } catch (IllegalArgumentException actualizado) { return false; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var primero = ejecutor.submit(despachar); var segundo = ejecutor.submit(despachar);
            assertTrue(listos.await(10, TimeUnit.SECONDS)); inicio.countDown();
            assertEquals(1, (primero.get(20, TimeUnit.SECONDS) ? 1 : 0) + (segundo.get(20, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(1, contar("despacho_medicamento")); assertEquals(3, stock(originalId));
        } finally { inicio.countDown(); ejecutor.shutdownNow(); }
    }

    @Test void noAdquisicionConservaTrazabilidadSinDescontar() {
        recetasServicio.marcarComoNoAdquirida(recetaId, "farmacia");
        assertEquals("NO_ADQUIRIDA", estado()); assertEquals(5, stock(originalId));
        assertEquals(0, contar("despacho_medicamento")); assertEquals(0, contar("movimiento_inventario_medicamento"));
        assertEquals(1, contar("auditoria"));
    }

    @Test void busquedaFiltraIdConsultaYActivoEnBaseDeDatos() {
        assertEquals(1, recetasServicio.buscarActivasFarmacia(recetaId, null).size());
        assertEquals(1, recetasServicio.buscarActivasFarmacia(null, consultaId).size());
        assertTrue(recetasServicio.buscarActivasFarmacia(recetaId + 100, null).isEmpty());
        tx.executeWithoutResult(s -> em.find(Receta.class, recetaId).setActivo(false));
        assertTrue(recetasServicio.buscarActivasFarmacia(recetaId, null).isEmpty());
    }

    @Test void sustitucionPersisteNotificacionYTrazabilidad() {
        var form = formulario(); var item = form.getDetalles().get(0);
        item.setSustituir(true); item.setMedicamentoDespachadoId(alternativoId); item.setMotivoSustitucion("Presentación disponible");
        servicio.registrarDespacho(form, "farmacia");
        assertEquals(5, stock(originalId)); assertEquals(6, stock(alternativoId)); assertEquals(1, contar("notificacion_interna"));
        assertEquals(alternativoId, jdbc.queryForObject("select medicamento_despachado_id from despacho_medicamento_detalle", Long.class));
        assertEquals(originalId, jdbc.queryForObject("select medicamento_id from receta_detalle where id=?", Long.class, detalleId));
        assertEquals(new BigDecimal("15.00"), jdbc.queryForObject("select monto_total from despacho_medicamento", BigDecimal.class));
    }
}
