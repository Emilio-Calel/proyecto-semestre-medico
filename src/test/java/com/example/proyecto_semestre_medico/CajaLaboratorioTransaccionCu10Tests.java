package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.dto.PagoLaboratorioFormDTO;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.PagoLaboratorioServiceImpl;
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

// BD efímera propia: entidades, repositorios JPA, bloqueo y transacciones reales.
// No se carga application.properties ni se inicia Spring Boot.
class CajaLaboratorioTransaccionCu10Tests {
    AnnotationConfigApplicationContext contexto;
    PagoLaboratorioService servicio;
    JdbcTemplate jdbc;
    Long ordenId;

    @Configuration @EnableTransactionManagement @EnableMethodSecurity
    static class Configuracion {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:cu10_" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource fuente) {
            var fabrica = new LocalContainerEntityManagerFactoryBean(); fabrica.setDataSource(fuente);
            fabrica.setPackagesToScan("com.example.proyecto_semestre_medico.model");
            fabrica.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            fabrica.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return fabrica;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory fabrica) {
            return new JpaTransactionManager(fabrica);
        }
        static <T, R extends JpaRepository<T, Long>> JpaRepositoryFactoryBean<R, T, Long> repositorio(
                Class<R> tipo, EntityManagerFactory fabrica) {
            var repositorio = new JpaRepositoryFactoryBean<R, T, Long>(tipo);
            repositorio.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(fabrica));
            return repositorio;
        }
        @Bean JpaRepositoryFactoryBean<OrdenLaboratorioRepository, OrdenLaboratorio, Long> ordenes(EntityManagerFactory f) {
            return repositorio(OrdenLaboratorioRepository.class, f);
        }
        @Bean JpaRepositoryFactoryBean<PagoLaboratorioRepository, PagoLaboratorio, Long> pagos(EntityManagerFactory f) {
            return repositorio(PagoLaboratorioRepository.class, f);
        }
        @Bean JpaRepositoryFactoryBean<UsuarioRepository, Usuario, Long> usuarios(EntityManagerFactory f) {
            return repositorio(UsuarioRepository.class, f);
        }
        @Bean JpaRepositoryFactoryBean<AuditoriaRepository, Auditoria, Long> auditorias(EntityManagerFactory f) {
            return repositorio(AuditoriaRepository.class, f);
        }
        @Bean PagoLaboratorioService servicio(OrdenLaboratorioRepository o, PagoLaboratorioRepository p,
                UsuarioRepository u, AuditoriaRepository a) {
            return new PagoLaboratorioServiceImpl(o, p, u, a, mock(DatosSensiblesService.class));
        }
    }

    static void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cajero", "irrelevante", List.of(new SimpleGrantedAuthority("ROLE_CAJERO"))));
    }

    @BeforeEach void preparar() {
        contexto = new AnnotationConfigApplicationContext(Configuracion.class);
        servicio = contexto.getBean(PagoLaboratorioService.class);
        jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
        var transaccion = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        ordenId = transaccion.execute(estado -> {
            EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(contexto.getBean(EntityManagerFactory.class));
            Rol rol = new Rol(); rol.setCodigo("CAJERO"); rol.setNombre("Cajero"); em.persist(rol);
            Usuario cajero = new Usuario(); cajero.setRol(rol); cajero.setNombreUsuario("cajero");
            cajero.setPasswordHash("hash-de-prueba"); em.persist(cajero);
            Rol rolMedico = new Rol(); rolMedico.setCodigo("MEDICO"); rolMedico.setNombre("Médico"); em.persist(rolMedico);
            Usuario usuarioMedico = new Usuario(); usuarioMedico.setRol(rolMedico); usuarioMedico.setNombreUsuario("doctor");
            usuarioMedico.setPasswordHash("hash-de-prueba"); em.persist(usuarioMedico);
            Sucursal sucursal = new Sucursal(); sucursal.setNombre("Sucursal de prueba"); em.persist(sucursal);
            Personal personal = new Personal(); personal.setUsuario(usuarioMedico); personal.setSucursal(sucursal);
            personal.setNombreCompleto("Médico Demo"); em.persist(personal);
            Especialidad especialidad = new Especialidad(); especialidad.setNombre("General");
            especialidad.setDescripcion("Prueba CU-10"); em.persist(especialidad);
            Medico medico = new Medico(); medico.setPersonal(personal); medico.setEspecialidad(especialidad); em.persist(medico);
            Paciente paciente = new Paciente(); paciente.setNombreCompleto("Ana Paciente");
            paciente.setDpiCifrado("solo-datos-de-prueba"); paciente.setDpiHash("a".repeat(64)); em.persist(paciente);
            EstadoCita estadoCita = new EstadoCita(); estadoCita.setCodigo("PENDIENTE_CIERRE");
            estadoCita.setNombre("Pendiente de cierre"); em.persist(estadoCita);
            Cita cita = new Cita(); cita.setNumeroCita("CITA-TEST"); cita.setPaciente(paciente); cita.setMedico(medico);
            cita.setSucursal(sucursal); cita.setEstadoCita(estadoCita); cita.setMotivoConsulta("Prueba CU-10");
            cita.setFechaHoraInicio(LocalDateTime.now()); cita.setFechaHoraFin(LocalDateTime.now().plusMinutes(30)); em.persist(cita);
            ConsultaMedica consulta = new ConsultaMedica(); consulta.setCita(cita); consulta.setMedico(medico);
            consulta.setEstado("FINALIZADA"); consulta.setMotivoVisita("Prueba CU-10"); em.persist(consulta);
            Laboratorio laboratorio = new Laboratorio(); laboratorio.setNombre("Laboratorio"); em.persist(laboratorio);
            CatalogoExamen examen = new CatalogoExamen(); examen.setCodigo("HEM"); examen.setNombre("Hemograma");
            examen.setPrecio(new BigDecimal("75.00")); examen.setLaboratorio(laboratorio); em.persist(examen);
            OrdenLaboratorio orden = new OrdenLaboratorio(); orden.setNumeroOrden("LAB-TEST");
            orden.setConsultaMedica(consulta); orden.setMedico(medico); orden.setPaciente(paciente);
            orden.setEstado("PENDIENTE"); orden.setExterna(false); orden.setMontoTotal(new BigDecimal("75.00"));
            OrdenLaboratorioDetalle detalle = new OrdenLaboratorioDetalle(); detalle.setOrdenLaboratorio(orden);
            detalle.setExamen(examen); detalle.setPrecio(new BigDecimal("75.00")); orden.getDetalles().add(detalle);
            em.persist(orden); em.flush(); return orden.getId();
        });
        autenticar();
    }

    @AfterEach void cerrar() {
        SecurityContextHolder.clearContext(); if (contexto != null) contexto.close();
    }

    PagoLaboratorioFormDTO formulario() {
        var form = new PagoLaboratorioFormDTO(); form.setOrdenId(ordenId); form.setMetodoPago("EFECTIVO");
        form.setMontoRecibido(new BigDecimal("100.00")); return form;
    }

    String estadoOrden() {
        return jdbc.queryForObject("select estado from orden_laboratorio where id = ?", String.class, ordenId);
    }

    int cantidadPagos() { return jdbc.queryForObject("select count(*) from pago_laboratorio", Integer.class); }

    @Test void pagoYTransicionQuedanPersistidosYNoSeDuplican() {
        var pago = servicio.procesarPago("cajero", formulario());
        assertEquals(1, cantidadPagos()); assertEquals("EN_PROCESO", estadoOrden());
        assertNotNull(pago.getNumeroTransaccion());
        assertEquals(new BigDecimal("25.00"), jdbc.queryForObject("select cambio from pago_laboratorio", BigDecimal.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago("cajero", formulario()));
        assertEquals(1, cantidadPagos());
    }

    @Test void errorAlPersistirPagoNoCambiaOrden() {
        jdbc.execute("alter table pago_laboratorio add constraint fallo_pago check (monto <> 75.00)");
        assertThrows(RuntimeException.class, () -> servicio.procesarPago("cajero", formulario()));
        assertEquals(0, cantidadPagos()); assertEquals("PENDIENTE", estadoOrden());
    }

    @Test void errorDeAuditoriaReviertePagoYTransicion() {
        jdbc.execute("alter table auditoria add constraint fallo_auditoria check (accion <> 'PAGO_LABORATORIO')");
        assertThrows(RuntimeException.class, () -> servicio.procesarPago("cajero", formulario()));
        assertEquals(0, cantidadPagos()); assertEquals("PENDIENTE", estadoOrden());
        assertEquals(0, jdbc.queryForObject("select count(*) from auditoria", Integer.class));
    }

    @Test void errorEnCommitDeOrdenReviertePagoYAuditoria() {
        jdbc.execute("alter table orden_laboratorio add constraint fallo_estado check (estado <> 'EN_PROCESO')");
        assertThrows(RuntimeException.class, () -> servicio.procesarPago("cajero", formulario()));
        assertEquals(0, cantidadPagos()); assertEquals("PENDIENTE", estadoOrden());
        assertEquals(0, jdbc.queryForObject("select count(*) from auditoria", Integer.class));
    }

    @Test void dosCobrosSimultaneosProducenSoloUnPago() throws Exception {
        var ejecutor = Executors.newFixedThreadPool(2);
        var listos = new CountDownLatch(2); var inicio = new CountDownLatch(1);
        Callable<Boolean> cobrar = () -> {
            autenticar(); listos.countDown();
            try {
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No se inició la concurrencia");
                servicio.procesarPago("cajero", formulario()); return true;
            } catch (IllegalArgumentException yaCobrada) {
                return false;
            } finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var primero = ejecutor.submit(cobrar); var segundo = ejecutor.submit(cobrar);
            assertTrue(listos.await(10, TimeUnit.SECONDS)); inicio.countDown();
            int aprobados = (primero.get(20, TimeUnit.SECONDS) ? 1 : 0) + (segundo.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, aprobados); assertEquals(1, cantidadPagos()); assertEquals("EN_PROCESO", estadoOrden());
        } finally { inicio.countDown(); ejecutor.shutdownNow(); }
    }
}
