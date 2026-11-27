CREATE TABLE gimnasios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre_comercial VARCHAR(150) NOT NULL,
    correo VARCHAR(255) NOT NULL,
    telefono VARCHAR(30),
    slug VARCHAR(80) NOT NULL,
    fecha_alta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX ux_gimnasios_nombre_comercial_ci
    ON gimnasios (LOWER(BTRIM(nombre_comercial)));
CREATE UNIQUE INDEX ux_gimnasios_slug_ci
    ON gimnasios (LOWER(BTRIM(slug)));

INSERT INTO gimnasios (nombre_comercial, correo, telefono, slug)
VALUES ('Spartan Gym', 'admin@saasgym.com', NULL, 'spartan-gym');

DO $$
DECLARE
    tenant_table TEXT;
    spartan_gym_id UUID;
BEGIN
    SELECT id INTO STRICT spartan_gym_id
    FROM gimnasios
    WHERE slug = 'spartan-gym';

    FOREACH tenant_table IN ARRAY ARRAY[
        'usuarios',
        'socios',
        'personal',
        'sucursales',
        'tipos_membresia',
        'rutinas',
        'detalle_rutinas',
        'pagos',
        'membresias_socio',
        'asistencias',
        'controles_biometricos',
        'productos_inventario',
        'producto_componentes',
        'movimientos_financieros',
        'ventas_productos',
        'detalle_ventas_productos',
        'notificaciones',
        'password_reset_tokens',
        'configuracion_app',
        'ejercicios_completados',
        'ajustes_stock',
        'reservas_ventas_productos',
        'cuentas_bancarias',
        'solicitudes_pago_membresia'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN gimnasio_id UUID', tenant_table);
        EXECUTE format('UPDATE %I SET gimnasio_id = $1', tenant_table)
            USING spartan_gym_id;
        EXECUTE format('ALTER TABLE %I ALTER COLUMN gimnasio_id SET NOT NULL', tenant_table);
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (gimnasio_id) REFERENCES gimnasios(id)',
            tenant_table,
            'fk_' || tenant_table || '_gimnasio'
        );
        EXECUTE format('CREATE INDEX %I ON %I (gimnasio_id)',
            'idx_' || tenant_table || '_gimnasio_id',
            tenant_table
        );
    END LOOP;
END $$;

ALTER TABLE tipos_membresia
    DROP CONSTRAINT tipos_membresia_nombre_key;
ALTER TABLE tipos_membresia
    ADD CONSTRAINT uq_tipos_membresia_gimnasio_nombre UNIQUE (gimnasio_id, nombre);

ALTER TABLE configuracion_app
    DROP CONSTRAINT configuracion_app_pkey,
    DROP CONSTRAINT configuracion_app_id_check,
    ADD CONSTRAINT configuracion_app_pkey PRIMARY KEY (gimnasio_id, id);
