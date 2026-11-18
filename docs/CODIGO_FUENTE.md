# Código fuente — SysAlquileres v2.0

## 1. Dónde está

| Parte | Ruta |
|---|---|
| Aplicación de escritorio | `src/main/java/pe/edu/upeu/alquileres/` |
| Pantallas FXML | `src/main/resources/view/` |
| Estilos | `src/main/resources/css/app.css` |
| Configuración y SQL inicial | `src/main/resources/application.properties`, `schema.sql`, `seed.sql` |
| Scripts SQL | `database/` |
| Aplicación Android | `android/app/src/main/java/pe/edu/upeu/alquileres/movil/` |
| Recursos Android | `android/app/src/main/res/` |
| Pruebas de construcción | `scripts/build-exe.ps1`, `android/gradlew.bat` |

## 2. Estructura por capas

```
pe.edu.upeu.alquileres
├── App.java                    Punto de entrada (login → principal)
├── SysAlquileres.java          Lanzador alternativo
├── components/                 Diálogos, Celdas (chips), NotaVentaDialog
├── config/                     AppContext (DI) y DatabaseConfig (HikariCP/Supabase)
├── controller/                 Login, Main y controladores de los 4 módulos
├── enums/                      Estados y métodos de pago
├── exception/                  BusinessException
├── impresion/                  NotaVenta, ImpresoraWindows, ImpresoraSerial, DatosEmpresa
├── model/                      Usuario, Departamento, Inquilino, Contrato, Pago…
├── repository/                 JDBC + AbstractJpaRepository + SqlHelper
├── service/                    Interfaces e implementaciones con reglas de negocio
└── util/                       Formato, Sesion, Validador
```

Flujo: **FXML → Controller → Service → Repository → Base de datos**.

## 3. Compilación

### Escritorio

```powershell
mvn -q -DskipTests compile         # compilar
mvn -B javafx:run                   # ejecutar
mvn -q -DskipTests package          # generar JAR
```

Requisitos: JDK 21, Maven 3.9+. JavaFX 21 y el resto de librerías se descargan con Maven (`pom.xml`).

### Android

```powershell
cd android
.\gradlew.bat assembleDebug        # APK de depuración
.\gradlew.bat assembleRelease      # APK para entregar
```

Requisitos: Android SDK (se indica en `android/local.properties`, archivo ignorado por git) y JDK 17/21.

## 4. Dependencias principales

| Dependencia | Uso |
|---|---|
| `javafx-controls`, `javafx-fxml` | Interfaz de escritorio |
| `postgresql`, `h2` | Motores de base de datos |
| `HikariCP` | Pool de conexiones |
| `jakarta.validation`, `hibernate-validator` | Validación de modelos |
| `jSerialComm` | Impresora térmica por puerto COM |
| `lombok` | Getters/setters/builders |
| `slf4j`, `logback` | Registro de eventos |

El APK **no usa dependencias externas**: `HttpURLConnection` + `org.json` (incluidos en Android).

## 5. Explicación detallada

La explicación clase por clase, con fragmentos de código, está en:

- [`docs/GUIA_EXPOSICION.md`](GUIA_EXPOSICION.md) → secciones 4, 5, 6, 7, 8, 9 y 10.
- [`docs/SUPABASE.md`](SUPABASE.md) → conexión a la base en la nube.

## 6. Control de versiones

Repositorio: `https://github.com/Yorlxx/SysAlquileres.git` (rama `main`).

El repositorio **no contiene credenciales reales**: la clave de Supabase se lee de variables de entorno y el archivo local con datos de conexión está ignorado por `.gitignore`.
