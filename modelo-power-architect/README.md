# Modelo de datos · SQL Power Architect

 Esta carpeta contiene el modelo del proyecto abierto con **SQL Power Architect 1.0.9 o superior**.

## Archivos

| Archivo | Descripción |
|---|---|
| `SysAlquileres-PowerArchitect.architect` | **Modelo actualizado** del sistema (7 tablas). |
| `PowerArchitect-Original.architect` | Modelo original entregado en `REQUERIMIENTOS/` (6 tablas). |

## Cómo abrirlo

1. Abrir **SQL Power Architect**.
2. `File → Open…` y seleccionar `SysAlquileres-PowerArchitect.architect`.
3. El diagrama aparece en el *Play Pen* con las 7 tablas y sus relaciones.

> También se puede revisar sin instalar nada en `docs/SUPABASE_EXPLICADO.html` (diagrama ERD) y en `docs/SUPABASE.html`.

## Tablas del modelo actualizado

1. `usuario` — login, rol y estado (agregada).
2. `tipo_personeria` — Natural / Jurídica.
3. `departamento` — número, piso, dirección, precio y estado.
4. `inquilino` — DNI único y datos personales / jurídicos.
5. `contrato` — fechas, monto, estado, inquilino y departamento.
6. `pago` — fecha, periodo, monto, método, descuento e IGV.
7. `finalizacion_contrato` — fecha y motivo de finalización.

## Cambios respecto al original

- Se agregó la tabla **`usuario`**, necesaria para el login y para los accesos de la APK (roles `ADMIN` y `CLIENTE`).
- `pago.descuento` e `pago.igv`: de `varchar` a **`numeric(10,2)`**, igual que en `schema.sql`.
- `inquilino.ruc`: ampliado a **11 caracteres**.
- `finalizacion_contrato.id_finalizacion`: ajustado a **BIGINT (int8)**.
- Índices agregados: `idx_contrato_departamento`, `idx_contrato_inquilino` e `idx_pago_contrato`.
- La tabla `usuario` también se dibuja en el *play-pen*.

## Relaciones

```text
tipo_personeria 1 ── N inquilino
inquilino      1 ── N contrato
departamento   1 ── N contrato
contrato       1 ── N pago
contrato       1 ── 1 finalizacion_contrato
usuario        (independiente: solo autenticación)
```

Los scripts SQL equivalentes están en `database/` (`schema.sql`, `postgresql_sistema_alquileres.sql` y `supabase_schema.sql`).
