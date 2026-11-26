# Entregables del proyecto final — SysAlquileres v2.0

| # | Requisito | Ubicación | Estado |
|---|---|---|---|
| 1 | **Manual de usuario** | [`docs/MANUAL_USUARIO.md`](docs/MANUAL_USUARIO.md) · [Versión HTML atractiva](docs/MANUAL_USUARIO.html) | ✅ |
| 2 | **Código fuente** | [`docs/CODIGO_FUENTE.md`](docs/CODIGO_FUENTE.md) · [`src/`](src/) · [`android/`](android/) | ✅ |
| 3 | **Empaquetamiento `.exe`** | [`docs/EMPAQUETAMIENTO.md`](docs/EMPAQUETAMIENTO.md) · [`dist/SysAlquileres-v2.0.0-portable.zip`](dist/SysAlquileres-v2.0.0-portable.zip) | ✅ |

## Documentación adicional

- [Guía completa de exposición y código](docs/GUIA_EXPOSICION.md)
- [Conexión con Supabase](docs/SUPABASE.md)
- [Impresión térmica 58 mm](docs/IMPRESION_58MM.md)
- [Políticas RLS para el APK](database/supabase_movil_policies.sql)

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
