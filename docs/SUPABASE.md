# Conexión de SysAlquileres con Supabase

Guía paso a paso para dejar el sistema funcionando con **Supabase** (PostgreSQL en la nube).
La aplicación sigue funcionando con H2 de forma local; Supabase se activa solo cambiando la
configuración de la base de datos.

---

## 1. Crear el proyecto en Supabase

1. Entra a <https://supabase.com> y crea una cuenta (puedes usar GitHub).
2. Presiona **New project**.
3. Completa:

   | Campo | Valor sugerido |
   |---|---|
   | Organization | tu organización o cuenta personal |
   | Name | `sysalquileres` |
   | Database Password | una clave segura **(guárdala, la necesitarás)** |
   | Region | la más cercana (ej.: `South America (São Paulo)`) |
   | Plan | Free |

4. Espera 1–2 minutos hasta que el proyecto termine de aprovisionarse.

---

## 2. Crear las tablas

1. En el panel del proyecto abre **SQL Editor → New query**.
2. Copia y pega **todo** el contenido de:

   ```
   database/supabase_schema.sql
   ```

3. Presiona **Run**.
4. Verifica en **Table Editor** que aparezcan las tablas:
   `tipo_personeria`, `usuario`, `departamento`, `inquilino`, `contrato`, `pago`
   y `finalizacion_contrato`.

> El script ya incluye datos de ejemplo y reinicia los contadores de los IDs.
> Puedes ejecutarlo de nuevo cuando quieras: primero elimina las tablas y las vuelve a crear.

---

## 3. Obtener la cadena de conexión

1. Abre **Project Settings → Database**.
2. En **Connection string** elige la pestaña **Session pooler** (recomendada, funciona con IPv4).
3. Verás algo parecido a:

   ```
   postgresql://postgres.abcdefghijklmnop:TU_CLAVE@aws-0-sa-east-1.pooler.supabase.com:6543/postgres
   ```

4. Conviértela al formato **JDBC** que usa la aplicación:

   | Parte | Valor |
   |---|---|
   | URL | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres` |
   | Usuario | `postgres.abcdefghijklmnop` (incluye el punto y el ID del proyecto) |
   | Clave | la que definiste al crear el proyecto |

> El proyecto agrega automáticamente `?sslmode=require` y `prepareThreshold=0`
> cuando detecta una URL de Supabase, así que no hace falta escribirlos.

---

## 4. Configurar la aplicación

### Opción A — Variables de entorno (recomendada, no sube la clave a GitHub)

En PowerShell, antes de ejecutar:

```powershell
$env:SUPABASE_DB_URL      = "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres"
$env:SUPABASE_DB_USER     = "postgres.abcdefghijklmnop"
$env:SUPABASE_DB_PASSWORD = "TU_CLAVE"
$env:SUPABASE_DDL_AUTO    = "false"   # las tablas ya existen
$env:SUPABASE_SEED_DEMO   = "false"
mvn javafx:run
```

Para dejarlas permanentes en el equipo:

```powershell
setx SUPABASE_DB_URL      "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres"
setx SUPABASE_DB_USER     "postgres.abcdefghijklmnop"
setx SUPABASE_DB_PASSWORD "TU_CLAVE"
setx SUPABASE_DDL_AUTO    "false"
setx SUPABASE_SEED_DEMO   "false"
```

Cierra y vuelve a abrir la terminal después de usar `setx`.

### Opción B — Editar `application.properties`

Descomenta el bloque **PARA USAR SUPABASE** del archivo:

```
src/main/resources/application.properties
```

y completa `db.url`, `db.username` y `db.password`.
No olvides `db.ddl.auto=false` y `db.seed.demo=false`.

> ⚠️ Si usas esta opción, revisa que `application.properties` no se suba con la clave real.

---

## 5. Comprobar que funciona

Al iniciar la aplicación, en la consola debe aparecer:

```
INFO  p.e.u.a.config.DatabaseConfig - Base de datos lista: jdbc:postgresql://... [Supabase / PostgreSQL]
```

Luego inicia sesión con:

```
usuario: admin
clave:   admin123
```

Si puedes ver los departamentos, inquilinos, contratos y pagos de ejemplo, la conexión está lista.

Puedes confirmarlo también desde Supabase con una consulta en el **SQL Editor**:

```sql
SELECT * FROM vw_contrato_detalle ORDER BY idcontrato;
```

---

## 6. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Connection refused` o `timeout` | El host directo usa IPv6 y tu red no lo soporta | Usa la cadena **Session pooler** (puerto `6543`) |
| `password authentication failed` | Usuario incorrecto | En el pooler el usuario es `postgres.<project-ref>`, no solo `postgres` |
| `prepared statement "S_1" already exists` | Pooler en modo transacción | Agrega `prepareThreshold=0` a la URL (la app ya lo hace) |
| `no pg_hba.conf entry ... SSL off` | Falta cifrado | Agrega `sslmode=require` (la app ya lo hace) |
| `relation "departamento" does not exist` | No se ejecutó el script | Ejecuta `database/supabase_schema.sql` en el SQL Editor |
| La conexión tarda mucho después de días sin uso | Proyectos Free se pausan | Entra al panel de Supabase y presiona **Restore project** |

---

## 7. Notas para la exposición

- La arquitectura no cambió: repositorios → servicios → controladores.
- Solo cambia el motor de base de datos; el código Java es el mismo.
- Credenciales de prueba: **admin / admin123**.
- Las contraseñas se guardan en texto plano porque es un proyecto académico;
  en producción se debería usar un hash (BCrypt) y `Service Role`/RLS.
- Si no hay internet, la app sigue funcionando con la base H2 local sin tocar nada.
