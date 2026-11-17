-- Productos preparados con receta: al vender un batido se descuenta la proteína y la leche
-- que lleva. Los insumos se cuentan en su unidad (g, ml) y pueden no venderse al público.
-- Idempotente.

ALTER TABLE productos_inventario
    ADD COLUMN IF NOT EXISTS descripcion TEXT,
    ADD COLUMN IF NOT EXISTS unidad VARCHAR(20) NOT NULL DEFAULT 'unidad',
    ADD COLUMN IF NOT EXISTS vendible BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS producto_componentes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    producto_id UUID NOT NULL REFERENCES productos_inventario(id) ON DELETE CASCADE,
    insumo_id UUID NOT NULL REFERENCES productos_inventario(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    CONSTRAINT ux_producto_componente UNIQUE (producto_id, insumo_id),
    CONSTRAINT ck_producto_componente_distinto CHECK (producto_id <> insumo_id)
);

CREATE INDEX IF NOT EXISTS idx_producto_componentes_insumo ON producto_componentes (insumo_id);

-- Ejemplo en cada sucursal: insumos de la barra de batidos y dos batidos con receta.
WITH productos (nombre, categoria, precio, stock, unidad, vendible, descripcion) AS (
    VALUES
    ('Proteína whey a granel', 'Insumos', 0.00, 2270, 'g', FALSE,
     'Proteína para la barra de batidos. Se descuenta por gramos al vender un batido.'),
    ('Leche entera', 'Insumos', 0.00, 10000, 'ml', FALSE,
     'Leche para batidos. Se descuenta por mililitros.'),
    ('Banano', 'Insumos', 0.00, 24, 'unidad', FALSE, NULL),
    ('Batido de proteína con leche', 'Bebidas', 120.00, 0, 'unidad', TRUE,
     '30 g de proteína whey con 300 ml de leche entera.'),
    ('Batido de proteína con banano', 'Bebidas', 140.00, 0, 'unidad', TRUE,
     '30 g de proteína whey, 300 ml de leche entera y un banano.')
)
INSERT INTO productos_inventario (nombre, categoria, precio, stock, unidad, vendible, descripcion, sucursal_id)
SELECT p.nombre, p.categoria, p.precio, p.stock, p.unidad, p.vendible, p.descripcion, s.id
FROM productos p
CROSS JOIN sucursales s
WHERE NOT EXISTS (
    SELECT 1 FROM productos_inventario x
    WHERE x.sucursal_id = s.id AND lower(x.nombre) = lower(p.nombre)
);

WITH receta (producto, insumo, cantidad) AS (
    VALUES
    ('Batido de proteína con leche', 'Proteína whey a granel', 30),
    ('Batido de proteína con leche', 'Leche entera', 300),
    ('Batido de proteína con banano', 'Proteína whey a granel', 30),
    ('Batido de proteína con banano', 'Leche entera', 300),
    ('Batido de proteína con banano', 'Banano', 1)
)
INSERT INTO producto_componentes (producto_id, insumo_id, cantidad)
SELECT p.id, i.id, r.cantidad
FROM receta r
JOIN productos_inventario p ON lower(p.nombre) = lower(r.producto)
JOIN productos_inventario i ON lower(i.nombre) = lower(r.insumo) AND i.sucursal_id = p.sucursal_id
ON CONFLICT (producto_id, insumo_id) DO NOTHING;
