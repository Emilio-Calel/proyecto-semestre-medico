-- Aplicar antes de iniciar el HIS: Hibernate utiliza ddl-auto=validate.
-- Relación opcional para comprobar cancelación/vigencia antes de enviar el recordatorio.
ALTER TABLE correo_pendiente
    ADD COLUMN IF NOT EXISTS cita_recordatorio_id BIGINT REFERENCES cita(id);

-- Vincular recordatorios pendientes de la implementación anterior, sin tocar otros correos.
UPDATE correo_pendiente AS correo
SET cita_recordatorio_id = cita.id
FROM cita
WHERE correo.cita_recordatorio_id IS NULL
  AND correo.estado = 'PENDIENTE'
  AND cita.origen = 'SEGUIMIENTO'
  AND correo.asunto = 'Recordatorio de seguimiento - ' || cita.numero_cita;
