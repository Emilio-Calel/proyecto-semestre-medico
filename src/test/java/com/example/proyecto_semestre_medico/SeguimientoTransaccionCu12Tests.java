package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.dto.SeguimientoCitaDTO;
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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// JPA y cola reales con H2 efímera; SMTP simulado. Sin application.properties ni schedulers.
class SeguimientoTransaccionCu12Tests {
    AnnotationConfigApplicationContext contexto;
    SeguimientoCitaService servicio;
    RecordatorioSeguimientoService recordatorios;
    CorreoPendienteService cola;
    CorreoService smtp;
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    EntityManager em;
    Long origenId, consultaId, medicoId, pacienteId, sucursalId, especialidadId;
    LocalDateTime inicio;

    @Configuration @EnableTransactionManagement @EnableMethodSecurity
    static class Configuracion {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:cu12_" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        }
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
        @Bean JpaRepositoryFactoryBean<CitaRepository, Cita, Long> citas(EntityManagerFactory f) { return repo(CitaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<ConsultaMedicaRepository, ConsultaMedica, Long> consultas(EntityManagerFactory f) { return repo(ConsultaMedicaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<MedicoRepository, Medico, Long> medicos(EntityManagerFactory f) { return repo(MedicoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<EstadoCitaRepository, EstadoCita, Long> estados(EntityManagerFactory f) { return repo(EstadoCitaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<EventoAgendaRepository, EventoAgenda, Long> eventos(EntityManagerFactory f) { return repo(EventoAgendaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<HorarioMedicoRepository, HorarioMedico, Long> horarios(EntityManagerFactory f) { return repo(HorarioMedicoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<CorreoPendienteRepository, CorreoPendiente, Long> correos(EntityManagerFactory f) { return repo(CorreoPendienteRepository.class, f); }
        @Bean CorreoService smtp() { return mock(CorreoService.class); }
        @Bean CorreoPendienteService cola(CorreoPendienteRepository c, CorreoService smtp, CitaRepository citas) {
            return new CorreoPendienteServiceImpl(c, smtp, citas);
        }
        @Bean CorreoSeguimientoPlantilla plantilla() { return new CorreoSeguimientoPlantilla("Central", "22223333"); }
        @Bean CitaDisponibilidadService disponibilidad(HorarioMedicoRepository h, EventoAgendaRepository e,
                CitaRepository c, MedicoRepository m) { return new CitaDisponibilidadServiceImpl(h, e, c, m, 30); }
        @Bean SeguimientoCitaService seguimiento(CitaRepository c, ConsultaMedicaRepository cm, MedicoRepository m,
                EstadoCitaRepository ec, EventoAgendaRepository ea, CitaDisponibilidadService d, CorreoPendienteService correo,
                CorreoSeguimientoPlantilla plantilla) {
            return new SeguimientoCitaServiceImpl(c, cm, m, ec, ea, d, correo, 10, 30, plantilla);
        }
        @Bean RecordatorioSeguimientoService recordatorios(CitaRepository c, CorreoPendienteService cola, CorreoSeguimientoPlantilla p) {
            return new RecordatorioSeguimientoServiceImpl(c, cola, p);
        }
    }

    static void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "doctor", "irrelevante", List.of(new SimpleGrantedAuthority("ROLE_MEDICO"))));
    }

    @BeforeEach void preparar() {
        contexto = new AnnotationConfigApplicationContext(Configuracion.class);
        servicio = contexto.getBean(SeguimientoCitaService.class); recordatorios = contexto.getBean(RecordatorioSeguimientoService.class);
        cola = contexto.getBean(CorreoPendienteService.class); smtp = contexto.getBean(CorreoService.class);
        jdbc = new JdbcTemplate(contexto.getBean(DataSource.class)); tx = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        em = SharedEntityManagerCreator.createSharedEntityManager(contexto.getBean(EntityManagerFactory.class));
        tx.executeWithoutResult(s -> {
            Rol rol = new Rol(); rol.setCodigo("MEDICO"); rol.setNombre("Médico"); em.persist(rol);
            Usuario doctor = new Usuario(); doctor.setRol(rol); doctor.setNombreUsuario("doctor"); doctor.setPasswordHash("hash-de-prueba"); em.persist(doctor);
            Sucursal sucursal = new Sucursal(); sucursal.setNombre("Central"); em.persist(sucursal); sucursalId = sucursal.getId();
            Personal personal = new Personal(); personal.setUsuario(doctor); personal.setSucursal(sucursal); personal.setNombreCompleto("Médico Demo"); em.persist(personal);
            Especialidad especialidad = new Especialidad(); especialidad.setNombre("General"); especialidad.setDescripcion("CU-12"); em.persist(especialidad); especialidadId = especialidad.getId();
            Medico medico = new Medico(); medico.setPersonal(personal); medico.setEspecialidad(especialidad); em.persist(medico); medicoId = medico.getId();
            Paciente paciente = new Paciente(); paciente.setNombreCompleto("Ana Paciente"); paciente.setDpiCifrado("solo-prueba");
            paciente.setDpiHash("c".repeat(64)); paciente.setCorreo("ana@example.test"); em.persist(paciente); pacienteId = paciente.getId();
            EstadoCita origenEstado = null;
            for (String codigo : List.of("PENDIENTE_CIERRE", "PENDIENTE_PAGO", "CANCELADA")) {
                EstadoCita ec = new EstadoCita(); ec.setCodigo(codigo); ec.setNombre(codigo); em.persist(ec);
                if (codigo.equals("PENDIENTE_CIERRE")) origenEstado = ec;
            }
            Cita origen = new Cita(); origen.setNumeroCita("CIT-ORIGEN"); origen.setPaciente(paciente); origen.setMedico(medico);
            origen.setEspecialidad(especialidad); origen.setSucursal(sucursal); origen.setEstadoCita(origenEstado);
            origen.setMotivoConsulta("Consulta original"); origen.setFechaHoraInicio(LocalDateTime.now().minusDays(1));
            origen.setFechaHoraFin(LocalDateTime.now().minusDays(1).plusMinutes(30)); em.persist(origen); origenId = origen.getId();
            ConsultaMedica consulta = new ConsultaMedica(); consulta.setCita(origen); consulta.setMedico(medico);
            consulta.setEstado("FINALIZADA"); consulta.setMotivoVisita("Consulta original"); em.persist(consulta); consultaId = consulta.getId();
            for (String dia : List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")) {
                HorarioMedico horario = new HorarioMedico(); horario.setMedico(medico); horario.setDiaSemana(dia);
                horario.setHoraInicio(LocalTime.of(8, 0)); horario.setHoraFin(LocalTime.of(18, 0)); em.persist(horario);
            }
        });
        inicio = LocalDate.now().plusDays(3).atTime(10, 0); autenticar();
    }

    @AfterEach void cerrar() { SecurityContextHolder.clearContext(); if (contexto != null) contexto.close(); }

    SeguimientoCitaDTO formulario() {
        var f = new SeguimientoCitaDTO(); f.setCitaOrigenId(origenId); f.setFechaHoraInicio(inicio);
        f.setTipoSeguimiento("REVISION_RESULTADOS_LABORATORIO"); f.setMotivo("Revisar resultados de laboratorio publicados."); return f;
    }
    int seguimientos() { return jdbc.queryForObject("select count(*) from cita where origen='SEGUIMIENTO'", Integer.class); }
    int correos() { return jdbc.queryForObject("select count(*) from correo_pendiente", Integer.class); }

    @Test void persisteContextoRelacionConsultaTipoYCorreoSinContactarSmtp() {
        Cita creada = servicio.crearSeguimiento(formulario(), "doctor");
        assertEquals(1, seguimientos()); assertEquals(1, correos()); verifyNoInteractions(smtp);
        Map<String, Object> fila = jdbc.queryForMap("select paciente_id, medico_id, especialidad_id, sucursal_id, cita_seguimiento_de_id, tipo_seguimiento, motivo_consulta from cita where id=?", creada.getId());
        assertEquals(pacienteId, fila.get("PACIENTE_ID")); assertEquals(medicoId, fila.get("MEDICO_ID"));
        assertEquals(especialidadId, fila.get("ESPECIALIDAD_ID")); assertEquals(sucursalId, fila.get("SUCURSAL_ID"));
        assertEquals(origenId, fila.get("CITA_SEGUIMIENTO_DE_ID")); assertEquals("REVISION_RESULTADOS_LABORATORIO", fila.get("TIPO_SEGUIMIENTO"));
        assertEquals(consultaId, jdbc.queryForObject("select cm.id from cita s join consulta_medica cm on cm.cita_id=s.cita_seguimiento_de_id where s.id=?", Long.class, creada.getId()));
        assertFalse(creada.getRecordatorioSeguimientoEnviado());
        assertEquals("Cita de Seguimiento Agendada - Hospital Central", jdbc.queryForObject("select asunto from correo_pendiente", String.class));
    }

    @Test void ocupadoYDuplicadoNoCreanOtraCitaNiCorreo() {
        servicio.crearSeguimiento(formulario(), "doctor");
        assertEquals(SeguimientoCu12Tests.OCUPADO, assertThrows(IllegalArgumentException.class,
                () -> servicio.crearSeguimiento(formulario(), "doctor")).getMessage());
        assertEquals(1, seguimientos()); assertEquals(1, correos());
    }

    @Test void horarioFueraDeJornadaNoSeAcepta() {
        var f = formulario(); f.setFechaHoraInicio(inicio.withHour(23));
        assertEquals(SeguimientoCu12Tests.FECHA, assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(f, "doctor")).getMessage());
        assertEquals(0, seguimientos()); assertEquals(0, correos());
    }

    @Test void falloCriticoAlEncolarRevierteCita() {
        jdbc.execute("alter table correo_pendiente add constraint fallo_correo check (asunto not like 'Cita de Seguimiento Agendada%')");
        assertThrows(RuntimeException.class, () -> servicio.crearSeguimiento(formulario(), "doctor"));
        assertEquals(0, seguimientos()); assertEquals(0, correos());
    }

    @Test void smtpCaidoNoRevierteCitaYPermiteReintento() {
        servicio.crearSeguimiento(formulario(), "doctor");
        doThrow(new IllegalStateException("SMTP temporalmente caído")).when(smtp).enviar(any(), any(), any());
        assertEquals(0, cola.procesarPendientes()); assertEquals(1, seguimientos());
        assertEquals("PENDIENTE", jdbc.queryForObject("select estado from correo_pendiente", String.class));
        assertEquals(1, jdbc.queryForObject("select intentos from correo_pendiente", Integer.class));
    }

    Long crearDentroDeVentana() {
        Cita creada = servicio.crearSeguimiento(formulario(), "doctor");
        tx.executeWithoutResult(s -> {
            Cita c = em.find(Cita.class, creada.getId()); c.setFechaHoraInicio(LocalDateTime.now().plusHours(36));
            c.setFechaHoraFin(c.getFechaHoraInicio().plusMinutes(30));
        });
        return creada.getId();
    }

    void cancelar(Long id) {
        tx.executeWithoutResult(s -> em.find(Cita.class, id).setEstadoCita(contexto.getBean(EstadoCitaRepository.class).findByCodigo("CANCELADA").orElseThrow()));
    }

    @Test void recordatorioQuedaVinculadoAUnaCitaYNoSeDuplica() {
        Long id = crearDentroDeVentana();
        assertEquals(1, recordatorios.procesarRecordatorios()); assertEquals(0, recordatorios.procesarRecordatorios());
        assertEquals(2, correos());
        assertEquals(id, jdbc.queryForObject("select cita_recordatorio_id from correo_pendiente where cita_recordatorio_id is not null", Long.class));
        assertEquals("Recordatorio: Su Cita de Seguimiento Mañana", jdbc.queryForObject("select asunto from correo_pendiente where cita_recordatorio_id is not null", String.class));
    }

    @Test void canceladaNoGeneraRecordatorioYCanceladaDespuesDeColaNoLoEnvia() {
        Long id = crearDentroDeVentana(); cancelar(id);
        assertEquals(0, recordatorios.procesarRecordatorios()); assertEquals(1, correos());
        tx.executeWithoutResult(s -> em.find(Cita.class, id).setEstadoCita(contexto.getBean(EstadoCitaRepository.class).findByCodigo("PENDIENTE_PAGO").orElseThrow()));
        assertEquals(1, recordatorios.procesarRecordatorios()); cancelar(id);
        cola.procesarPendientes();
        verify(smtp, never()).enviar(any(), eq("Recordatorio: Su Cita de Seguimiento Mañana"), any());
        assertEquals("FALLIDO", jdbc.queryForObject("select estado from correo_pendiente where cita_recordatorio_id is not null", String.class));
    }

    @Test void consultarContextoOVolverNoPersisteNada() {
        servicio.obtenerContexto(origenId, "doctor");
        servicio.obtenerHorariosDisponibles(origenId, inicio.toLocalDate(), "doctor");
        assertEquals(0, seguimientos()); assertEquals(0, correos());
    }

    @Test void dobleConfirmacionConcurrenteSoloCreaUnaCita() throws Exception {
        var ejecutor = Executors.newFixedThreadPool(2); var listos = new CountDownLatch(2); var inicioComun = new CountDownLatch(1);
        Callable<Boolean> agendar = () -> {
            autenticar(); listos.countDown();
            try {
                if (!inicioComun.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No inició la concurrencia");
                servicio.crearSeguimiento(formulario(), "doctor"); return true;
            } catch (IllegalArgumentException ocupado) { assertEquals(SeguimientoCu12Tests.OCUPADO, ocupado.getMessage()); return false; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var primero = ejecutor.submit(agendar); var segundo = ejecutor.submit(agendar);
            assertTrue(listos.await(10, TimeUnit.SECONDS)); inicioComun.countDown();
            assertEquals(1, (primero.get(20, TimeUnit.SECONDS) ? 1 : 0) + (segundo.get(20, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(1, seguimientos()); assertEquals(1, correos());
        } finally { inicioComun.countDown(); ejecutor.shutdownNow(); }
    }
}
