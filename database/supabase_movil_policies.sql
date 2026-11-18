-- ============================================================
--  POLÍTICAS RLS PARA LA APP MÓVIL (SysAlquileres APK) - CRUD
--
--  Ejecutar en el SQL Editor de Supabase.
--  Permite al rol anon: SELECT, INSERT, UPDATE y DELETE en los
--  módulos del sistema. Uso académico/demo.
--  En producción: Supabase Auth + políticas por usuario/rol.
-- ============================================================

GRANT USAGE ON SCHEMA public TO anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO anon;

-- ---------- Departamentos ----------
DROP POLICY IF EXISTS "movil_crud_departamento" ON public.departamento;
CREATE POLICY "movil_crud_departamento" ON public.departamento
    FOR ALL TO anon USING (true) WITH CHECK (true);

-- ---------- Inquilinos ----------
DROP POLICY IF EXISTS "movil_crud_inquilino" ON public.inquilino;
CREATE POLICY "movil_crud_inquilino" ON public.inquilino
    FOR ALL TO anon USING (true) WITH CHECK (true);

-- ---------- Contratos ----------
DROP POLICY IF EXISTS "movil_crud_contrato" ON public.contrato;
CREATE POLICY "movil_crud_contrato" ON public.contrato
    FOR ALL TO anon USING (true) WITH CHECK (true);

-- ---------- Pagos ----------
DROP POLICY IF EXISTS "movil_crud_pago" ON public.pago;
CREATE POLICY "movil_crud_pago" ON public.pago
    FOR ALL TO anon USING (true) WITH CHECK (true);

-- ---------- Catálogos y login (solo lectura) ----------
DROP POLICY IF EXISTS "movil_read_tipo_personeria" ON public.tipo_personeria;
CREATE POLICY "movil_read_tipo_personeria" ON public.tipo_personeria
    FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "movil_read_usuario" ON public.usuario;
CREATE POLICY "movil_read_usuario" ON public.usuario
    FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "movil_read_finalizacion" ON public.finalizacion_contrato;
CREATE POLICY "movil_read_finalizacion" ON public.finalizacion_contrato
    FOR SELECT TO anon USING (true);

-- Verificación:
-- SELECT tablename, policyname FROM pg_policies WHERE schemaname='public' ORDER BY tablename;
