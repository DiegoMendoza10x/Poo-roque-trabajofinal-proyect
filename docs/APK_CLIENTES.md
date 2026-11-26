# APK de apoyo: roles ADMIN / CLIENTE

## 1. Idea general

La APK ya **no es un CRUD móvil**. Ahora es una **bandeja de entrada informativa**:

- El **administrador** la usa para consultar (solo lectura) los módulos.
- El **cliente (inquilino)** entra con su propio usuario y ve **únicamente lo suyo**:
  departamento, titular, contrato, fecha de vencimiento e historial de pagos.

La escritura de datos sigue haciéndose en la **aplicación de escritorio**.

---

## 2. Cómo se crea el acceso del cliente

Cuando se guarda un **contrato nuevo** en el `.exe`:

```text
ContratoController.onGuardar()
   → ContratoServiceImp.save(contrato)
   → UsuarioServiceImp.crearCliente(contrato)
        · usuario = DNI del inquilino
        · clave   = 8 caracteres aleatorios (A-Z, 0-9)
        · rol     = CLIENTE
        · estado  = Activo
   → Dialogos.informar(...) muestra usuario y clave al administrador
```

El administrador debe entregar esos datos al inquilino.

- Si el inquilino ya tenía acceso, la clave se **renueva** y se muestra la nueva.
- Si el DNI pertenece a un administrador, el sistema lo impide.
- Editar un contrato **no** crea ni cambia usuarios; solo alta nueva.
- Al **finalizar** un contrato, el acceso del cliente se desactiva automáticamente.
- Al **crear otro contrato**, el acceso se reactiva automáticamente.

Archivos involucrados:

| Archivo | Cambio |
|---|---|
| `service/IUsuarioService.java` | Nuevo método `crearCliente(Contrato)`. |
| `service/impl/UsuarioServiceImp.java` | Genera la clave y create/renueva el usuario. |
| `repository/UsuarioRepository.java` | `findByUsuario`, `guardarCliente`, `actualizarCliente`. |
| `controller/ContratoController.java` | Muestra las credenciales después de guardar. |
| `config/AppContext.java` | Inyecta `IUsuarioService` en el controlador de contratos. |

> No hace falta agregar columnas: el usuario es el **DNI**, que ya existe en `inquilino`.

---

## 2.1 Recuperación de clave desde el escritorio

Si el cliente olvida su clave, el administrador abre el panel **Accesos clientes** en el `.exe`:

1. Barra lateral → **Accesos clientes**.
2. Buscar por DNI o nombre (o marcar **Mostrar claves** para verlas en la tabla).
3. Seleccionar al cliente.
4. Ver su clave actual o pulsar **Generar clave** para crear una nueva.
5. Pulsar **Guardar clave** y entregar la nueva clave al inquilino.
6. Si debe bloquearse el acceso, usar **Desactivar**; luego **Activar acceso** para restaurarlo.

El panel además es un **CRUD completo**: botón **Nuevo cliente** (DNI, nombre y clave) y **Eliminar** con confirmación. La desactivación por contrato finalizado y la reactivación por contrato nuevo son automáticas y no dependen de este panel.

> No hace falta la *secret key* de Supabase: el escritorio se conecta por JDBC con las variables `SUPABASE_DB_*` y actualiza directamente la tabla `usuario`. La clave se guarda en texto plano por ser un proyecto académico.

---

## 3. Login con validación de rol

`LoginActivity` consulta:

```text
/rest/v1/usuario?usuario=eq.<dni>&clave=eq.<clave>
                &estado=eq.Activo&select=usuario,nombre,rol&limit=1
```

Guarda en `SharedPreferences`: `nombre`, `rol` y `usuario` (el DNI).

Luego enruta:

| Rol | Actividad | Qué ve |
|---|---|---|
| `CLIENTE` | `ClienteActivity` | Solo sus datos. |
| `ADMIN` (cualquier otro) | `MainActivity` | Listados generales, solo lectura. |

---

## 4. Vista del cliente

`ClienteActivity` filtra por el DNI de sesión:

```text
# Titular
/rest/v1/inquilino?dni=eq.<dni>&select=dni,nombre,apellido,telefono,direccion&limit=1

# Contratos del titular
/rest/v1/contrato?inquilino.dni=eq.<dni>
  &select=idcontrato,fecha_inicio,fecha_fin,monto_mensual,estado,
          inquilino(dni),departamento(numero,piso,direccion)
  &order=fecha_inicio.desc

# Pagos de esos contratos
/rest/v1/pago?idcontrato=in.(<ids de sus contratos>)
  &select=idpago,fecha_pago,periodo,monto,metodo,descuento,igv
  &order=fecha_pago.desc
```

> **Nota técnica:** en PostgREST, para filtrar por una relación hay que incluirla en el `select` (`inquilino(dni)`). Para los pagos se usa un segundo paso con `idcontrato=in.(...)`, porque el filtro profundo `contrato.inquilino.dni` no filtra las filas padre. Esto fue probado contra la base real.

En pantalla se muestran:

- **Titular** y DNI.
- **Departamento**: número, piso y dirección.
- **Contrato**: número, inicio, vencimiento, monto mensual y estado.
- **Historial de pagos**: número de nota, fecha, total (`monto - descuento + IGV`) y método.
- **Último pago** y **próximo vencimiento**.

---

## 5. Vista del administrador (solo lectura)

`MainActivity` mantiene las cuatro pestañas (Departamentos, Inquilinos, Contratos y Pagos), pero:

- El botón **Nuevo** está oculto.
- Las opciones de **Editar** y **Eliminar** fueron reemplazadas por **Ver detalle**.
- En Pagos se conserva **Ver nota de venta** (compartir/imprimir por el sistema).

La cabecera indica `· solo lectura`.

---

## 6. Seguridad en la base (RLS)

`SupabaseClient` envía la cabecera `x-dni` con el usuario de sesión. El script
`database/supabase_movil_policies.sql` (actualizado):

- Quita las políticas CRUD anteriores y deja **solo SELECT**.
- Define `public.movil_dni()` para leer esa cabecera.
- `inquilino`, `contrato`, `pago` y `finalizacion_contrato` se filtran por DNI, salvo `admin`.
- `usuario` sigue legible para permitir el login.

```powershell
# Ejecutar una vez en el SQL Editor de Supabase:
# database/supabase_movil_policies.sql
```

Hasta que ese script se aplique, la base puede conservar las políticas permisivas antiguas; la APK **ya filtra por aplicación**, pero aplicar el script refuerza el aislamiento.

---

## 7. Compilación y entrega

```powershell
cd android
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"   # importante: gradle 8.11 + GraalVM 21 falla en jlink
.\gradlew.bat assembleRelease
```

Salida: `android\app\build\outputs\apk\release\app-release.apk`

Entregable copiado a: `dist\SysAlquileres-Movil-v1.1.apk`

Instalación:

```powershell
adb install -r "D:\CRUD DEPARTAMENTOS\SysAlquileres\dist\SysAlquileres-Movil-v1.1.apk"
```

---

## 8. Prueba recomendada

1. En el escritorio: crear un contrato nuevo para un inquilino.
2. Anotar el **usuario = DNI** y la **clave** que muestra el diálogo.
3. Abrir la APK e ingresar con esos datos.
4. Verificar que aparece el departamento y el contrato del inquilino.
5. Verificar que el historial de pagos contiene **solo** los pagos de ese contrato.
6. Ingresar con `admin / admin123`: debe abrir los listados generales, sin botones de escritura.

---

## 9. Limitaciones actuales

- La APK no crea usuarios ni contratos; todo se hace en el escritorio.
- La clave se renueva al crear un contrato nuevo para el mismo DNI (se muestra al admin).
- La nota de venta en la APK se **comparte** por Android; la impresión térmica directa sigue en el escritorio.
- Para producción conviene migrar a Supabase Auth en lugar de usuario/clave en texto plano.
