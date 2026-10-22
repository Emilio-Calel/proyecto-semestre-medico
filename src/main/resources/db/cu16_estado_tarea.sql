-- Aplicar antes de iniciar el HIS (ddl-auto=validate). Conserva tareas e historial.
BEGIN;
ALTER TABLE tarea_medico ADD COLUMN IF NOT EXISTS estado VARCHAR(20);
UPDATE tarea_medico
SET estado = CASE WHEN completada THEN 'COMPLETADA' ELSE 'PENDIENTE' END
WHERE estado IS NULL;
ALTER TABLE tarea_medico ALTER COLUMN estado SET DEFAULT 'PENDIENTE';
ALTER TABLE tarea_medico ALTER COLUMN estado SET NOT NULL;
ALTER TABLE tarea_medico ALTER COLUMN fecha_limite DROP NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'tarea_medico'::regclass AND conname = 'ck_tarea_medico_estado') THEN
        ALTER TABLE tarea_medico ADD CONSTRAINT ck_tarea_medico_estado
            CHECK (estado IN ('PENDIENTE', 'EN_PROGRESO', 'COMPLETADA')
                   AND completada = (estado = 'COMPLETADA'));
    END IF;
END;
$$;
COMMIT;
