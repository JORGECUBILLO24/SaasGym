-- ==========================================
-- Marca (nov 2026): el correo de ejemplo de la configuración pasa a admin@saasgym.com.
-- Solo cambia la fila si todavía tiene el correo de ejemplo anterior (se reconoce por su
-- MD5); un correo que el administrador ya haya puesto no se toca. Idempotente.
-- ==========================================
DO $$
BEGIN
    UPDATE configuracion_app
    SET configuracion_json = jsonb_set(configuracion_json::jsonb, '{email}', '"admin@saasgym.com"')::text,
        fecha_actualizacion = CURRENT_TIMESTAMP
    WHERE md5(configuracion_json::jsonb ->> 'email') = '5f515a6d06ba9b40028eb37ba4df6a3d';
EXCEPTION WHEN others THEN
    -- Una configuración con JSON inválido no debe impedir que la API arranque.
    RAISE NOTICE 'No se actualizó el correo de ejemplo: %', SQLERRM;
END
$$;
