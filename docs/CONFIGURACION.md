# Configuración del sistema: impresión y base de datos

## 1. Panel de configuración de impresión

Está en la barra lateral de la ventana principal, botón **“Config. impresión”**.

Permite definir:

| Campo | Qué hace |
|---|---|
| **Conexión** | *Cable / Windows (controlador)* o *Bluetooth / puerto COM*. |
| **Papel** | 58 mm (32 columnas) o 80 mm (48 columnas). |
| **Impresora de Windows** | Impresora instalada, por ejemplo `POS-58-Series` o `POS-80-Series`. |
| **Puerto COM / Bluetooth** | Puerto serie creado al emparejar la impresora Bluetooth (SPP). |
| **Velocidad (baudios)** | 9600 por defecto; también 19200, 38400, 57600 y 115200. |

### Botones

- **Guardar configuración**: persiste los valores.
- **Imprimir prueba**: envía un ticket corto (`PRUEBA DE IMPRESION`) con el ancho elegido.
- **Actualizar listas**: vuelve a buscar impresoras y puertos COM (útil si conectaste la impresora después de abrir el sistema).

### Dónde se guarda

```text
%USERPROFILE%\.SysAlquileres\impresion.properties
```

Ejemplo:

```properties
papel=58
usar.windows=true
impresora=POS-58-Series
puerto.com=
baudios=9600
```

La clase encargada es `config/ConfigImpresion.java` (Singleton). Al abrir la ventana de **Nota de venta**, los campos aparecen con estos valores ya seleccionados. Después de imprimir, la configuración usada se guarda sola.

> Si antes “no salía nada” al querer configurar, ahora hay un panel propio con listas, guardado y prueba de impresión.

---

## 2. ¿Qué base de datos se está usando?

El sistema tiene **dos modos**:

### Por defecto (archivo `application.properties`)

```properties
db.url=jdbc:h2:file:./data/alquileres;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
db.username=sa
db.password=
db.ddl.auto=true
db.seed.demo=true
```

→ **H2 embebida local**. No requiere instalar nada y crea la carpeta `data/`.

### Prioridad de configuración en `DatabaseConfig.java`

1. `application.properties`
2. Propiedades del sistema (`-Ddb.url=...`)
3. Variables de entorno genéricas (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, ...)
4. **Variables de entorno de Supabase** (`SUPABASE_DB_*`) → máxima prioridad

### Estado real de este entorno

Hay variables `SUPABASE_DB_*` configuradas, por lo que **la aplicación está usando Supabase / PostgreSQL**, no H2:

> **H2 inicial vs nube:** la versión entregada arranca con H2 embebida (gratuita, automática, sin internet). La opción cloud con Supabase permite compartir datos entre PCs y con la APK, pero implica un **costo adicional** de hosting y requiere variables de entorno.

```text
SUPABASE_DB_URL = jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres
SUPABASE_DDL_AUTO = false
SUPABASE_SEED_DEMO = false
```

`DatabaseConfig` agrega automáticamente `sslmode=require` y `prepareThreshold=0` porque es el pooler de Supabase. La contraseña se lee de la variable de entorno: **no está en el código ni en el repositorio**.

El panel **Config. impresión** muestra en vivo:

```text
Base de datos activa: Supabase / PostgreSQL
jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0
```

### Cómo volver a H2 local

1. Cerrar la aplicación.
2. Quitar o desactivar las variables `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`, `SUPABASE_DDL_AUTO` y `SUPABASE_SEED_DEMO` (y también `DB_*` si existen).
3. Volver a ejecutar `mvn javafx:run`.

También se puede forzar por terminal, por ejemplo:

```powershell
$env:DB_URL="jdbc:h2:file:./data/alquileres;MODE=PostgreSQL"
mvn javafx:run
```

---

## 3. Modelo Power Architect

Dentro del proyecto:

| Archivo | Descripción |
|---|---|
| `database/modelo-power-architect.architect` | **Modelo actualizado**: 7 tablas, incluida `usuario`. |
| `database/modelo-power-architect-original.architect` | Modelo original entregado en `REQUERIMIENTOS`. |

### Cambios del modelo actualizado

- Agregada la tabla **`usuario`** (`idusuario`, `usuario` único, `clave`, `nombre`, `rol`, `estado`), necesaria para el login.
- `pago.descuento` e `pago.igv`: corregidos de `varchar` a **`numeric(10,2)`**, igual que en `schema.sql`.
- `inquilino.ruc`: ampliado a **11 caracteres**.
- `finalizacion_contrato.id_finalizacion`: ajustado a **BIGINT (int8)**.
- Índices agregados: `idx_contrato_departamento`, `idx_contrato_inquilino` e `idx_pago_contrato`.
- La tabla `usuario` aparece también en el *play-pen* del modelo para verla en el diagrama.

El XML fue validado y abre con SQL Power Architect 1.0.9 o superior.

---

## 4. Prueba rápida recomendada

1. Entrar con **admin / admin123**.
2. Abrir **Config. impresión**.
3. Confirmar que el panel muestra la base activa y la ruta del archivo de configuración.
4. Elegir papel 58 mm, impresora `POS-58-Series`, guardar y presionar **Imprimir prueba**.
5. Cambiar a 80 mm, guardar, abrir **Pagos → Nota de venta** y verificar que el ticket abre en 48 columnas.
6. Para Bluetooth: emparejar la impresora, presionar **Actualizar listas**, elegir el puerto COM y volver a probar.

---

## 5. Panel de accesos de clientes

El panel **Accesos clientes** es un **CRUD completo**:

- **Nuevo cliente**: alta manual con DNI, nombre y clave.
- **Leer**: lista de `CLIENTE` con DNI, nombre, rol, estado y clave (oculta/mostrable).
- **Actualizar**: generar/guardar clave, activar o desactivar acceso.
- **Eliminar**: con confirmación.

No se necesita la *secret key* de Supabase: el escritorio actualiza `usuario` por JDBC.

**Regla automática:** al finalizar un contrato la clave se desactiva; al crear un contrato nuevo se reactiva.
