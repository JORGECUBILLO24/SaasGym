-- Pedidos de productos desde la app: el socio arma su carrito, elige efectivo o transferencia
-- y recibe un código; recepción, administración o el entrenador lo validan para completar la
-- compra. Idempotente.

ALTER TABLE ventas_productos
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'Completada',
    ADD COLUMN IF NOT EXISTS codigo_pedido VARCHAR(12),
    ADD COLUMN IF NOT EXISTS referencia_pago VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_validacion TIMESTAMP,
    ADD COLUMN IF NOT EXISTS validado_por VARCHAR(150);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ventas_productos_codigo_pedido ON ventas_productos (codigo_pedido);

-- Compras de la app anteriores a este cambio: quedaron sin cobrar en recepción.
UPDATE ventas_productos SET estado = 'Pendiente'
WHERE metodo_pago = 'Pendiente en recepción' AND estado = 'Completada';
