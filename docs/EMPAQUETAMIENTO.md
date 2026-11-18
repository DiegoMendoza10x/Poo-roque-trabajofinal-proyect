# Empaquetamiento del ejecutable `.exe`

## 1. Objetivo

Entregar un programa de Windows que **se abra con doble clic y no requiera Java instalado**.
Se logra con `jpackage` (incluido en JDK 21), que genera una *imagen de aplicación*: ejecutable + dependencias + un JRE embebido.

## 2. Herramientas

| Herramienta | Versión |
|---|---|
| JDK con `jpackage` | 21.0.7 |
| Maven | 3.9.16 |
| Script | `scripts/build-exe.ps1` |

## 3. Cómo generarlo

```powershell
cd "D:\CRUD DEPARTAMENTOS\SysAlquileres"
powershell -ExecutionPolicy Bypass -File scripts\build-exe.ps1
```

El script:

1. Compila con `mvn package`.
2. Copia el JAR y las dependencias a `target\app`.
3. Ejecuta `jpackage --type app-image` indicando:
   - `--main-jar SysAlquileres-1.0.0.jar`
   - `--main-class pe.edu.upeu.alquileres.App`
   - módulos: `java.base`, `java.desktop`, `java.sql`, `java.naming`, `java.logging`, `java.management`, `java.xml`, `java.prefs`, `java.scripting`, `jdk.unsupported`, `jdk.crypto.ec`, `jdk.crypto.cryptoki`.
   - `--icon icono\icono.ico` (icono del ejecutable, multi-tamaño 16–256 px).
4. Elimina la carpeta `data` de prueba y comprime en `dist\SysAlquileres-v2.0.0-portable.zip`.

El archivo maestro se genera con `python scripts\prepare_icon.py` a partir de la imagen en `icono/`;
ese script recorta el fondo blanco, genera el `.ico`, los mipmaps de Android y
`src/main/resources/images/app-icon.png` (icono de la ventana JavaFX y del login).

### ¿Por qué esos módulos extra?

| Módulo | Razón |
|---|---|
| `java.scripting` | FXML lo usa internamente; sin él el arranque falla con `NoClassDefFoundError: javax/script/Bindings` |
| `jdk.crypto.ec`, `jdk.crypto.cryptoki` | Proveedores criptográficos necesarios para el handshake TLS con Supabase (`insufficient_security`) |
| `java.desktop` | Impresión (`javax.print`) y AWT |

## 4. Resultado

```
dist/
├── SysAlquileres/
│   ├── SysAlquileres.exe      ← ejecutable
│   ├── app/                   ← JAR de la app y dependencias
│   └── runtime/               ← JRE embebido
└── SysAlquileres-v2.0.0-portable.zip   (~60 MB)
```

**Verificación realizada:** se ejecutó el `.exe` y se comprobó que abre la ventana *"SysAlquileres · Iniciar sesión"*, tanto con H2 local como conectado a Supabase.

## 5. Forma de entrega

Se entrega el **ZIP**: el `.exe` suelto no funciona porque necesita `app\` y `runtime\` al lado.
El ZIP está en la raíz del repositorio (`dist/`) y se descarga desde GitHub.

## 6. Limitaciones

- Es una aplicación **portable**, no un instalador.
- Para crear un instalador `.exe`/`.msi` con `jpackage --type exe` se necesita **WiX Toolset 3.x** (no instalado en el equipo de desarrollo).
- GitHub avisa porque el ZIP supera los 50 MB recomendados; el límite real es 100 MB y la subida se completó.

## 7. Solución de problemas

| Síntoma | Causa / solución |
|---|---|
| `Exception in Application start method` + `javax/script/Bindings` | Falta `java.scripting` en `--add-modules` |
| `SSLHandshakeException: insufficient_security` | Faltan `jdk.crypto.ec` y `jdk.crypto.cryptoki` |
| "No se pudo conectar con la base de datos" | Sin Supabase se usa H2; con Supabase requiere internet y variables de entorno |
| El `.exe` no abre | Descomprimir el ZIP completo; no separar las carpetas |
