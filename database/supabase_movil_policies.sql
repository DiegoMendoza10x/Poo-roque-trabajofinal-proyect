-- ============================================================
--  POLÍTICAS RLS PARA LA APP MÓVIL (SysAlquileres APK)
--
--  Se ejecutan en el proyecto de Supabase (SQL Editor).
--  Permiten al rol anon:
--    - LEER todas las tablas del sistema (para los listados y el login)
--    - INSERTAR pagos (para el registro desde el celular)
--
--  Uso académico/demo. En producción se debe autenticar al usuario
--  con Supabase Auth y limitar las políticas por usuario/rol.
-- ============================================================

-- Asegurar permisos básicos de la API
GRANT USAGE ON SCHEMA public TO anon;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO anon;
GRANT INSERT ON public.pago TO anon;

-- Departamentos
DROP POLICY IF EXISTS "movil_select_departamento" ON public.departamento;
CREATE POLICY "movil_select_departamento" ON public.departamento
    FOR SELECT TO anon USING (true);

-- Inquilinos
DROP POLICY IF EXISTS "movil_select_inquilino" ON public.inquilino;
CREATE POLICY "movil_select_inquilino" ON public.inquilino
    FOR SELECT TO anon USING (true);

-- Contratos
DROP POLICY IF EXISTS "movil_select_contrato" ON public.contrato;
CREATE POLICY "movil_select_contrato" ON public.contrato
    FOR SELECT TO anon USING (true);

-- Pagos
DROP POLICY IF EXISTS "movil_select_pago" ON public.pago;
CREATE POLICY "movil_select_pago" ON public.pago
    FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "movil_insert_pago" ON public.pago;
CREATE POLICY "movil_insert_pago" ON public.pago
    FOR INSERT TO anon WITH CHECK (true);

-- Usuario (login de demostración)
DROP POLICY IF EXISTS "movil_select_usuario" ON public.usuario;
CREATE POLICY "movil_select_usuario" ON public.usuario
    FOR SELECT TO anon USING (true);

-- Catálogos auxiliares
DROP POLICY IF EXISTS "movil_select_tipo_personeria" ON public.tipo_personeria;
CREATE POLICY "movil_select_tipo_personeria" ON public.tipo_personeria
    FOR SELECT TO anon USING (true);
DROP POLICY IF EXISTS "movil_select_finalizacion" ON public.finalizacion_contrato;
CREATE POLICY "movil_select_finalizacion" ON public.finalizacion_contrato
    FOR SELECT TO anon USING (true);

-- Verificación
-- SELECT tablename, policyname FROM pg_policies WHERE schemaname='public' ORDER BY tablename;
