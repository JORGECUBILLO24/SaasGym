-- Catálogo inicial de productos para venta en recepción y en la tienda de la app.
-- Se crea en cada sucursal existente; precios en córdobas (NIO). Ajusta precios y stock
-- desde Administración > Inventario antes de usarlo en producción.
-- Idempotente: no duplica un producto que ya exista con el mismo nombre en la sucursal.

WITH catalogo (nombre, categoria, precio, stock) AS (
    VALUES
    -- Suplementos
    ('Proteína whey 2 lb, chocolate',            'Suplementos', 1450.00, 12),
    ('Proteína whey 2 lb, vainilla',             'Suplementos', 1450.00, 10),
    ('Proteína whey 5 lb, chocolate',            'Suplementos', 3200.00,  6),
    ('Proteína isolatada 2 lb',                  'Suplementos', 1950.00,  6),
    ('Creatina monohidratada 300 g',             'Suplementos',  950.00, 15),
    ('Pre-entreno 30 servicios',                 'Suplementos', 1100.00,  8),
    ('BCAA 30 servicios',                        'Suplementos',  850.00,  8),
    ('Ganador de peso 6 lb',                     'Suplementos', 2300.00,  5),
    ('Multivitamínico 60 tabletas',              'Suplementos',  550.00, 10),
    ('Omega 3, 90 cápsulas',                     'Suplementos',  480.00, 10),
    ('Proteína whey, sobre individual',          'Suplementos',   75.00, 40),
    -- Bebidas / snacks
    ('Agua purificada 600 ml',                   'Bebidas',       25.00, 60),
    ('Agua purificada 1 L',                      'Bebidas',       35.00, 40),
    ('Bebida hidratante 600 ml',                 'Bebidas',       45.00, 36),
    ('Bebida energizante 473 ml',                'Bebidas',       60.00, 24),
    ('Batido de proteína listo para tomar',      'Bebidas',      110.00, 18),
    ('Barra de proteína',                        'Bebidas',       70.00, 30),
    ('Barra de granola',                         'Bebidas',       30.00, 30),
    ('Café americano',                           'Bebidas',       40.00, 50),
    -- Accesorios
    ('Shaker 600 ml',                            'Accesorios',   180.00, 20),
    ('Guantes de entrenamiento',                 'Accesorios',   450.00, 10),
    ('Cinturón de levantamiento',                'Accesorios',   950.00,  6),
    ('Muñequeras',                               'Accesorios',   250.00, 12),
    ('Straps de levantamiento',                  'Accesorios',   280.00, 10),
    ('Toalla de microfibra',                     'Accesorios',   220.00, 15),
    ('Cuerda para saltar',                       'Accesorios',   300.00,  8),
    ('Bandas elásticas, juego de 5',             'Accesorios',   550.00,  8),
    ('Candado para casillero',                   'Accesorios',   150.00, 15),
    ('Magnesio en polvo',                        'Accesorios',   200.00, 10),
    -- Mercancía
    ('Camiseta del gimnasio',                    'Mercancía',    400.00, 20),
    ('Camiseta sin mangas del gimnasio',         'Mercancía',    380.00, 15),
    ('Gorra del gimnasio',                       'Mercancía',    350.00, 10),
    ('Botella térmica del gimnasio 750 ml',      'Mercancía',    450.00, 10)
)
INSERT INTO productos_inventario (nombre, categoria, precio, stock, sucursal_id)
SELECT c.nombre, c.categoria, c.precio, c.stock, s.id
FROM catalogo c
CROSS JOIN sucursales s
WHERE NOT EXISTS (
    SELECT 1 FROM productos_inventario p
    WHERE p.sucursal_id = s.id AND lower(p.nombre) = lower(c.nombre)
);
