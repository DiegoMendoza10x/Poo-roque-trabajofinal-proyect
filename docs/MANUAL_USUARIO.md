# Manual de usuario — SysAlquileres v2.0

Sistema para administrar el alquiler de departamentos: **versión de escritorio (Windows)** y **versión móvil (Android)**.
Ambas comparten la misma base de datos (H2 local o Supabase en la nube).

---

## 1. Requisitos e instalación

### 1.1 Versión de escritorio (`.exe`)

1. Descarga `dist/SysAlquileres-v2.0.0-portable.zip` desde el repositorio.
2. Extrae el ZIP en una carpeta de tu elección (por ejemplo `Escritorio\SysAlquileres`).
3. Abre `SysAlquileres.exe`.
   - No necesitas instalar Java, Maven ni PostgreSQL.
   - La carpeta debe conservar `app\`, `runtime\` y el ejecutable juntos.

### 1.2 Versión móvil (APK)

1. Copia `dist/SysAlquileres-Movil-v1.1.apk` al celular.
2. Habilita *Instalar aplicaciones desconocidas* y abre el APK.
3. Concede permiso de red (solo se usa `INTERNET`).

> Credenciales de acceso: **usuario `admin` · clave `admin123`**.

---

## 2. Iniciar sesión

1. Al abrir el programa aparece la pantalla de acceso con el logo **SA**.
2. Escribe el usuario y la clave.
3. Pulsa **Ingresar** (también funciona la tecla *Enter*).
4. Si los datos son correctos se abre la ventana principal. Si no, aparece un mensaje rojo.
5. **Cerrar** sale de la aplicación.

En el APK, al iniciar sesión se recuerda la sesión; para cambiar de usuario usa **Salir** en la esquina superior derecha.

---

## 3. Pantalla principal (versión de escritorio)

La ventana tiene tres zonas:

| Zona | Contenido |
|---|---|
| **Barra lateral izquierda** | Botones: Departamentos, Inquilinos, Contratos, Pagos y Notas de venta, Acerca de y Cerrar sesión |
| **Cabecera** | Título del módulo, subtítulo y tarjeta del usuario (avatar con iniciales, nombre y rol) |
| **Área de trabajo** | Formulario a la izquierda y tabla a la derecha |

El botón activo se resalta en azul. Los módulos se cargan una sola vez: al volver a entrar se conservan los filtros.

---

## 4. Módulo Departamentos

**Para registrar uno:**
1. Clic en **Departamentos** (en la barra lateral) y luego **Nuevo**.
2. Completa: Número, Piso, Dirección y Precio (S/).
3. Clic en **Guardar**.

**Para editar:** haz clic sobre una fila de la tabla; los datos se cargan en el formulario. Cambia lo necesario y guarda.

**Para buscar:** escribe en el cuadro *Buscar* (por número o dirección). La búsqueda ignora mayúsculas y acentos.

**Para filtrar:** elige *Disponible* u *Ocupado* en el desplegable.

**Para eliminar:** selecciona una fila y pulsa **Eliminar**; confirma en el diálogo. El sistema no permite borrar departamentos que tengan contratos.

> El estado cambia automáticamente a **Ocupado** al crear un contrato y vuelve a **Disponible** al finalizarlo.

---

## 5. Módulo Inquilinos

1. **Nuevo** → completa DNI (8 dígitos), Nombre, Apellido, Teléfono y Dirección.
2. En **Personería** elige:
   - **Natural**: solo los datos básicos.
   - **Jurídica**: aparecen además *Rep. legal*, *RUC* (10 u 11 dígitos) y *Dir. fiscal*.
3. **Guardar**.

El DNI no se puede repetir. Al seleccionar una fila se carga el formulario para editar. El botón **Eliminar** está bloqueado si el inquilino tiene contratos.

---

## 6. Módulo Contratos

**Registrar:**
1. **Nuevo** → elige Inquilino y Departamento (solo aparecen los disponibles).
2. El monto se rellena automáticamente con el precio del departamento; puedes ajustarlo.
3. Indica Fecha inicio, Fecha fin y Cantidad de personas.
4. **Guardar**. El departamento pasa a **Ocupado**.

**Reglas que el sistema controla:**
- La fecha de fin debe ser posterior a la de inicio.
- Un departamento **no puede tener dos contratos vigentes con fechas que se crucen**; si intentas eso verás el mensaje *"El departamento ya tiene un contrato vigente que se cruza con esas fechas"*.
- No se pueden editar ni eliminar contratos **finalizados**, ni contratos que tengan pagos.

**Finalizar:**
1. Selecciona el contrato en la tabla.
2. Clic en **Finalizar**.
3. Escribe el motivo (por ejemplo *Fin de plazo*) y acepta.
4. El departamento vuelve a **Disponible**.

> Los contratos con monto alto ya **no se resaltan en amarillo**: todos se muestran con el mismo formato y el estado va en un chip azul (Vigente) o gris (Finalizado).

---

## 7. Módulo Pagos y nota de venta

**Registrar un pago:**
1. Clic en **Pagos y notas de venta** → **Nuevo**.
2. Elige el **Contrato**; el monto se rellena con la renta mensual.
3. Revisa Fecha de pago y Periodo (se normaliza al primer día del mes).
4. Elige el método (Efectivo, Transferencia, Yape, Plin o Tarjeta) y, si aplica, descuento e IGV.
5. **Guardar**. El sistema pregunta si deseas **Imprimir nota** o **Ahora no**: el pago ya quedó guardado en ambos casos.

**Buscar/filtrar:** por inquilino, departamento o periodo; el desplegable filtra por método de pago.

**Emitir la nota de venta:**
1. Selecciona un pago en la tabla y clic en **Nota de venta** (o acepta la pregunta al guardar).
2. Revisa la vista previa del ticket: **58 mm / 32 columnas** u **80 mm / 48 columnas**.
3. Elige la forma de impresión:
   - **Cable / Windows:** selecciona la impresora instalada (por ejemplo `POS-58-Series` o `POS-80-Series`).
   - **Bluetooth (COM):** elige el puerto y la velocidad (9600 por defecto).
4. Clic en **Imprimir nota**.
5. **Copiar texto** envía el ticket al portapapeles.

La nota incluye empresa, número `NV-00000X`, cliente, DNI, departamento, contrato, periodo, importes, total, método y el nombre de quien atiende.

> La nota de venta **no es comprobante fiscal**: no reemplaza a la boleta ni a la factura de SUNAT.

---

## 7.1 Configuración de impresión

En la barra lateral hay un botón **Config. impresión**. Desde ese panel se puede:

- Elegir la conexión: **Cable / Windows** o **Bluetooth / puerto COM**.
- Elegir el papel: **58 mm (32 columnas)** u **80 mm (48 columnas)**.
- Seleccionar la impresora de Windows, el puerto COM y los baudios.
- **Guardar configuración**: se persiste en `%USERPROFILE%\.SysAlquileres\impresion.properties`.
- **Imprimir prueba**: envía un ticket corto para verificar la impresora.
- **Actualizar listas**: vuelve a buscar impresoras y puertos.

La ventana de nota de venta abre con esos valores ya seleccionados. El panel muestra además la base de datos activa y la ruta del archivo de configuración.

---

## 8. Cerrar sesión y salir

- **Cerrar sesión**: botón en la barra lateral → confirmar → vuelve al login.
- **Salir**: desde el login, botón **Cerrar**; cierra la aplicación y el pool de conexiones.

---

## 9. Versión móvil (APK de apoyo · v1.1)

La APK es de **solo lectura**. El login valida el **rol** de la tabla `usuario`:

| Rol | Qué abre | Qué ve |
|---|---|---|
| **CLIENTE** | pantalla *Mi alquiler* | Departamento, contrato, vencimiento e historial de pagos **solo suyos** |
| **ADMIN** | listados generales | Los 4 módulos para consultar, sin botones de escritura |

**Acceso del cliente:** al crear un contrato en el escritorio se genera su usuario y clave:

- Usuario = **DNI del inquilino**.
- Clave = 8 caracteres aleatorios que el sistema muestra al administrador.
- Entrega esos datos al inquilino para que ingrese en la APK.

| Acción | Cómo hacerlo |
|---|---|
| Actualizar datos | Botón **Actualizar mis datos** (cliente) o **Actualizar** (admin) |
| Ver detalle | Toca una fila → **Ver detalle** (solo lectura) |
| Ver nota | En Pagos, toca una fila → **Ver nota de venta** → **Compartir / imprimir** |
| Cerrar sesión | Botón **Cerrar sesión** |

El archivo es `dist/SysAlquileres-Movil-v1.1.apk`. Guía completa: `docs/APK_CLIENTES.md`.

---

## 9.1 Recuperación de clave del cliente (escritorio)

Si el inquilino olvida su clave:

1. En el `.exe`, abre **Accesos clientes** en la barra lateral.
2. Busca por DNI o nombre y selecciona al cliente.
3. Marca **Mostrar claves** si quieres ver la clave actual en la tabla.
4. Pulsa **Generar clave** y luego **Guardar clave** para asignar una nueva.
5. Entrega la nueva clave al inquilino para que ingrese en la APK.

También puedes **Activar** o **Desactivar** el acceso, dar de alta con **Nuevo cliente** y borrar con **Eliminar** (confirmación): el panel es un CRUD completo. No se necesita la *secret key* de Supabase: el escritorio actualiza la tabla `usuario` por JDBC.

**Automático:** al finalizar un contrato la clave se desactiva; al crear un contrato nuevo se reactiva.

---

## 10. Base de datos (información para el usuario)

- **Base activa en este entorno: Supabase / PostgreSQL**, porque las variables `SUPABASE_DB_*` están configuradas y tienen prioridad. `application.properties` trae H2 como valor por defecto.
- Prioridad: `application.properties` → propiedades `-D` → variables `DB_*` → variables `SUPABASE_DB_*` (máxima).
- La URL de Supabase se ajusta sola con `sslmode=require` y `prepareThreshold=0`.
- Para volver a H2: cierra la app, quita las variables `SUPABASE_DB_*` y `DB_*`, y ejecuta de nuevo. La base local se crea en `~/SysAlquileres/data/alquileres.mv.db`.
- Para reiniciar la base local: cierra la app, elimina la carpeta `~/SysAlquileres/data` y vuelve a abrir. Se recargan los datos de ejemplo.
- Datos de ejemplo: 6 departamentos, 5 inquilinos, 5 contratos y 8 pagos.

---

## 11. Solución de problemas

| Situación | Qué hacer |
|---|---|
| "Usuario o clave incorrectos" | Verifica `admin / admin123` |
| El `.exe` no abre | Asegúrate de tener juntos `app\`, `runtime\` y el `.exe` |
| "No se pudo conectar con la base de datos" | Si usas Supabase, revisa internet y las variables de entorno; sin ellas se usa H2 |
| No aparece la impresora | Instálala en Windows y vuelve a abrir el diálogo de nota |
| Error `prepared statement already exists` | La URL de Supabase debe incluir `prepareThreshold=0` (la app lo agrega sola) |
| El APK no lista datos | Revisa internet y que estén aplicadas las políticas RLS (`database/supabase_movil_policies.sql`) |

---

## 12. Modelo Power Architect

Dentro del proyecto:

- `database/modelo-power-architect.architect`: modelo **actualizado** con 7 tablas (incluida `usuario`).
- `database/modelo-power-architect-original.architect`: modelo original entregado en REQUERIMIENTOS.

Cambios del actualizado: tabla `usuario` agregada para el login; `descuento` e `igv` corregidos a `numeric(10,2)`; RUC de 11 caracteres; `id_finalizacion` como BIGINT; índices `idx_contrato_departamento`, `idx_contrato_inquilino` e `idx_pago_contrato`.

---

## 13. Manual HTML y PDF

El manual completo en formato navegable está en `docs/MANUAL_USUARIO.html`. Incluye índice lateral, capturas reales, tablas, preguntas frecuentes y un botón **Descargar en PDF**: al pulsarlo se abre el diálogo de impresión del navegador; elige **Guardar como PDF** como destino.

---

*Manual actualizado para la versión 2.0 (interfaz rediseñada, notas de venta 58/80 mm, panel de impresión, Supabase activo, `.exe`, APK y modelo Architect actualizado).*
