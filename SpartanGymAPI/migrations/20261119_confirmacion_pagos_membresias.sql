ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'Confirmado';

ALTER TABLE membresias_socio
    DROP CONSTRAINT IF EXISTS membresias_socio_estado_check;

ALTER TABLE membresias_socio
    ADD CONSTRAINT membresias_socio_estado_check
    CHECK (estado IN ('Activa', 'Vencida', 'Cancelada', 'Renovada', 'Pendiente'));

ALTER TABLE socios
    ALTER COLUMN estado_acceso SET DEFAULT 'Inactivo';
