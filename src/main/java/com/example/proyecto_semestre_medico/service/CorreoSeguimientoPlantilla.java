package com.example.proyecto_semestre_medico.service;

import com.example.proyecto_semestre_medico.model.Cita;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;

@Component
public class CorreoSeguimientoPlantilla {
    private final String hospital;
    private final String telefono;

    public CorreoSeguimientoPlantilla(@Value("${app.hospital.nombre:Hospital}") String hospital,
                                     @Value("${app.hospital.telefono:}") String telefono) {
        this.hospital = hospital;
        this.telefono = telefono;
    }

    public String asuntoConfirmacion() {
        String nombre = hospital.toLowerCase(java.util.Locale.ROOT).startsWith("hospital")
                ? hospital : "Hospital " + hospital;
        return "Cita de Seguimiento Agendada - " + nombre;
    }

    public String contenido(Cita cita, boolean recordatorio) {
        String contacto = telefono == null || telefono.isBlank()
                ? "Para consultas, comuníquese con el hospital."
                : "Para consultas, comuníquese al teléfono " + telefono + ".";
        return """
                Sistema Informático Hospitalario

                %s
                Paciente: %s
                Número de cita: %s
                Tipo de seguimiento: %s
                Médico: %s
                Especialidad: %s
                Sucursal: %s
                Fecha: %s
                Hora: %s
                Observaciones: %s

                Este es un correo automático del Sistema Informático Hospitalario. No responda a este mensaje.
                %s
                """.formatted(recordatorio ? "Recordatorio de cita de seguimiento." : "Se ha programado una cita de seguimiento.",
                cita.getPaciente().getNombreCompleto(), cita.getNumeroCita(), cita.getTipoSeguimientoDescripcion(),
                cita.getMedico().getPersonal().getNombreCompleto(), cita.getEspecialidad().getNombre(),
                cita.getSucursal().getNombre(), cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")), cita.getMotivoConsulta(), contacto);
    }
}
