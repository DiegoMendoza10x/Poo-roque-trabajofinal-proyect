# INFORME TÉCNICO PARA ANÁLISIS CON IA — SysAlquileres v2.0

> Documento autocontenido para estudiar el proyecto con ChatGPT, Claude, Gemini u otra IA.
> No contiene credenciales reales. Credenciales de prueba: **admin / admin123**.

---

## PROMPT SUGERIDO

```text
Eres un profesor de Programación Orientada a Objetos y Java. Analiza el informe
siguiente del proyecto SysAlquileres y responde:

1) Explica la arquitectura por capas y cómo viajan los datos desde un clic hasta la base de datos y de vuelta.
2) Identifica los 4 pilares OOP (abstracción, encapsulamiento, herencia, polimorfismo)
   con ejemplos concretos del código.
3) Explica los flujos de login, guardar un contrato e imprimir una nota de venta.
4) Compara H2 local, Supabase por JDBC y Supabase por REST (APK).
5) Formula 10 preguntas probables de una exposición y respúndelas brevemente.
Usa lenguaje sencillo y menciona nombres reales de clases y métodos.
```

---

## 1. RESUMEN

SysAlquileres es un sistema para administrar alquileres de departamentos. Tiene cuatro
módulos: Departamentos, Inquilinos, Contratos y Pagos, más login. Resuelve errores típicos
de llevar el control en papel/Excel: alquilar dos veces el mismo departamento, pagos sin
contrato, inquilinos duplicados o fechas imposibles.

**Entregables exigidos por el profesor:** (1) manual de usuario, (2) código fuente,
(3) empaquetamiento `.exe`. Adicionalmente se hizo un APK Android.

**Stack:** Java 21, JavaFX 21 + FXML + CSS, Maven, H2 embebida, Supabase PostgreSQL 17,
JDBC + HikariCP, Jakarta Validation, Lombok, SLF4J/Logback, jSerialComm, jpackage,
Android nativo con HttpURLConnection + org.json.

---

## 2. ESTRUCTURA DE CARPETAS

```
SysAlquileres/
├─ pom.xml
├─ README.md, ENTREGABLES.md
├─ database/
│  ├─ postgresql_sistema_alquileres.sql
│  ├─ supabase_schema.sql
│  └─ supabase_movil_policies.sql
├─ docs/
│  ├─ MANUAL_USUARIO.md / .html
│  ├─ GUIA_EXPOSICION.md
│  ├─ SUPABASE.md, IMPRESION_58MM.md
│  ├─ ESTUDIO_COMPLETO.html
│  └─ INFORME_PARA_IA.md (este archivo)
├─ icono/            JPG original, icono-1024.png, icono.ico
├─ scripts/          build-exe.ps1, prepare_icon.py
├─ dist/             .exe + ZIP portable + APK
├─ android/          proyecto Gradle/Android
└─ src/main/
   ├─ java/pe/edu/upeu/alquileres/
   │  ├─ App.java, SysAlquileres.java
   │  ├─ components/  config/  controller/  enums/  exception/
   │  ├─ impresion/   model/    repository/  service/  util/
   └─ resources/
      ├─ application.properties, schema.sql, seed.sql
      ├─ css/app.css, images/app-icon.png
      └─ view/*.fxml
```

---

## 3. ARQUITECTURA POR CAPAS

```
FXML (vista) → Controller (eventos) → Service (reglas) → Repository (SQL) → Base de datos
```

| Capa | Responsabilidad | Ejemplo |
|---|---|---|
| Vista | Mostrar controles con FXML y CSS | `departamentos.fxml` |
| Controller | Recibir clics, leer campos, llenar tablas | `PagoController` |
| Service | Validar reglas de negocio | `ContratoServiceImp` |
| Repository | Traducir objetos a SQL con PreparedStatement | `ContratoRepository` |
| Model | Representar datos | `Departamento`, `Pago` |
| Config/Util | Conexión, DI, formato, validación | `AppContext`, `DatabaseConfig` |

**Inyección de dependencias:** `AppContext` es un singleton que crea repositorios →
servicios → controladores y los guarda en un `Map<Class<?>, Object>`. Al cargar un FXML:
`loader.setControllerFactory(AppContext.getInstance()::getBean)`. Así el controlador
recibe sus servicios por constructor.

**Grafo real:**

- LoginController ← IUsuarioService ← UsuarioServiceImp ← UsuarioRepository
- DepartamentoController ← IDepartamentoService ← DepartamentoServiceImp ← DepartamentoRepository + ContratoRepository
- InquilinoController ← IInquilinoService ← InquilinoServiceImp ← InquilinoRepository + ContratoRepository + TipoPersoneriaRepository
- ContratoController ← IContratoService, IInquilinoService, IDepartamentoService
- PagoController ← IPagoService, IContratoService

---

## 4. CLASES PRINCIPALES

| Clase | Hereda / implementa | Recibe | Produce / hace |
|---|---|---|---|
| `App` | — | ruta FXML, título, tamaño | carga escena con CSS e icono |
| `App.Ventana` | extends `Application` | `Stage` | `start()` y `stop()` |
| `AppContext` | Singleton | — | beans (getBean) |
| `DatabaseConfig` | Singleton | properties + env vars | DataSource HikariCP |
| `Sesion` | utilidad final | `Usuario` | usuario, nombre, rol |
| `Validador` | utilidad final | objeto anotado | BusinessException o nada |
| `Formato` | utilidad final | texto/fechas/dinero | cadenas formateadas |
| `BusinessException` | extends `RuntimeException` | mensaje | error controlado |
| `SqlHelper<T>` | abstracta | — | abre conexiones, ejecuta queries |
| `AbstractJpaRepository<T,ID>` | extends SqlHelper, implements ICrudGenericoRepository | entidad T / id ID | CRUD con commit/rollback |
| `UsuarioRepository` | extends AbstractJpaRepository | usuario, clave | Optional<Usuario> |
| `DepartamentoRepository` | extends AbstractJpaRepository | entidad/id | findByNumero, findDisponibles |
| `InquilinoRepository` | extends AbstractJpaRepository | entidad/id | findByDni |
| `ContratoRepository` | extends AbstractJpaRepository | entidad/id | findVigenteSuperpuesto, finalizar |
| `PagoRepository` | extends AbstractJpaRepository | entidad/id | countByContrato, sumTotal |
| `I*Service` / `*ServiceImp` | interfaz / implementación | repositorios + entidades | reglas de negocio |
| `Dialogos` | utilidad | textos | Alert de JavaFX |
| `Celdas` | utilidad | funciones texto+estilo | TableCell con chips |
| `NotaVentaDialog` | utilidad | Window, Pago | vista previa e impresión |
| `NotaVenta` | utilidad | Pago | List<String> de 32 columnas |
| `ImpresoraWindows` | utilidad | PrintService, líneas | impresión javax.print |
| `ImpresoraSerial` | utilidad | puerto, baudios, líneas | comandos ESC/POS |

### Modelos

`Usuario`, `Departamento`, `Inquilino`, `TipoPersoneria`, `Contrato`, `Pago`,
`FinalizacionContrato`. Usan Lombok `@Data`, `@Builder`, `@NoArgsConstructor`,
`@AllArgsConstructor`. Tienen getters calculados como `getInquilinoNombre()`,
`getDepartamentoNumero()`, `getMontoTexto()`, `getPeriodoTexto()` que alimentan columnas.

### Enums

`EstadoDepartamento` (DISPONIBLE, OCUPADO), `EstadoContrato` (VIGENTE, FINALIZADO),
`MetodoPago` (EFECTIVO, TRANSFERENCIA, YAPE, PLIN, TARJETA), con `getDescripcion()` y `desde(String)`.

### Generics

`AbstractJpaRepository<T, ID>`: T = entidad, ID = tipo de clave. Ejemplo:
`DepartamentoRepository extends AbstractJpaRepository<Departamento, Long>` convierte
`findById(ID)` en `findById(Long)` y `findAll()` en `List<Departamento>`. Evita repetir
el CRUD y evita casts. Otros generics: `ObservableList<Pago>`, `FilteredList<Contrato>`,
`TableColumn<Pago, BigDecimal>`, `Optional<Usuario>`.

---

## 5. FLUJOS

### Arranque

`App.main()` → `Application.launch(App.Ventana.class)` → `start()` →
`AppContext.getInstance()` → `DatabaseConfig.init()` → registrar repos/services/controllers →
`App.mostrarLogin()` → `cargar("/view/login.fxml")` con FXMLLoader, controllerFactory,
`app.css` e icono. Al cerrar: `stop()` → `DatabaseConfig.shutdown()`.

### Login

`login.fxml` (`onAction="#onIngresar"`) → `LoginController.onIngresar()` →
`IUsuarioService.login()` → `UsuarioServiceImp` valida campos →
`UsuarioRepository.login()`: `SELECT * FROM usuario WHERE usuario=? AND clave=? AND estado='Activo'`.
Si hay fila: `Sesion.iniciar(usuario)` → `App.mostrarPrincipal()`. Si no: error rojo.

### Navegación

Botón del sidebar → `MainController.irInquilinos()` → `abrir("inquilinos")` busca en `cache`;
si no existe carga el FXML; reemplaza `StackPane contenido`, actualiza cabecera y marca
`nav-button-activo`.

### Guardar departamento

`DepartamentoController.onGuardar()` → `Departamento.builder()…build()` →
`IDepartamentoService.save()` → `DepartamentoServiceImp`: `normalizar()`,
`Validador.validar()`, `validarNumeroUnico()`, estado DISPONIBLE →
`AbstractJpaRepository.save()` (`setAutoCommit(false)`, `insert`, `commit`/`rollback`) →
`cargar()` y `limpiarFormulario()`.

### Contrato

`ContratoServiceImp.save()` → `validarContrato()`: fecha fin > inicio,
`findVigenteSuperpuesto(depto, inicio, fin, excluirId)` lanza `BusinessException` si existe;
`repository.save()` y `departamentoRepository.updateEstado(depto, OCUPADO)`.
`finalizar()` → `ContratoRepository.finalizar()` en una transacción: UPDATE contrato,
INSERT finalizacion_contrato, UPDATE departamento a Disponible si no queda otro vigente.

### Pago y nota

`PagoController.onGuardar()` → `PagoServiceImp.normalizar()` fija periodo al día 1,
valores nulos a cero, descuento/IGV ≥ 0 → `Validador.validar()` → `PagoRepository.insert()`.
Seleccionar pago → `onImprimirNota()` → `NotaVentaDialog.mostrar()` →
`NotaVenta.lineas()` genera 32 columnas → `ImpresoraWindows` (javax.print, 58 mm) o
`ImpresoraSerial` (ESC/POS por COM).

### Cómo viaja un dato

Texto del TextField → Controllerarma un objeto → Service normaliza/valida →
Repository convierte a parámetros SQL → fila en BD → SELECT → `mapRow(ResultSet)` crea
objetos → `ObservableList` → `FilteredList` → `PropertyValueFactory` lee getters →
celdas de la tabla. El buscador dispara `aplicarFiltro()` y la tabla se actualiza.

---

## 6. BASE DE DATOS

Tablas: `usuario`, `tipo_personeria`, `departamento`, `inquilino`, `contrato`, `pago`,
`finalizacion_contrato`. Vistas: `vw_contrato_detalle`, `vw_pago_detalle`.

Relaciones: `tipo_personeria 1—N inquilino`; `departamento 1—N contrato N—1 inquilino`;
`contrato 1—N pago`; `contrato 0..1 finalizacion_contrato`.

Scripts:
- `schema.sql` + `seed.sql` → H2 (6 deptos, 5 inquilinos, 5 contratos, 8 pagos, admin).
- `postgresql_sistema_alquileres.sql` → PostgreSQL local con EXCLUDE/btree_gist.
- `supabase_schema.sql` → Supabase (sin EXCLUDE; regla validada en Java).
- `supabase_movil_policies.sql` → RLS anon SELECT/INSERT/UPDATE/DELETE para el APK.

---

## 7. SUPABASE

URL: `https://zarbbelqcsivzhjrpguu.supabase.co`, PostgreSQL 17.12, Session pooler
`aws-0-us-east-1.pooler.supabase.com:5432`.

**Escritorio (JDBC):**
`jdbc:postgresql://…:5432/postgres?sslmode=require&prepareThreshold=0`.
`DatabaseConfig.loadProperties()` lee `application.properties` y sobrescribe con
`DB_*` y `SUPABASE_DB_*`. `normalizarUrl()` agrega SSL y prepareThreshold si detecta
Supabase. `ddl.auto=false` y `seed.demo=false`.

**APK (REST):** `GET/POST/PATCH/DELETE /rest/v1/<tabla>` con headers `apikey` y
`Authorization: Bearer <anon key>`. Android no puede usar JDBC/HikariCP. La anon key es
pública; la seguridad la dan las políticas RLS. La secret key nunca se usa en el cliente
y debe rotarse si se compartió.

---

## 8. APK

Proyecto `android/`, Java nativo, sin dependencias externas (HttpURLConnection + org.json).

- `SupabaseClient`: get/post/patch/delete con headers de Supabase.
- `LoginActivity`: valida contra `/rest/v1/usuario`, guarda sesión en SharedPreferences.
- `MainActivity`: pestañas Departamentos/Inquilinos/Contratos/Pagos, `ItemAdapter` con
  lista maestra `todos` y lista `visibles` filtrada (esto corrigió el bug al cambiar de pestaña).
- `FormularioActivity`: campos dinámicos por módulo con combos desde Supabase; POST para
  alta, PATCH para edición, DELETE para borrar; nota de venta compartible con ACTION_SEND.

Iconos: mipmaps `ic_launcher`, `ic_launcher_round` e icono adaptativo.

---

## 9. EMPAQUETAMIENTO E ICONOS

`scripts/build-exe.ps1`: `mvn package` → copia JARs a `target\app` →
`jpackage --type app-image --icon icono\icono.ico` con módulos
`java.base, java.desktop, java.sql, java.naming, java.logging, java.management,
java.xml, java.prefs, java.scripting, jdk.unsupported, jdk.crypto.ec, jdk.crypto.cryptoki`
→ ZIP portable en `dist/`. `java.scripting` es requerido por FXML; `jdk.crypto.*` por TLS.

`scripts/prepare_icon.py`: recorta el JPG, elimina fondo blanco conectado al borde, genera
PNG 1024, ICO multi-tamaño (16–256), mipmaps Android y `app-icon.png` para la ventana JavaFX.

---

## 10. PILARES OOP

- **Encapsulamiento:** campos privados + Lombok; `Sesion.actual`; repositorios esconden SQL.
- **Abstracción:** `ICrudGenericoRepository<T,ID>`, interfaces `I*Service`, `App` oculta JavaFX.
- **Herencia:** `AbstractJpaRepository<T,ID>` → repositorios concretos; `App.Ventana extends Application`; `BusinessException extends RuntimeException`.
- **Polimorfismo:** `getBean()` devuelve implementaciones tras interfaces; `TableCell.updateItem()` sobrescrito en `Celdas` y controllers; lambdas.

Patrones: Singleton, DI, Repository/DAO, Builder (Lombok), Factory, Observer (listeners),
Template Method (`save()`), MVC, generics.

---

## 11. PREGUNTAS Y RESPUESTAS BREVES

1. ¿Por qué capas? → Separación de responsabilidades; mantenimiento.
2. ¿Dónde está la lógica? → `service/impl`.
3. ¿Cómo evitas solapamiento? → `findVigenteSuperpuesto` + BusinessException.
4. ¿Por qué BigDecimal? → Exactitud monetaria.
5. ¿Qué es AppContext? → Singleton que inyecta dependencias.
6. ¿Qué hace HikariCP? → Pool de conexiones.
7. ¿Cómo conectas Supabase? → JDBC + TLS + env vars; APK usa REST.
8. ¿Qué pasa sin internet? → H2 local automáticamente.
9. ¿Cómo imprimes? → javax.print o ESC/POS por puerto COM.
10. ¿Cómo generaste el .exe? → jpackage con módulos extra.
11. ¿Por qué no JavaFX en APK? → Requiere Gluon; se reutilizó base/reglas/diseño.
12. ¿Es segura la anon key? → Sí; RLS es la protección real.
13. ¿PreparedStatement? → Consulta con `?` que evita inyección SQL.
14. ¿Por qué Scene Builder se ve distinto? → No aplica CSS ni ejecuta controladores.
15. ¿Transacción? → Operaciones confirmadas con commit o deshechas con rollback.

---

## 12. COMANDOS

```powershell
mvn -q -DskipTests compile
mvn -B javafx:run
powershell -ExecutionPolicy Bypass -File scripts\build-exe.ps1
python scripts\prepare_icon.py
cd android
.\gradlew.bat assembleRelease
```

Repositorio: `https://github.com/Yorlxx/SysAlquileres.git` (rama `main`).
