# Migraciones de base de datos (Flyway)

La API aplica sola los cambios de base de datos al arrancar. Ya no se corre SQL a mano en Neon.

## Dónde están

`SaasGymAPI/src/main/resources/db/migration/`

| Archivo | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Esquema completo (antes `gimnasio_schema.sql`). |
| `V2__biblioteca_ejercicios.sql` | Biblioteca de 536 ejercicios con GIF. |
| `V3__seguridad_transferencias.sql` | Sesiones (`auth_version`), ajustes de stock, reservas y vencimiento de pedidos, cuentas bancarias y pagos por transferencia. |

Flyway guarda en la tabla `flyway_schema_history` qué versiones ya aplicó. Al arrancar, corre en orden solo las que falten. Si una falla, la API no arranca y Render mantiene la versión anterior.

## Cómo hacer un cambio de base

1. Crea un archivo **nuevo** con el número siguiente: `V3__descripcion_corta.sql` (dos guiones bajos).
2. Escribe ahí el SQL (`ALTER TABLE ...`, `CREATE TABLE ...`, `INSERT ...`).
3. Ajusta las entidades Java que correspondan.
4. Abre el PR. La CI levanta un PostgreSQL vacío y corre **todas** las migraciones: si el SQL tiene un error, el PR sale en rojo.
5. Al desplegar, Render arranca la API y Flyway aplica el `V3` en producción. No hay que hacer nada más.

## Reglas

- **Nunca edites una migración que ya está en `main`.** Flyway guarda una huella de cada archivo; si cambia, la API se niega a arrancar. Para corregir algo, crea una migración nueva.
- **Una migración por PR**, con número único. Si dos PRs usan el mismo número, el segundo en entrar se renumera antes de mergear.
- **No metas datos de ejemplo** (productos, socios de prueba) en migraciones: llegarían a cada gimnasio nuevo. Los datos de ejemplo van en scripts aparte para entornos locales.
- Los cambios grandes o riesgosos (borrar columnas, cambiar tipos) se hacen en dos pasos: primero agregar lo nuevo, después de desplegar quitar lo viejo.

## Bases que ya existían

- **Producción (Neon):** ya tenía aplicados V1 y V2 a mano. La primera vez que arranque con Flyway, la marca como versión 2 (`baseline`) **sin ejecutar nada** y desde ahí aplica V3 en adelante.
- **Tu base local:** si está al día con producción, funciona igual. Si está atrasada, lo más simple es borrarla y crear una vacía: Flyway la arma completa al arrancar.

## Desactivarlo

`FLYWAY_ENABLED=false` lo apaga (solo para emergencias). Las pruebas sin base real lo tienen apagado; las de integración con PostgreSQL lo usan para armar su base.

## Carpeta `SaasGymAPI/migrations/`

Histórico de los scripts que se corrían a mano hasta el 19-nov-2026. Ya están incluidos en V1 y V2; **no agregues archivos nuevos ahí**.
