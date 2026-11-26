# Impresión de nota de venta en impresora térmica (58 mm / 80 mm)

## 1. ¿Qué se implementó?

El CRUD de **Pagos** quedó completo: al **registrar un pago nuevo**, el sistema
guarda el pago y pregunta si se desea emitir la nota de venta.

Si la respuesta es **“Imprimir nota”**, se abre una ventana con:

- **Vista previa** del ticket según el papel elegido.
- **Papel:** 58 mm (32 columnas) o 80 mm (48 columnas).
- **Conexión:**
  1. **Cable / Windows** → impresora instalada en Windows (`POS-58-Series`, `POS-80-Series`, etc.).
  2. **Bluetooth (COM)** → puerto serial creado al emparejar la impresora.
- Botones **Imprimir nota**, **Copiar texto** y **Cerrar**.

También se puede reemitir una nota desde el botón **“Nota de venta”** de la tabla,
seleccionando primero un pago.

> La nota de venta **no es comprobante fiscal**: no reemplaza a la boleta ni a la
> factura electrónica de SUNAT. Es un documento interno de control.

> Toda la configuración se hace desde el panel **Config. impresión** de la barra lateral.
> Ahí se eligen papel, conexión, impresora/puerto y velocidad, se guarda y se puede
> hacer una impresión de prueba. Ver [`CONFIGURACION.md`](CONFIGURACION.md).

---

## 2. Flujo completo al hacer un pago

```text
Formulario de pagos
   → Guardar pago (service + base de datos)
   → Pregunta: ¿Imprimir nota de venta?
        → Ahora no: termina
        → Imprimir nota:
             → Elegir papel (58 / 80 mm)
             → Elegir conexión (Windows o Bluetooth COM)
             → Imprimir en la impresora térmica
```

El pago se guarda **antes** de preguntar, así que el número de nota es el del
pago recién creado (`NV-000123`, por ejemplo).

---

## 3. ¿Cómo se usa?

### Desde el registro de un pago

1. Abrir **Pagos** → **Nuevo**.
2. Llenar contrato, fecha, periodo, monto, método, descuento e IGV.
3. Presionar **Guardar**.
4. En el diálogo elegir **Imprimir nota** o **Ahora no**.
5. Si se eligió imprimir:
   - Elegir **Papel**: 58 u 80 mm.
   - Elegir **Cable / Windows** o **Bluetooth (COM)**.
   - Seleccionar la impresora o el puerto y presionar **Imprimir nota**.

### Desde un pago ya registrado

1. Seleccionar un pago de la tabla.
2. Presionar **Nota de venta**.
3. Configurar papel y conexión, luego imprimir.

---

## 4. Ancho del ticket: 58 mm y 80 mm

La fuente normal de una impresora térmica usa **12 puntos por carácter**:

| Papel | Puntos | Columnas |
|---|---:|---:|
| 58 mm | 384 | `384 / 12 = 32` |
| 80 mm | 576 | `576 / 12 = 48` |

`NotaVenta.columnas(milimetros)` decide cuántas columnas usar, y todas las
líneas se centran, se recortan o se alinean a la derecha con ese valor.

Ejemplo en 80 mm:

```text
|Alquiler                             S/ 1,250.00|
|TOTAL                                S/ 1,300.00|
```

---

## 5. Impresión por cable (Windows)

```text
Java → driver de Windows → cable USB → impresora
```

- `ImpresoraWindows` lista las impresoras con `PrintServiceLookup`.
- `PrinterJob` recibe un `PageFormat` con el ancho elegido (58 u 80 mm).
- La fuente monoespaciada se ajusta para que quepan 32 o 48 caracteres.
- Windows entrega el trabajo al controlador y este lo envía a la impresora.

En esta computadora se detectaron `POS-58-Series` y `POS-80-Series`.

---

## 6. Impresión por Bluetooth (puerto COM)

```text
Java → puerto COM virtual → Bluetooth → impresora
```

1. Emparejar la impresora en **Configuración → Bluetooth**.
2. Windows crea un puerto COM (por ejemplo `COM5`).
3. `ImpresoraSerial` usa **jSerialComm** con 8 datos, 1 parada, sin paridad y
   velocidad configurable (9600 por defecto).
4. Se envían comandos **ESC/POS** directamente:

| Comando | Bytes | Significado |
|---|---|---|
| `ESC @` | `1B 40` | Inicializar impresora |
| Texto | ASCII | Contenido del ticket |
| `ESC d 4` | `1B 64 04` | Avanzar 4 líneas |

Antes de enviar se quitan las tildes, porque muchas impresoras económicas no las
interpretan bien.

---

## 7. Cálculo y contenido del ticket

```text
TOTAL = monto - descuento + IGV
```

Se usa `BigDecimal` para no perder centavos.

Incluye: empresa, RUC, dirección, teléfono, número `NV-`, fecha de emisión,
cliente, DNI, departamento, contrato, periodo, fecha de pago, atendido,
alquiler, descuento, IGV, total, método de pago y mensaje de no fiscalidad.

---

## 8. Archivos

| Archivo | Función |
|---|---|
| `impresion/DatosEmpresa.java` | Datos de la cabecera (editar aquí). |
| `impresion/NotaVenta.java` | Arma el ticket para 58 o 80 mm. |
| `impresion/ImpresoraWindows.java` | Impresión por driver de Windows. |
| `impresion/ImpresoraSerial.java` | Impresión ESC/POS por COM. |
| `components/NotaVentaDialog.java` | Vista previa, papel y conexión. |
| `config/ConfigImpresion.java` | Guarda y carga la configuración elegida. |
| `controller/ConfiguracionImpresionController.java` | Lógica del panel de configuración. |
| `view/configuracion-impresion.fxml` | Panel de configuración en la barra lateral. |
| `components/Dialogos.java` | Pregunta “Imprimir nota / Ahora no”. |
| `controller/PagoController.java` | Guarda el pago y dispara la pregunta. |
| `view/pagos.fxml` | Botón “Nota de venta”. |
| `pom.xml` | Dependencia `jSerialComm`. |

---

## 9. Problemas frecuentes

| Problema | Solución |
|---|---|
| No aparece la impresora | Instalarla o emparejarla por Bluetooth. |
| “No se pudo abrir el puerto COM” | Encender la impresora y revisar el número de COM. |
| Texto cortado | Elegir el papel correcto (58 u 80 mm). |
| Símbolos raros | El modo COM ya envía ASCII sin tildes. |
| No corta el papel | Normal en impresoras económicas; solo avanza líneas. |

---

## 10. Explicación corta para exponer

> “Cuando se registra un pago, el sistema lo guarda y pregunta si se quiere la
> nota de venta. La nota se arma en 32 columnas para papel de 58 mm o 48 columnas
> para 80 mm. Si la impresora está en Windows, se imprime por cable USB o
> Bluetooth usando el driver; si el Bluetooth crea un puerto COM, se envían
> comandos ESC/POS directamente. El total se calcula con `BigDecimal` y el
> documento es interno, no fiscal.”
