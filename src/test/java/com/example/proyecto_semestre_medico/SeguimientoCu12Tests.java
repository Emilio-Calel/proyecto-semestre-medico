package com.example.proyecto_semestre_medico;

import com.example.proyecto_semestre_medico.controller.MedicoController;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeguimientoCu12Tests {
    CitaRepository citas;
    ConsultaMedicaRepository consultas;
    MedicoRepository medicos;
    EstadoCitaRepository estados;
    EventoAgendaRepository eventos;
    CitaDisponibilidadService disponibilidad;
    CorreoPendienteService cola;
    CorreoSeguimientoPlantilla plantilla;
    SeguimientoCitaServiceImpl servicio;
    MedicoController controller;
    SeguimientoCitaDTO form;
    Cita origen;
    ConsultaMedica consulta;
    Medico medico;
    Paciente paciente;
    Sucursal sucursal;
    Especialidad especialidad;
    LocalDateTime inicio;
    static final String FECHA = "Seleccione una fecha futura dentro de los horarios disponibles del médico.";
    static final String OCUPADO = "El horario seleccionado ya no está disponible. Por favor, elija otro horario.";
    static final String OBSERVACIONES = "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.";

    @BeforeEach void preparar() {
        citas = mock(CitaRepository.class); consultas = mock(ConsultaMedicaRepository.class);
        medicos = mock(MedicoRepository.class); estados = mock(EstadoCitaRepository.class);
        eventos = mock(EventoAgendaRepository.class); disponibilidad = mock(CitaDisponibilidadService.class);
        cola = mock(CorreoPendienteService.class); plantilla = new CorreoSeguimientoPlantilla("Central", "22223333");
        servicio = new SeguimientoCitaServiceImpl(citas, consultas, medicos, estados, eventos, disponibilidad, cola, 10, 30, plantilla);
        controller = new MedicoController(mock(ConsultaMedicaService.class), mock(OrdenLaboratorioService.class),
                mock(RecetaService.class), servicio);
        sucursal = new Sucursal(); sucursal.setId(3L); sucursal.setNombre("Central");
        especialidad = new Especialidad(); especialidad.setId(4L); especialidad.setNombre("Medicina General");
        Personal personal = new Personal(); personal.setNombreCompleto("Médico Demo"); personal.setSucursal(sucursal);
        medico = new Medico(); medico.setId(7L); medico.setPersonal(personal); medico.setEspecialidad(especialidad);
        paciente = new Paciente(); ReflectionTestUtils.setField(paciente, "id", 9L);
        paciente.setNombreCompleto("Ana Paciente"); paciente.setCorreo("ana@example.test");
        origen = new Cita(); ReflectionTestUtils.setField(origen, "id", 11L); origen.setNumeroCita("CIT-ORIGEN");
        origen.setPaciente(paciente); origen.setMedico(medico); origen.setEspecialidad(especialidad); origen.setSucursal(sucursal);
        origen.setEstadoCita(estado("PENDIENTE_CIERRE"));
        consulta = new ConsultaMedica(); consulta.setId(21L); consulta.setCita(origen); consulta.setMedico(medico); consulta.setEstado("FINALIZADA");
        when(citas.findById(11L)).thenReturn(Optional.of(origen));
        when(consultas.findByCitaId(11L)).thenReturn(Optional.of(consulta));
        when(medicos.findByPersonalUsuarioNombreUsuario("doctor")).thenReturn(Optional.of(medico));
        when(medicos.buscarParaAgendar(7L)).thenReturn(Optional.of(medico));
        when(estados.findByCodigo("PENDIENTE_PAGO")).thenReturn(Optional.of(estado("PENDIENTE_PAGO")));
        inicio = LocalDate.now().plusDays(3).atTime(10, 0);
        when(disponibilidad.obtenerHorarios(7L, inicio.toLocalDate())).thenReturn(List.of(new HorarioDisponibleDTO(inicio, inicio.plusMinutes(30), "10:00")));
        when(citas.save(any())).thenAnswer(i -> { Cita c = i.getArgument(0); ReflectionTestUtils.setField(c, "id", 51L); return c; });
        form = new SeguimientoCitaDTO(); form.setCitaOrigenId(11L); form.setFechaHoraInicio(inicio);
        form.setTipoSeguimiento("MONITOREO_TRATAMIENTO"); form.setMotivo(" Control de síntomas y respuesta al tratamiento. ");
    }

    EstadoCita estado(String codigo) { var estado = new EstadoCita(); estado.setCodigo(codigo); return estado; }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void medicoAbreConsultaPropiaConTodoElContexto() {
        var contexto = servicio.obtenerContexto(11L, "doctor");
        assertEquals("Ana Paciente", contexto.getPaciente()); assertEquals("Médico Demo", contexto.getMedico());
        assertEquals("Medicina General", contexto.getEspecialidad()); assertEquals("Central", contexto.getSucursal());
        assertEquals(11L, contexto.getCitaId());
    }

    @Test void rechazaMedicoAjenoALaCitaOALaConsulta() {
        var otro = new Medico(); otro.setId(99L);
        when(medicos.findByPersonalUsuarioNombreUsuario("otro")).thenReturn(Optional.of(otro));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerContexto(11L, "otro"));
        consulta.setMedico(otro);
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerContexto(11L, "doctor"));
        verify(citas, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"consulta", "paciente", "medico", "especialidad", "sucursal", "consultaNoFinalizada", "sucursalIncoherente"})
    void rechazaContextoInexistenteOIncoherente(String campo) {
        switch (campo) {
            case "consulta" -> when(consultas.findByCitaId(11L)).thenReturn(Optional.empty());
            case "paciente" -> origen.setPaciente(null);
            case "medico" -> origen.setMedico(null);
            case "especialidad" -> origen.setEspecialidad(null);
            case "sucursal" -> origen.setSucursal(null);
            case "consultaNoFinalizada" -> consulta.setEstado("EN_CURSO");
            case "sucursalIncoherente" -> { var otra = new Sucursal(); otra.setId(99L); medico.getPersonal().setSucursal(otra); }
        }
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerContexto(11L, "doctor"));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "OTRO", "MONITOREO_CONDICION"})
    void tipoObligatorioYRestringido(String tipo) {
        form.setTipoSeguimiento(tipo);
        assertEquals("Debe seleccionar el tipo de seguimiento.", assertThrows(IllegalArgumentException.class,
                () -> servicio.crearSeguimiento(form, "doctor")).getMessage()); verifyNoInteractions(cola);
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "corto"})
    void observacionesObligatorias(String motivo) {
        form.setMotivo(motivo);
        assertEquals(OBSERVACIONES, assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(form, "doctor")).getMessage());
    }

    @Test void validaLimitesDeObservacionesYRecortaEspacios() {
        form.setMotivo("x".repeat(2001));
        assertEquals(OBSERVACIONES, assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(form, "doctor")).getMessage());
        form.setMotivo("  " + "x".repeat(10) + "  ");
        assertEquals("x".repeat(10), servicio.crearSeguimiento(form, "doctor").getMotivoConsulta());
        form.setMotivo("x".repeat(2000)); assertEquals(2000, servicio.crearSeguimiento(form, "doctor").getMotivoConsulta().length());
    }

    @Test void fechaNulaOPasadaOInventadaSeRechaza() {
        for (LocalDateTime fecha : Arrays.asList(null, LocalDateTime.now().minusMinutes(1), inicio.plusMinutes(7))) {
            form.setFechaHoraInicio(fecha);
            assertEquals(FECHA, assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(form, "doctor")).getMessage());
        }
        verify(citas, never()).save(any());
    }

    @Test void revalidaDisponibilidadYUsaFa01SiSeOcupa() {
        when(citas.existeCitaEnRango(eq(7L), any(), any())).thenReturn(false, true);
        assertEquals(OCUPADO, assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(form, "doctor")).getMessage());
        verify(citas, never()).save(any()); verifyNoInteractions(cola);
    }

    @Test void bloqueoDeAgendaNoPermiteCrear() {
        when(eventos.existeBloqueoEnRango(eq(7L), any(), any())).thenReturn(true);
        assertEquals(OCUPADO, assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(form, "doctor")).getMessage());
    }

    @ParameterizedTest @ValueSource(strings={"MONITOREO_TRATAMIENTO", "REVISION_RESULTADOS_LABORATORIO"})
    void creaMismoContextoRelacionPadreTipoYDuracion(String tipo) {
        form.setTipoSeguimiento(tipo); form.setPrioridad(null);
        Cita creada = servicio.crearSeguimiento(form, "doctor");
        assertSame(paciente, creada.getPaciente()); assertSame(medico, creada.getMedico());
        assertSame(sucursal, creada.getSucursal()); assertSame(especialidad, creada.getEspecialidad());
        assertSame(origen, creada.getCitaSeguimientoDe());
        assertEquals(consulta.getCita().getId(), creada.getCitaSeguimientoDe().getId());
        assertEquals(tipo, creada.getTipoSeguimiento()); assertEquals("NORMAL", creada.getPrioridadSeguimiento());
        assertEquals(inicio.plusMinutes(30), creada.getFechaHoraFin()); assertEquals("PENDIENTE_PAGO", creada.getEstadoCita().getCodigo());
        assertEquals("SEGUIMIENTO", creada.getOrigen()); assertFalse(creada.getPrioridadEmergencia());
        assertFalse(creada.getRecordatorioSeguimientoEnviado()); assertNull(creada.getFechaRecordatorioSeguimiento());
        verify(medicos).buscarParaAgendar(7L);
    }

    @ParameterizedTest @ValueSource(strings={"BAJA", "NORMAL", "ALTA"})
    void prioridadesExistentesSeConservan(String prioridad) {
        form.setPrioridad(prioridad); assertEquals(prioridad, servicio.crearSeguimiento(form, "doctor").getPrioridadSeguimiento());
    }

    @Test void prioridadNoDefinidaSeRechaza() {
        form.setPrioridad("URGENTE"); assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(form, "doctor"));
    }

    @Test void confirmacionTieneAsuntoContenidoYPieGlobal() {
        Cita creada = servicio.crearSeguimiento(form, "doctor");
        verify(cola).registrar(eq("ana@example.test"), eq("Cita de Seguimiento Agendada - Hospital Central"),
                argThat(texto -> texto.contains("Médico Demo") && texto.contains("Monitoreo de Tratamiento")
                        && texto.contains("Sucursal: Central") && texto.contains(inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        && texto.contains("Hora: 10:00") && texto.contains(creada.getMotivoConsulta())
                        && texto.contains("No responda a este mensaje.") && texto.contains("22223333")));
    }

    @Test void sinCorreoDelPacienteNoSeOmiteSilenciosamenteLaNotificacion() {
        paciente.setCorreo(null); assertThrows(IllegalArgumentException.class, () -> servicio.crearSeguimiento(form, "doctor"));
        verifyNoInteractions(cola);
    }

    @Test void mensajeDeExitoLiteralYManipulacionDeContextoIgnorada() {
        var binder = new WebDataBinder(form, "seguimientoForm"); controller.camposSeguimiento(binder);
        binder.bind(new MutablePropertyValues(Map.of("pacienteId", "99", "medicoId", "99", "sucursalId", "99", "especialidadId", "99")));
        var flash = new RedirectAttributesModelMap();
        assertEquals("redirect:/medico", controller.guardarSeguimiento(form, binder.getBindingResult(),
                new UsernamePasswordAuthenticationToken("doctor", "irrelevante"), flash));
        assertEquals("Cita de seguimiento agendada exitosamente. Tipo: Monitoreo de Tratamiento. Paciente: Ana Paciente.", flash.getFlashAttributes().get("mensaje"));
        assertEquals(4, binder.getBindingResult().getSuppressedFields().length);
    }

    @Test void conflictoConservaTipoObservacionesYContextoAlVolver() {
        when(citas.existeCitaEnRango(eq(7L), any(), any())).thenReturn(true);
        var flash = new RedirectAttributesModelMap(); var binder = new WebDataBinder(form, "seguimientoForm");
        controller.guardarSeguimiento(form, binder.getBindingResult(), new UsernamePasswordAuthenticationToken("doctor", "irrelevante"), flash);
        assertEquals(OCUPADO, flash.getFlashAttributes().get("error"));
        var modelo = new ExtendedModelMap(); modelo.addAllAttributes(flash.getFlashAttributes());
        controller.nuevoSeguimiento(11L, inicio.toLocalDate(), new UsernamePasswordAuthenticationToken("doctor", "irrelevante"), modelo, new RedirectAttributesModelMap());
        assertSame(form, modelo.get("seguimientoForm")); assertNotNull(modelo.get("contexto"));
        assertEquals("MONITOREO_TRATAMIENTO", form.getTipoSeguimiento()); assertTrue(form.getMotivo().contains("Control"));
    }

    @Test void conversionInvalidaNoCreaCitaYVolverSoloConsultaContexto() {
        var binder = new WebDataBinder(form, "seguimientoForm"); binder.bind(new MutablePropertyValues(Map.of("fechaHoraInicio", "no-fecha")));
        var flash = new RedirectAttributesModelMap(); controller.guardarSeguimiento(form, binder.getBindingResult(), null, flash);
        assertEquals(FECHA, flash.getFlashAttributes().get("error"));
        servicio.obtenerContexto(11L, "doctor"); verify(citas, never()).save(any()); verifyNoInteractions(cola);
    }

    Cita recordatorio() {
        Cita cita = servicio.crearSeguimiento(form, "doctor"); cita.setFechaHoraInicio(LocalDateTime.now().plusHours(36));
        when(citas.buscarSeguimientosParaRecordatorio(any(), any())).thenReturn(List.of(cita.getId()));
        when(citas.buscarParaSeguimiento(51L)).thenReturn(Optional.of(cita)); clearInvocations(cola); return cita;
    }

    @Test void recordatorioSeEncolaEnVentanaConAsuntoLiteralYSinDuplicados() {
        Cita cita = recordatorio(); var servicioRecordatorio = new RecordatorioSeguimientoServiceImpl(citas, cola, plantilla);
        assertEquals(1, servicioRecordatorio.procesarRecordatorios());
        verify(cola).registrarRecordatorio(eq(cita), eq("Recordatorio: Su Cita de Seguimiento Mañana"),
                argThat(t -> t.contains("Médico Demo") && t.contains("Sucursal: Central") && t.contains("Monitoreo de Tratamiento")
                        && t.contains("Fecha:") && t.contains("Hora:") && t.contains("No responda a este mensaje.")));
        assertTrue(cita.getRecordatorioSeguimientoEnviado()); assertNotNull(cita.getFechaRecordatorioSeguimiento());
        assertEquals(0, servicioRecordatorio.procesarRecordatorios());
    }

    @ParameterizedTest @ValueSource(strings={"CANCELADA", "EXPIRADA", "NO_ASISTIO"})
    void canceladaNoGeneraRecordatorio(String estado) {
        Cita cita = recordatorio(); cita.setEstadoCita(estado(estado));
        assertEquals(0, new RecordatorioSeguimientoServiceImpl(citas, cola, plantilla).procesarRecordatorios());
        verifyNoInteractions(cola); assertFalse(cita.getRecordatorioSeguimientoEnviado());
    }

    @Test void cancelacionPosteriorAEncolarImpideEnvio() {
        Cita cita = recordatorio(); cita.setEstadoCita(estado("CANCELADA"));
        var repositorio = mock(CorreoPendienteRepository.class); var smtp = mock(CorreoService.class);
        CorreoPendiente correo = new CorreoPendiente(); correo.setCitaRecordatorio(cita);
        when(repositorio.findByEstadoAndProximoIntentoLessThanEqualOrderByFechaCreacionAsc(eq("PENDIENTE"), any())).thenReturn(List.of(correo));
        assertEquals(0, new CorreoPendienteServiceImpl(repositorio, smtp, citas).procesarPendientes());
        verifyNoInteractions(smtp); assertEquals("FALLIDO", correo.getEstado());
    }

    @Test void cancelacionEntreSeleccionDeCandidatosYBloqueoSeRespeta() {
        Cita cita = recordatorio();
        when(citas.buscarParaSeguimiento(51L)).thenAnswer(i -> {
            cita.setEstadoCita(estado("CANCELADA")); return Optional.of(cita);
        });
        assertEquals(0, new RecordatorioSeguimientoServiceImpl(citas, cola, plantilla).procesarRecordatorios());
        verifyNoInteractions(cola);
    }

    @Test void smtpCaidoMantieneCorreoPendienteParaReintento() {
        var repositorio = mock(CorreoPendienteRepository.class); var smtp = mock(CorreoService.class);
        CorreoPendiente correo = new CorreoPendiente(); correo.setDestinatario("ana@example.test"); correo.setAsunto("Confirmación"); correo.setContenido("Contenido");
        when(repositorio.findByEstadoAndProximoIntentoLessThanEqualOrderByFechaCreacionAsc(eq("PENDIENTE"), any())).thenReturn(List.of(correo));
        doThrow(new IllegalStateException("SMTP temporalmente caído")).when(smtp).enviar(any(), any(), any());
        assertEquals(0, new CorreoPendienteServiceImpl(repositorio, smtp, citas).procesarPendientes());
        assertEquals("PENDIENTE", correo.getEstado()); assertEquals(1, correo.getIntentos()); assertNotNull(correo.getProximoIntento());
    }

    @Test void noEnviaRecordatorioFueraDeLaVentanaDocumentada() {
        Cita cita = recordatorio(); cita.setFechaHoraInicio(LocalDateTime.now().plusHours(12));
        assertEquals(0, new RecordatorioSeguimientoServiceImpl(citas, cola, plantilla).procesarRecordatorios());
        verifyNoInteractions(cola);
    }

    @Test void vistaConservaDosTiposObservacionesYVolverInterno() {
        var servlet = new org.springframework.mock.web.MockServletContext();
        var request = new org.springframework.mock.web.MockHttpServletRequest(servlet);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        var variables = new HashMap<String, Object>(); variables.put("contexto", servicio.obtenerContexto(11L, "doctor"));
        variables.put("seguimientoForm", form); variables.put("fechaSeleccionada", inicio.toLocalDate());
        variables.put("horarios", List.of(new HorarioDisponibleDTO(inicio, inicio.plusMinutes(30), "10:00")));
        try (var contextoSpring = new org.springframework.web.context.support.StaticWebApplicationContext()) {
            contextoSpring.setServletContext(servlet); contextoSpring.refresh();
            servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, contextoSpring);
            var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
            var motor = new org.thymeleaf.spring6.SpringTemplateEngine(); motor.setTemplateResolver(resolver);
            var exchange = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet).buildExchange(request, response);
            var contextoVista = new org.thymeleaf.context.WebContext(exchange, Locale.forLanguageTag("es-GT")); contextoVista.setVariables(variables);
            contextoVista.setVariable("thymeleafRequestContext", new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(
                    new org.springframework.web.servlet.support.RequestContext(request, response, servlet, variables), request));
            String html = org.springframework.web.util.HtmlUtils.htmlUnescape(motor.process("medico/seguimiento", contextoVista));
            assertTrue(html.contains("href=\"/medico\"")); assertFalse(html.contains("href=\"/\""));
            assertEquals(1, html.split("value=\"MONITOREO_TRATAMIENTO\"", -1).length - 1);
            assertEquals(1, html.split("value=\"REVISION_RESULTADOS_LABORATORIO\"", -1).length - 1);
            assertTrue(html.contains("Ana Paciente")); assertTrue(html.contains("Control de síntomas"));
            assertTrue(html.contains("Observaciones / motivo del seguimiento"));
        }
    }

    @Configuration @EnableMethodSecurity static class Seguridad { }

    @Test void soloMedicoConUsuarioDeSesionPuedeAcceder() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.register(Seguridad.class); contexto.registerBean(SeguimientoCitaServiceImpl.class, () -> servicio);
            contexto.registerBean(MedicoController.class, () -> controller); contexto.refresh();
            var protegido = contexto.getBean(SeguimientoCitaService.class);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> protegido.obtenerContexto(11L, "doctor"));
            for (String rol : List.of("FARMACEUTICO", "CAJERO", "PACIENTE")) {
                autenticar(rol);
                assertThrows(AccessDeniedException.class, () -> protegido.crearSeguimiento(form, "doctor"));
                assertThrows(AccessDeniedException.class, () -> contexto.getBean(MedicoController.class).nuevoSeguimiento(11L, null,
                        new UsernamePasswordAuthenticationToken("doctor", "irrelevante"), new ExtendedModelMap(), new RedirectAttributesModelMap()));
            }
            autenticar("MEDICO"); assertThrows(AccessDeniedException.class, () -> protegido.obtenerContexto(11L, "otro"));
            assertNotNull(protegido.obtenerContexto(11L, "doctor"));
        }
    }

    void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("doctor", "irrelevante",
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
