-- ============================================================
--  POLÍTICAS RLS PARA LA APK "APOYO / SOLO LECTURA"
--  SysAlquileres · versión con roles ADMIN y CLIENTE
--
--  Ejecutar en el SQL Editor de Supabase.
--
--  La APK envía la cabecera x-dni con el usuario de sesión:
--    - admin  → ve los listados generales (solo lectura)
--    - DNI    → ve SOLAMENTE sus propios datos
--
--  El escritorio NO usa estas políticas: se conecta por JDBC.
-- ============================================================

-- Cabecera enviada por SupabaseClient.java
CREATE OR REPLACE FUNCTION public.movil_dni()
RETURNS text LANGUAGE sql STABLE AS $$
  SELECT NULLIF(current_setting('request.headers', true)::json ->> 'x-dni', '');
$$;

GRANT USAGE ON SCHEMA public TO anon;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO anon;

-- La APK ya no crea, edita ni elimina datos.
REVOKE INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public FROM anon;

-- ---------- Quitar políticas CRUD antiguas ----------
DROP POLICY IF EXISTS "movil_crud_departamento" ON public.departamento;
DROP POLICY IF EXISTS "movil_crud_inquilino" ON public.inquilino;
DROP POLICY IF EXISTS "movil_crud_contrato" ON public.contrato;
DROP POLICY IF EXISTS "movil_crud_pago" ON public.pago;

-- ---------- Login: se permite leer usuario para validar credenciales ----------
DROP POLICY IF EXISTS "movil_read_usuario" ON public.usuario;
CREATE POLICY "movil_read_usuario" ON public.usuario
    FOR SELECT TO anon USING (true);

-- ---------- Catálogos y departamentos: lectura general ----------
DROP POLICY IF EXISTS "movil_read_departamento" ON public.departamento;
CREATE POLICY "movil_read_departamento" ON public.departamento
    FOR SELECT TO anon USING (true);

DROP POLICY IF EXISTS "movil_read_tipo_personeria" ON public.tipo_personeria;
CREATE POLICY "movil_read_tipo_personeria" ON public.tipo_personeria
    FOR SELECT TO anon USING (true);

-- ---------- Inquilino: admin ve todos; cliente solo el suyo ----------
DROP POLICY IF EXISTS "movil_read_inquilino" ON public.inquilino;
CREATE POLICY "movil_read_inquilino" ON public.inquilino
    FOR SELECT TO anon USING (
        public.movil_dni() = 'admin' OR dni = public.movil_dni()
    );

-- ---------- Contrato: admin todos; cliente solo los suyos ----------
DROP POLICY IF EXISTS "movil_read_contrato" ON public.contrato;
CREATE POLICY "movil_read_contrato" ON public.contrato
    FOR SELECT TO anon USING (
        public.movil_dni() = 'admin'
        OR idinquilino IN (
            SELECT idinquilino FROM public.inquilino WHERE dni = public.movil_dni()
        )
    );

-- ---------- Pago: admin todos; cliente solo los de sus contratos ----------
DROP POLICY IF EXISTS "movil_read_pago" ON public.pago;
CREATE POLICY "movil_read_pago" ON public.pago
    FOR SELECT TO anon USING (
        public.movil_dni() = 'admin'
        OR idcontrato IN (
            SELECT c.idcontrato FROM public.contrato c
            JOIN public.inquilino i ON i.idinquilino = c.idinquilino
            WHERE i.dni = public.movil_dni()
        )
    );

-- ---------- Finalizaciones: admin todas; cliente solo las suyas ----------
DROP POLICY IF EXISTS "movil_read_finalizacion" ON public.finalizacion_contrato;
CREATE POLICY "movil_read_finalizacion" ON public.finalizacion_contrato
    FOR SELECT TO anon USING (
        public.movil_dni() = 'admin'
        OR idcontrato IN (
            SELECT c.idcontrato FROM public.contrato c
            JOIN public.inquilino i ON i.idinquilino = c.idinquilino
            WHERE i.dni = public.movil_dni()
        )
    );

-- Verificación:
-- SELECT tablename, policyname FROM pg_policies
-- WHERE schemaname='public' ORDER BY tablename, policyname;
