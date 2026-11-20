-- ==========================================
-- Seguridad (nov 2026): sesiones, stock, pedidos y pagos por transferencia. Idempotente.
-- ==========================================

-- Cambiar o restablecer la contraseña cierra todas las sesiones abiertas (los JWT llevan esta versión).
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS auth_version INT NOT NULL DEFAULT 0;

-- Bitácora de ajustes manuales de stock: editar un producto ya no cambia su stock.
CREATE TABLE IF NOT EXISTS ajustes_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    producto_id UUID NOT NULL REFERENCES productos_inventario(id) ON DELETE CASCADE,
    cantidad INT NOT NULL CHECK (cantidad <> 0),
    stock_resultante INT NOT NULL CHECK (stock_resultante >= 0),
    motivo VARCHAR(200) NOT NULL,
    usuario VARCHAR(150) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ajustes_stock_producto_fecha
ON ajustes_stock (producto_id, fecha DESC);

-- Pedidos de la app: vencen si no se pagan, y guardan exactamente lo que apartaron del inventario.
ALTER TABLE ventas_productos
    ADD COLUMN IF NOT EXISTS fecha_expiracion TIMESTAMP;

CREATE TABLE IF NOT EXISTS reservas_ventas_productos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    venta_id UUID NOT NULL REFERENCES ventas_productos(id) ON DELETE CASCADE,
    producto_id UUID NOT NULL REFERENCES productos_inventario(id) ON DELETE CASCADE,
    cantidad INT NOT NULL CHECK (cantidad > 0)
);

CREATE INDEX IF NOT EXISTS idx_reservas_ventas_productos_venta
ON reservas_ventas_productos (venta_id);

CREATE INDEX IF NOT EXISTS idx_ventas_productos_pendientes
ON ventas_productos (estado, fecha_expiracion)
WHERE estado = 'Pendiente';

-- Cuentas del gimnasio para recibir transferencias (las administra el panel web).
CREATE TABLE IF NOT EXISTS cuentas_bancarias (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    banco VARCHAR(80) NOT NULL,
    tipo_cuenta VARCHAR(20) NOT NULL CHECK (tipo_cuenta IN ('Ahorro', 'Corriente')),
    numero_cuenta VARCHAR(40) NOT NULL,
    titular VARCHAR(120) NOT NULL,
    moneda VARCHAR(10) NOT NULL CHECK (moneda IN ('NIO', 'USD')),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Pago de membresía por transferencia que reporta el socio; se activa al aprobarlo recepción.
CREATE TABLE IF NOT EXISTS solicitudes_pago_membresia (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    socio_id UUID NOT NULL REFERENCES socios(usuario_id) ON DELETE CASCADE,
    tipo_membresia_id INT NOT NULL REFERENCES tipos_membresia(id),
    cuenta_bancaria_id UUID NOT NULL REFERENCES cuentas_bancarias(id),
    monto DECIMAL(10, 2) NOT NULL CHECK (monto >= 0),
    referencia VARCHAR(40) NOT NULL,
    comprobante_url TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente' CHECK (estado IN ('Pendiente', 'Aprobada', 'Rechazada')),
    motivo_rechazo VARCHAR(255),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_revision TIMESTAMP,
    revisado_por VARCHAR(150),
    pago_id UUID REFERENCES pagos(id)
);

-- Un solo pago en revisión por socio, y una referencia bancaria no se reutiliza.
CREATE UNIQUE INDEX IF NOT EXISTS ux_solicitud_pago_pendiente_socio
ON solicitudes_pago_membresia (socio_id)
WHERE estado = 'Pendiente';

CREATE UNIQUE INDEX IF NOT EXISTS ux_solicitud_pago_referencia
ON solicitudes_pago_membresia (cuenta_bancaria_id, UPPER(referencia))
WHERE estado <> 'Rechazada';

CREATE INDEX IF NOT EXISTS idx_solicitudes_pago_estado_fecha
ON solicitudes_pago_membresia (estado, fecha_creacion);
