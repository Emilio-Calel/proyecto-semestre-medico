package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.AgendaMedicoController;
import com.example.proyecto_semestre_medico.controller.CitaApiController;
import com.example.proyecto_semestre_medico.dto.*;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.*;
import com.example.proyecto_semestre_medico.service.impl.*;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import javax.sql.DataSource;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// H2 efímera y beans explícitos: no carga PostgreSQL, application.properties ni schedulers.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgendaMedicaCu16Tests {
    AnnotationConfigApplicationContext contexto;
    AgendaMedicoService servicio;
    AgendaMedicoController controller;
    CitaDisponibilidadService disponibilidad;
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    EntityManager em;
    Long medicoId, otroMedicoId, pacienteId, sucursalId, especialidadId;
    LocalDateTime inicio;
    static final String RUTA = "/medico/agenda";

    @Configuration @EnableTransactionManagement @EnableMethodSecurity
    static class Configuracion {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:cu16_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", ""); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource fuente) {
            var f = new LocalContainerEntityManagerFactoryBean(); f.setDataSource(fuente);
            f.setPackagesToScan("com.example.proyecto_semestre_medico.model"); f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop")); return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f) { return new JpaTransactionManager(f); }
        static <T, R extends JpaRepository<T, Long>> JpaRepositoryFactoryBean<R, T, Long> repo(Class<R> tipo, EntityManagerFactory f) {
            var r = new JpaRepositoryFactoryBean<R, T, Long>(tipo); r.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(f)); return r;
        }
        @Bean JpaRepositoryFactoryBean<MedicoRepository, Medico, Long> medicos(EntityManagerFactory f) { return repo(MedicoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<CitaRepository, Cita, Long> citas(EntityManagerFactory f) { return repo(CitaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<EventoAgendaRepository, EventoAgenda, Long> eventos(EntityManagerFactory f) { return repo(EventoAgendaRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<TareaMedicoRepository, TareaMedico, Long> tareas(EntityManagerFactory f) { return repo(TareaMedicoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<HorarioMedicoRepository, HorarioMedico, Long> horarios(EntityManagerFactory f) { return repo(HorarioMedicoRepository.class, f); }
        @Bean JpaRepositoryFactoryBean<ConsultaMedicaRepository, ConsultaMedica, Long> consultas(EntityManagerFactory f) { return repo(ConsultaMedicaRepository.class, f); }
        @Bean AgendaMedicoService agenda(MedicoRepository m, CitaRepository c, EventoAgendaRepository e, TareaMedicoRepository t) {
            return new AgendaMedicoServiceImpl(m, c, e, t);
        }
        @Bean AgendaMedicoController controller(AgendaMedicoService s) { return new AgendaMedicoController(s); }
        @Bean CitaApiController apiCitas(CitaDisponibilidadService d) { return new CitaApiController(mock(CitaCatalogoService.class), d); }
        @Bean SeguimientoCitaService seguimiento(CitaRepository c, ConsultaMedicaRepository cm, MedicoRepository m,
                EventoAgendaRepository e, CitaDisponibilidadService d) {
            return new SeguimientoCitaServiceImpl(c, cm, m, mock(EstadoCitaRepository.class), e, d,
                    mock(CorreoPendienteService.class), 5, 30, new CorreoSeguimientoPlantilla("Central", ""));
        }
        @Bean CitaDisponibilidadService disponibilidad(HorarioMedicoRepository h, EventoAgendaRepository e, CitaRepository c, MedicoRepository m) {
            return new CitaDisponibilidadServiceImpl(h, e, c, m, 30);
        }
    }
    static Authentication autenticar(String nombre, String rol) {
        var auth = new UsernamePasswordAuthenticationToken(nombre, "prueba", List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(auth); return auth;
    }
    Authentication doctor() { return autenticar("doctor", "MEDICO"); }
    @BeforeAll void abrir() {
        contexto = new AnnotationConfigApplicationContext(Configuracion.class);
        servicio = contexto.getBean(AgendaMedicoService.class); controller = contexto.getBean(AgendaMedicoController.class);
        disponibilidad = contexto.getBean(CitaDisponibilidadService.class); jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
        tx = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        em = SharedEntityManagerCreator.createSharedEntityManager(contexto.getBean(EntityManagerFactory.class));
    }
    @BeforeEach void preparar() {
        doctor(); inicio = LocalDate.now().plusDays(3).atTime(10, 0);
        jdbc.execute("alter table evento_agenda drop constraint if exists fallo_evento");
        jdbc.execute("alter table tarea_medico drop constraint if exists fallo_tarea");
        for (String tabla : List.of("evento_agenda", "tarea_medico", "consulta_medica", "cita", "horario_medico", "medico", "personal", "paciente", "usuario", "rol", "estado_cita", "especialidad", "sucursal")) jdbc.update("delete from " + tabla);
        tx.executeWithoutResult(s -> {
            var rol = new Rol(); rol.setCodigo("MEDICO"); rol.setNombre("Médico"); em.persist(rol);
            var sucursal = new Sucursal(); sucursal.setNombre("Central"); em.persist(sucursal); sucursalId = sucursal.getId();
            var especialidad = new Especialidad(); especialidad.setNombre("General"); especialidad.setDescripcion("CU-16"); em.persist(especialidad); especialidadId = especialidad.getId();
            medicoId = crearMedico("doctor", rol, sucursal, especialidad).getId();
            otroMedicoId = crearMedico("otro", rol, sucursal, especialidad).getId();
            var paciente = new Paciente(); paciente.setNombreCompleto("Paciente Demo"); paciente.setDpiCifrado("prueba"); paciente.setDpiHash("a".repeat(64)); em.persist(paciente); pacienteId = paciente.getId();
            for (String codigo : List.of("CONFIRMADA", "PENDIENTE_PAGO", "CANCELADA", "NO_ASISTIO", "EXPIRADA", "PENDIENTE_CIERRE")) {
                var estado = new EstadoCita(); estado.setCodigo(codigo); estado.setNombre(codigo); em.persist(estado);
            }
            for (String dia : List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")) {
                var h = new HorarioMedico(); h.setMedico(em.getReference(Medico.class, medicoId)); h.setDiaSemana(dia);
                h.setHoraInicio(LocalTime.of(8, 0)); h.setHoraFin(LocalTime.of(18, 0)); em.persist(h);
            }
        });
    }
    Medico crearMedico(String nombre, Rol rol, Sucursal s, Especialidad e) {
        var u = new Usuario(); u.setNombreUsuario(nombre); u.setPasswordHash("prueba"); u.setRol(rol); em.persist(u);
        var p = new Personal(); p.setUsuario(u); p.setSucursal(s); p.setNombreCompleto(nombre); em.persist(p);
        var m = new Medico(); m.setPersonal(p); m.setEspecialidad(e); em.persist(m); return m;
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @AfterAll void cerrar() { contexto.close(); }
    EventoAgendaFormDTO evento() {
        var f = new EventoAgendaFormDTO(); f.setTitulo("Evento médico"); f.setDescripcion("Descripción del evento");
        f.setFechaInicio(inicio); f.setFechaFin(inicio.plusHours(1)); f.setTipoEvento("PERSONAL"); return f;
    }
    TareaMedicoFormDTO tarea() {
        var f = new TareaMedicoFormDTO(); f.setTitulo("Revisar resultados"); f.setDescripcion("Resultados pendientes"); f.setPrioridad("NORMAL"); return f;
    }
    List<AgendaItemDTO> agenda() { return servicio.listarAgenda("doctor", inicio.toLocalDate().atStartOfDay(), inicio.toLocalDate().plusDays(1).atStartOfDay()); }
    int cantidad(String tabla) { return jdbc.queryForObject("select count(*) from " + tabla, Integer.class); }
    boolean horario(LocalDateTime hora) { return disponibilidad.obtenerHorarios(medicoId, hora.toLocalDate()).stream().anyMatch(h -> h.getInicio().equals(hora)); }
    Long cita(String estado, Long medico, String origen) {
        return tx.execute(s -> {
            var c = new Cita(); c.setNumeroCita("CIT-" + UUID.randomUUID().toString().substring(0, 8));
            c.setPaciente(em.getReference(Paciente.class, pacienteId)); c.setMedico(em.getReference(Medico.class, medico));
            c.setEspecialidad(em.getReference(Especialidad.class, especialidadId)); c.setSucursal(em.getReference(Sucursal.class, sucursalId));
            c.setEstadoCita(em.createQuery("select e from EstadoCita e where e.codigo=:codigo", EstadoCita.class).setParameter("codigo", estado).getSingleResult());
            c.setMotivoConsulta("Consulta"); c.setOrigen(origen); c.setFechaHoraInicio(inicio); c.setFechaHoraFin(inicio.plusMinutes(30)); em.persist(c); return c.getId();
        });
    }
    @Test void medicoAbreSuAgendaSinPersistir() {
        var modelo = new ExtendedModelMap(); assertEquals("medico/agenda", controller.agenda("DIA", inicio.toLocalDate(), "TODAS", doctor(), modelo));
        assertEquals(0, cantidad("evento_agenda")); assertEquals(0, cantidad("tarea_medico")); assertTrue(((List<?>) modelo.get("agenda")).isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"PACIENTE","CAJERO","ADMINISTRADOR"})
    void noMedicoNoAccedeNiModifica(String rol) {
        var auth = autenticar("doctor", rol);
        assertThrows(AccessDeniedException.class, () -> controller.agenda("DIA", null, "TODAS", auth, new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class, () -> servicio.guardarEvento(evento(), "doctor"));
        assertThrows(AccessDeniedException.class, () -> servicio.guardarTarea(tarea(), "doctor"));
    }
    @Test void usuarioDebeTenerMedicoRealActivo() {
        autenticar("sinMedico", "MEDICO"); assertThrows(IllegalArgumentException.class, () -> servicio.listarTareas("sinMedico", "TODAS"));
        doctor(); jdbc.update("update medico set activo=false where id=?", medicoId);
        assertThrows(IllegalArgumentException.class, this::agenda);
    }
    @Test void noPermiteSuplantarNombreDeOtroMedico() {
        assertThrows(AccessDeniedException.class, () -> servicio.listarAgenda("otro", inicio, inicio.plusDays(1)));
        assertThrows(AccessDeniedException.class, () -> servicio.guardarEvento(evento(), "otro"));
        assertThrows(AccessDeniedException.class, () -> servicio.listarTareas("otro", "TODAS"));
    }
    @Test void calendarioSoloMuestraCitasYEventosPropios() {
        Long cita = cita("CONFIRMADA", medicoId, "ONLINE"); cita("CONFIRMADA", otroMedicoId, "ONLINE");
        var propio = servicio.guardarEvento(evento(), "doctor"); autenticar("otro", "MEDICO"); servicio.guardarEvento(evento(), "otro"); doctor();
        var lista = agenda(); assertEquals(2, lista.size());
        assertTrue(lista.stream().anyMatch(i -> i.getTipo().equals("CITA") && i.getId().equals(cita) && i.getTitulo().equals("Paciente Demo")));
        assertTrue(lista.stream().anyMatch(i -> i.getTipo().equals("EVENTO") && i.getId().equals(propio.getId())));
    }
    @ParameterizedTest @ValueSource(strings={"PENDIENTE_PAGO","CANCELADA","NO_ASISTIO","EXPIRADA"})
    void calendarioExcluyeEstadosNoProgramados(String estado) {
        cita(estado, medicoId, "ONLINE"); cita(estado, medicoId, "SEGUIMIENTO"); assertTrue(agenda().isEmpty());
    }
    @Test void conservaEstadoAdicionalCompatible() { cita("PENDIENTE_CIERRE", medicoId, "ONLINE"); assertEquals(1, agenda().size()); }

    @ParameterizedTest @ValueSource(strings={"nulo","vacio","corto","largo","inicioNulo","pasado","finNulo","finIgual","finAnterior","tipoNulo","tipoInvalido","descripcion","diaHoras","diaPasado"})
    void eventoRechazaCamposInvalidosSinPersistir(String caso) {
        var f = evento();
        switch (caso) {
            case "nulo" -> f.setTitulo(null); case "vacio" -> f.setTitulo("  "); case "corto" -> f.setTitulo("1234"); case "largo" -> f.setTitulo("x".repeat(201));
            case "inicioNulo" -> f.setFechaInicio(null); case "pasado" -> f.setFechaInicio(LocalDateTime.now().minusMinutes(2));
            case "finNulo" -> f.setFechaFin(null); case "finIgual" -> f.setFechaFin(inicio); case "finAnterior" -> f.setFechaFin(inicio.minusMinutes(1));
            case "tipoNulo" -> f.setTipoEvento(null); case "tipoInvalido" -> f.setTipoEvento("ARBITRARIO"); case "descripcion" -> f.setDescripcion("x".repeat(2001));
            case "diaHoras" -> f.setTodoElDia(true);
            case "diaPasado" -> { f.setTodoElDia(true); f.setFechaInicio(LocalDate.now().minusDays(1).atStartOfDay()); f.setFechaFin(LocalDate.now().atStartOfDay()); }
        }
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarEvento(f, "doctor")); assertEquals(0, cantidad("evento_agenda"));
    }
    @ParameterizedTest @ValueSource(strings={"BLOQUEO","PERSONAL","CAPACITACION","VACACIONES"})
    void cuatroTiposValidosConPropietarioPersistido(String tipo) {
        var f = evento(); f.setTipoEvento(tipo); var e = servicio.guardarEvento(f, "doctor");
        assertEquals(medicoId, jdbc.queryForObject("select medico_id from evento_agenda where id=?", Long.class, e.getId()));
        assertEquals(tipo, servicio.obtenerEvento(e.getId(), "doctor").getTipoEvento());
    }
    @Test void hoyHoraFuturaYTodoElDiaHoySeAdmiten() {
        var f = evento(); f.setFechaInicio(LocalDateTime.now().plusMinutes(5)); f.setFechaFin(f.getFechaInicio().plusHours(1)); servicio.guardarEvento(f, "doctor");
        f.setTodoElDia(true); f.setFechaInicio(LocalDate.now().atStartOfDay()); f.setFechaFin(LocalDate.now().plusDays(1).atStartOfDay());
        var e = servicio.guardarEvento(f, "doctor"); assertTrue(servicio.obtenerEvento(e.getId(), "doctor").getTodoElDia());
    }
    @Test void edicionPrellenaYPersisteRNCompleta() {
        var e = servicio.guardarEvento(evento(), "doctor"); var f = servicio.obtenerEvento(e.getId(), "doctor");
        assertEquals("Evento médico", f.getTitulo()); assertEquals(inicio, f.getFechaInicio()); assertEquals("Descripción del evento", f.getDescripcion());
        f.setTitulo("Título editado"); servicio.guardarEvento(f, "doctor"); assertEquals("Título editado", servicio.obtenerEvento(e.getId(), "doctor").getTitulo());
        f.setTitulo("1234"); assertThrows(IllegalArgumentException.class, () -> servicio.guardarEvento(f, "doctor"));
        assertEquals("Título editado", servicio.obtenerEvento(e.getId(), "doctor").getTitulo());
    }
    @Test void eventoAjenoNoPuedeObtenerseEditarseNiEliminarse() {
        var e = servicio.guardarEvento(evento(), "doctor"); var f = evento(); f.setId(e.getId()); autenticar("otro", "MEDICO");
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerEvento(e.getId(), "otro"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarEvento(f, "otro"));
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarEvento(e.getId(), "otro"));
        assertTrue(jdbc.queryForObject("select activo from evento_agenda where id=?", Boolean.class, e.getId()));
    }
    @Test void eliminarEventoLogicamenteYRepetirNoResucita() {
        var e = servicio.guardarEvento(evento(), "doctor"); servicio.eliminarEvento(e.getId(), "doctor");
        assertEquals(1, cantidad("evento_agenda")); assertTrue(agenda().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarEvento(e.getId(), "doctor"));
        var f = evento(); f.setId(e.getId()); assertThrows(IllegalArgumentException.class, () -> servicio.guardarEvento(f, "doctor"));
        f.setId(-1L); assertThrows(IllegalArgumentException.class, () -> servicio.guardarEvento(f, "doctor"));
    }
    @Test void bloqueoCreaEditaCambiaTipoYEliminaEnDisponibilidadCompartida() {
        assertTrue(horario(inicio)); var f = evento(); f.setTipoEvento("BLOQUEO"); var e = servicio.guardarEvento(f, "doctor");
        assertFalse(horario(inicio)); assertTrue(horario(inicio.plusHours(1)));
        f.setId(e.getId()); f.setFechaInicio(inicio.plusHours(1)); f.setFechaFin(inicio.plusHours(2)); servicio.guardarEvento(f, "doctor");
        assertTrue(horario(inicio)); assertFalse(horario(inicio.plusHours(1)));
        f.setTipoEvento("PERSONAL"); servicio.guardarEvento(f, "doctor"); assertTrue(horario(inicio.plusHours(1)));
        f.setTipoEvento("BLOQUEO"); servicio.guardarEvento(f, "doctor"); assertFalse(horario(inicio.plusHours(1)));
        servicio.eliminarEvento(e.getId(), "doctor"); assertTrue(horario(inicio.plusHours(1)));
    }
    @ParameterizedTest @CsvSource({"PERSONAL,true", "CAPACITACION,false", "VACACIONES,false", "BLOQUEO,false"})
    void tiposRespetanDisponibilidadCompartida(String tipo, boolean disponible) {
        var f = evento(); f.setTipoEvento(tipo); servicio.guardarEvento(f, "doctor"); assertEquals(disponible, horario(inicio));
    }
    @Test void vacacionesBloqueanCu03YCu12AlCrearMoverYDesactivarSinAlterarCitas() {
        Long origenId = cita("PENDIENTE_CIERRE", medicoId, "ONLINE");
        Long existenteId = cita("CONFIRMADA", medicoId, "ONLINE");
        tx.executeWithoutResult(s -> {
            var origen = em.find(Cita.class, origenId);
            origen.setFechaHoraInicio(inicio.minusDays(4)); origen.setFechaHoraFin(inicio.minusDays(4).plusMinutes(30));
            var consulta = new ConsultaMedica(); consulta.setCita(origen); consulta.setMedico(origen.getMedico());
            consulta.setEstado("FINALIZADA"); consulta.setMotivoVisita("Consulta de origen"); em.persist(consulta);
            var existente = em.find(Cita.class, existenteId);
            existente.setFechaHoraInicio(inicio.plusHours(2)); existente.setFechaHoraFin(inicio.plusHours(2).plusMinutes(30));
        });
        var citasAntes = jdbc.queryForList("select * from cita order by id");
        comprobarHorarioCu03YCu12(origenId, inicio, true);
        var f = evento(); f.setTipoEvento("VACACIONES"); var e = servicio.guardarEvento(f, "doctor");
        comprobarHorarioCu03YCu12(origenId, inicio, false);
        comprobarHorarioCu03YCu12(origenId, inicio.plusHours(1), true);
        f.setId(e.getId()); f.setFechaInicio(inicio.plusHours(1)); f.setFechaFin(inicio.plusHours(2));
        servicio.guardarEvento(f, "doctor");
        comprobarHorarioCu03YCu12(origenId, inicio, true);
        comprobarHorarioCu03YCu12(origenId, inicio.plusHours(1), false);
        servicio.eliminarEvento(e.getId(), "doctor");
        comprobarHorarioCu03YCu12(origenId, inicio.plusHours(1), true);
        comprobarHorarioCu03YCu12(origenId, inicio.plusHours(2), false); // Sigue ocupado por la cita existente.
        assertEquals(citasAntes, jdbc.queryForList("select * from cita order by id"));
        assertFalse(jdbc.queryForObject("select activo from evento_agenda where id=?", Boolean.class, e.getId()));
    }
    void comprobarHorarioCu03YCu12(Long origenId, LocalDateTime hora, boolean esperado) {
        autenticar("paciente", "PACIENTE");
        var horariosCu03 = contexto.getBean(CitaApiController.class).listarHorarios(medicoId, hora.toLocalDate());
        assertEquals(esperado, horariosCu03.stream().anyMatch(h -> h.getInicio().equals(hora)), "CU-03: " + hora);
        doctor();
        var horariosCu12 = contexto.getBean(SeguimientoCitaService.class).obtenerHorariosDisponibles(origenId, hora.toLocalDate(), "doctor");
        assertEquals(esperado, horariosCu12.stream().anyMatch(h -> h.getInicio().equals(hora)), "CU-12: " + hora);
    }
    @Test void bloqueoTodoElDiaAfectaHorarioCompleto() {
        var f = evento(); f.setTodoElDia(true); f.setTipoEvento("BLOQUEO"); f.setFechaInicio(inicio.toLocalDate().atStartOfDay()); f.setFechaFin(inicio.toLocalDate().plusDays(1).atStartOfDay());
        servicio.guardarEvento(f, "doctor"); assertTrue(disponibilidad.obtenerHorarios(medicoId, inicio.toLocalDate()).isEmpty());
    }
    @Test void eventosHeredadosMantienenBloqueoYEdicionLosPresentaComoBloqueo() {
        var e = servicio.guardarEvento(evento(), "doctor"); jdbc.update("update evento_agenda set tipo_evento='REUNION' where id=?", e.getId());
        assertFalse(horario(inicio)); assertEquals("BLOQUEO", servicio.obtenerEvento(e.getId(), "doctor").getTipoEvento());
    }
    @Test void errorCriticoRevierteEdicionEvento() {
        var e = servicio.guardarEvento(evento(), "doctor"); jdbc.execute("alter table evento_agenda add constraint fallo_evento check (titulo <> 'Fallo intencional')");
        var f = evento(); f.setId(e.getId()); f.setTitulo("Fallo intencional"); f.setTipoEvento("BLOQUEO");
        assertThrows(RuntimeException.class, () -> servicio.guardarEvento(f, "doctor"));
        assertEquals("PERSONAL", servicio.obtenerEvento(e.getId(), "doctor").getTipoEvento()); assertTrue(horario(inicio));
    }
    @ParameterizedTest @ValueSource(strings={"nulo","vacio","corto","largo","prioridadNula","prioridadInvalida","fechaPasada","fechaActual","estadoInvalido","estadoNulo","descripcion"})
    void tareaValidaCamposSinPersistir(String caso) {
        var f = tarea(); switch (caso) {
            case "nulo" -> f.setTitulo(null); case "vacio" -> f.setTitulo(" "); case "corto" -> f.setTitulo("1234"); case "largo" -> f.setTitulo("x".repeat(201));
            case "prioridadNula" -> f.setPrioridad(null); case "prioridadInvalida" -> f.setPrioridad("URGENTE");
            case "fechaPasada" -> f.setFechaLimite(LocalDateTime.now().minusDays(1)); case "fechaActual" -> f.setFechaLimite(LocalDateTime.now());
            case "estadoInvalido" -> f.setEstado("INVENTADO"); case "estadoNulo" -> f.setEstado(null); case "descripcion" -> f.setDescripcion("x".repeat(2001));
        }
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarTarea(f, "doctor")); assertEquals(0, cantidad("tarea_medico"));
    }
    @Test void tareaDefaultPendienteSinFechaYPropietarioCorrecto() {
        var t = servicio.guardarTarea(tarea(), "doctor"); assertEquals("PENDIENTE", t.getEstado()); assertFalse(t.getCompletada()); assertNull(t.getFechaLimite());
        assertEquals(medicoId, jdbc.queryForObject("select medico_id from tarea_medico", Long.class));
        assertEquals("Media", t.getPrioridadDescripcion());
    }
    @ParameterizedTest @ValueSource(strings={"BAJA","NORMAL","ALTA"})
    void prioridadesValidasYFechaHoyFutura(String prioridad) {
        var f = tarea(); f.setPrioridad(prioridad); f.setFechaLimite(LocalDateTime.now().plusMinutes(5));
        assertEquals(prioridad, servicio.guardarTarea(f, "doctor").getPrioridad());
    }
    @Test void tresEstadosFiltrosYDescompletar() {
        var a = servicio.guardarTarea(tarea(), "doctor"); var b = servicio.guardarTarea(tarea(), "doctor"); var c = servicio.guardarTarea(tarea(), "doctor");
        servicio.cambiarEstadoTarea(b.getId(), "EN_PROGRESO", "doctor"); servicio.cambiarEstadoTarea(c.getId(), "COMPLETADA", "doctor");
        assertEquals(Set.of(a.getId(), b.getId()), new HashSet<>(servicio.listarTareas("doctor", "PENDIENTES").stream().map(TareaMedico::getId).toList()));
        assertEquals(List.of(c.getId()), servicio.listarTareas("doctor", "COMPLETADAS").stream().map(TareaMedico::getId).toList());
        assertEquals(3, servicio.listarTareas("doctor", "TODAS").size());
        servicio.cambiarEstadoTarea(c.getId(), "PENDIENTE", "doctor"); assertTrue(servicio.listarTareas("doctor", "COMPLETADAS").isEmpty());
        servicio.cambiarEstadoTarea(c.getId(), "PENDIENTE", "doctor"); assertEquals(3, servicio.listarTareas("doctor", "PENDIENTES").size());
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarEstadoTarea(a.getId(), "OTRO", "doctor"));
    }
    @Test void tareaAjenaNoVisibleNiEditableNiEliminable() {
        var t = servicio.guardarTarea(tarea(), "doctor"); var f = tarea(); f.setId(t.getId()); autenticar("otro", "MEDICO");
        assertTrue(servicio.listarTareas("otro", "TODAS").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarTarea(f, "otro"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarEstadoTarea(t.getId(), "COMPLETADA", "otro"));
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarTarea(t.getId(), "otro"));
    }
    @Test void eliminarTareaLogicamenteYRechazarInexistente() {
        var t = servicio.guardarTarea(tarea(), "doctor"); servicio.eliminarTarea(t.getId(), "doctor"); assertEquals(1, cantidad("tarea_medico"));
        assertTrue(servicio.listarTareas("doctor", "TODAS").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarTarea(t.getId(), "doctor"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarEstadoTarea(-1L, "COMPLETADA", "doctor"));
        var f = tarea(); f.setId(-1L); assertThrows(IllegalArgumentException.class, () -> servicio.guardarTarea(f, "doctor"));
    }
    @Test void errorCriticoRevierteEstadoTarea() {
        var t = servicio.guardarTarea(tarea(), "doctor"); jdbc.execute("alter table tarea_medico add constraint fallo_tarea check (estado <> 'COMPLETADA')");
        assertThrows(RuntimeException.class, () -> servicio.cambiarEstadoTarea(t.getId(), "COMPLETADA", "doctor"));
        assertEquals("PENDIENTE", servicio.listarTareas("doctor", "TODAS").get(0).getEstado());
        assertFalse(jdbc.queryForObject("select completada from tarea_medico", Boolean.class));
    }
    @Test void mensajesLiteralesCreacionYActualizacionDesdeDatosGuardados() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        String fecha = inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        mvc.perform(post(RUTA + "/evento/guardar").principal(doctor()).param("titulo", "  Evento médico  ").param("tipoEvento", "PERSONAL")
                        .param("fechaInicio", inicio.toString()).param("fechaFin", inicio.plusHours(1).toString()))
                .andExpect(redirectedUrl(RUTA)).andExpect(flash().attribute("mensaje", "Evento creado exitosamente. Evento médico — " + fecha + " a " + fecha + "."));
        Long id = jdbc.queryForObject("select id from evento_agenda", Long.class);
        mvc.perform(post(RUTA + "/evento/guardar").principal(doctor()).param("id", id.toString()).param("titulo", "Evento editado").param("tipoEvento", "PERSONAL")
                        .param("fechaInicio", inicio.toString()).param("fechaFin", inicio.plusHours(1).toString()))
                .andExpect(flash().attribute("mensaje", "Evento actualizado exitosamente."));
    }
    @ParameterizedTest @ValueSource(strings={"medicoId","nombreUsuario","color"})
    void camposAjenosYColorNoConfigurableRechazados(String campo) throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post(RUTA + "/evento/guardar").principal(doctor()).param("titulo", "Evento médico").param("tipoEvento", "PERSONAL")
                        .param("fechaInicio", inicio.toString()).param("fechaFin", inicio.plusHours(1).toString()).param(campo, "manipulado"))
                .andExpect(flash().attributeExists("eventoError", "eventoForm")); assertEquals(0, cantidad("evento_agenda"));
    }
    @Test void erroresDeConversionYValidacionConservanFormulario() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post(RUTA + "/evento/guardar").principal(doctor()).param("titulo", "Evento válido").param("fechaInicio", "fecha-invalida"))
                .andExpect(flash().attributeExists("eventoError", "eventoForm"));
        mvc.perform(post(RUTA + "/tarea/guardar").principal(doctor()).param("titulo", "Tarea válida").param("prioridad", "OTRA"))
                .andExpect(flash().attributeExists("tareaError", "tareaForm"));
        mvc.perform(post(RUTA + "/tarea/guardar").principal(doctor()).param("titulo", "Tarea válida").param("prioridad", "NORMAL").param("medicoId", otroMedicoId.toString()))
                .andExpect(flash().attributeExists("tareaError")); assertEquals(0, cantidad("tarea_medico"));
    }
    @ParameterizedTest @ValueSource(strings={"DIA","SEMANA","MES"})
    void vistasNavegacionDetallesPanelYTodoElDia(String vista) throws Exception {
        cita("CONFIRMADA", medicoId, "ONLINE"); var f = evento(); f.setTodoElDia(true);
        f.setFechaInicio(inicio.toLocalDate().minusDays(1).atStartOfDay()); f.setFechaFin(inicio.toLocalDate().plusDays(1).atStartOfDay());
        servicio.guardarEvento(f, "doctor"); servicio.guardarTarea(tarea(), "doctor");
        var modelo = new ExtendedModelMap(); controller.agenda(vista, inicio.toLocalDate(), "PENDIENTES", doctor(), modelo);
        @SuppressWarnings("unchecked") var dias = (Map<LocalDate, List<AgendaItemDTO>>) modelo.get("agendaPorDia");
        assertEquals(2, dias.get(inicio.toLocalDate()).size());
        if (dias.containsKey(inicio.toLocalDate().plusDays(1))) assertTrue(dias.get(inicio.toLocalDate().plusDays(1)).isEmpty());
        assertEquals(switch (vista) { case "MES" -> inicio.toLocalDate().minusMonths(1); case "DIA" -> inicio.toLocalDate().minusDays(1); default -> inicio.toLocalDate().minusWeeks(1); }, modelo.get("fechaAnterior"));
        String html = renderizar(modelo);
        for (String texto : List.of("Mes", "Semana", "Día", "Anterior", "Siguiente", "Hoy", "Todo el día", "Paciente Demo", "CONFIRMADA", "¿Está seguro de eliminar este evento?", "Sin fecha límite", "Pendiente", "Media", "Mis tareas (1)", "#8b5cf6")) assertTrue(html.contains(texto), texto);
        assertTrue(html.contains("<details>")); assertTrue(html.contains("aria-controls=\"task-panel\""));
        assertTrue(html.contains("panel.hidden = !panel.hidden")); assertTrue(html.contains("filtroTareas=PENDIENTES"));
        assertTrue(html.contains("href=\"/medico\"")); assertTrue(html.contains("href=\"/dashboard\""));
        assertFalse(html.contains("href=\"/\""));
        String panel = Files.readString(Path.of("src/main/resources/templates/medico/panel.html"));
        assertTrue(panel.contains("@{/medico/agenda}")); assertTrue(panel.contains("Agenda Médica"));
    }
    String renderizar(Map<String, Object> variables) {
        var servlet = new org.springframework.mock.web.MockServletContext(); var request = new org.springframework.mock.web.MockHttpServletRequest(servlet);
        request.setRequestURI(RUTA); var response = new org.springframework.mock.web.MockHttpServletResponse();
        try (var web = new org.springframework.web.context.support.StaticWebApplicationContext()) {
            web.setServletContext(servlet); web.refresh(); servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, web);
            var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver(); resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
            var motor = new org.thymeleaf.spring6.SpringTemplateEngine(); motor.setTemplateResolver(resolver);
            var exchange = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet).buildExchange(request, response);
            var ctx = new org.thymeleaf.context.WebContext(exchange, Locale.forLanguageTag("es-GT")); ctx.setVariables(variables);
            ctx.setVariable("thymeleafRequestContext", new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(new org.springframework.web.servlet.support.RequestContext(request, response, servlet, variables), request));
            return org.springframework.web.util.HtmlUtils.htmlUnescape(motor.process("medico/agenda", ctx));
        }
    }
}
