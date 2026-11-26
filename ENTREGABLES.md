# Entregables del proyecto final — SysAlquileres v2.0

| # | Requisito del docente | Ubicación | Estado |
|---|---|---|---|
| 1 | **Código fuente completo** en GitHub | [`src/`](src/) · [`android/`](android/) · [`docs/CODIGO_FUENTE.html`](docs/CODIGO_FUENTE.html) | ✅ |
| 2 | **Manual de usuario didáctico** por roles/perfiles | [`docs/MANUAL_USUARIO.html`](docs/MANUAL_USUARIO.html) · [MD](docs/MANUAL_USUARIO.md) | ✅ |
| 3 | **Empaquetado para Windows** | [`dist/SysAlquileres-v2.0.0-portable.zip`](dist/SysAlquileres-v2.0.0-portable.zip) · [`docs/EMPAQUETAMIENTO.html`](docs/EMPAQUETAMIENTO.html) | ✅ |

## Material de apoyo

- [Modelo Power Architect actualizado](modelo-power-architect/SysAlquileres-PowerArchitect.architect) · [cómo abrirlo](modelo-power-architect/README.md)
- [Guía de exposición interactiva](docs/GUIA_EXPOSICION.html)
- [Presentación de 23 diapositivas](docs/PRESENTACION.html)
- [Conexión con Supabase](docs/SUPABASE.html) · [Impresión térmica](docs/IMPRESION_58MM.html)
- [Políticas RLS para el APK](database/supabase_movil_policies.sql)

> Repositorio: `https://github.com/Yorlxx/SysAlquileres.git`. Si está privado, agregar al usuario **`dmanipar`** como colaborador desde *Settings → Collaborators*.

> **Base de datos:** la versión entregada usa **H2 embebida** (gratuita, se crea sola, sin internet). La opción en la nube con **Supabase/PostgreSQL** permite compartir datos entre PCs y con la APK, pero implica un **costo adicional** de hosting y requiere variables de entorno. En un software comercial se usaría una única base centralizada con secretos gestionados y autenticación real.

## Entregables binarios

| Archivo | Descripción |
|---|---|
| `dist/SysAlquileres-v2.0.0-portable.zip` | Programa de Windows con Java incluido |
| `dist/SysAlquileres-Movil-v1.1.apk` | Versión Android de apoyo: roles ADMIN/CLIENTE y solo lectura |

## Credenciales de prueba

```
usuario: admin
clave:   admin123
```

> Las credenciales reales de Supabase **no** están en el repositorio: se configuran mediante variables de entorno.
