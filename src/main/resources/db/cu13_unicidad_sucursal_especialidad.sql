-- Ejecutar antes de iniciar el HIS (ddl-auto=validate). No elimina ni modifica asignaciones.
BEGIN;
LOCK TABLE sucursal_especialidad IN SHARE ROW EXCLUSIVE MODE;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM sucursal_especialidad
        GROUP BY sucursal_id, especialidad_id HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen combinaciones Sucursal-Especialidad duplicadas. Revisar sus referencias e historial antes de aplicar la unicidad; no se ha eliminado ningún registro.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'sucursal_especialidad'::regclass
          AND conname = 'uk_sucursal_especialidad'
    ) THEN
        ALTER TABLE sucursal_especialidad
            ADD CONSTRAINT uk_sucursal_especialidad UNIQUE (sucursal_id, especialidad_id);
    END IF;
END;
$$;
COMMIT;
