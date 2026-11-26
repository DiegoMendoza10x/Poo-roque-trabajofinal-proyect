@echo off
setlocal EnableExtensions EnableDelayedExpansion
title PREPARAR EQUIPO - SysAlquileres (version mejorada)
color 0B

echo ============================================================
echo   SysAlquileres - Preparar copia para companero de equipo
echo   Clona la plantilla y la sube a TU propio GitHub
echo   Version mejorada: buffer, reintentos y verificacion real
echo ============================================================
echo.

REM ============ 1. Verificar Git ============
where git >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Git no esta instalado o no esta en el PATH.
  echo Descargalo de: https://git-scm.com/download/win
  echo.
  pause
  exit /b 1
)

REM ============ 2. Pedir datos ============
set "ORIGEN=https://github.com/Yorlxx/SysAlquileres.git"
set "RAMA=main"

set /p "CARPETA=Ruta destino (ej. C:\Entrega\SysAlquileres): "
if "!CARPETA!"=="" goto :SIN_RUTA

set /p "MIREPO=URL de TU repo GitHub (https://github.com/TU_USUARIO/TU_REPO.git): "
if "!MIREPO!"=="" goto :SIN_REPO

set /p "ORIGEN2=URL del repo plantilla (Enter para usar la original): "
if not "!ORIGEN2!"=="" set "ORIGEN=!ORIGEN2!"

set /p "RAMA2=Rama destino (Enter para usar main): "
if not "!RAMA2!"=="" set "RAMA=!RAMA2!"

REM ============ 3. Preparar carpeta ============
if exist "!CARPETA!" (
  echo.
  echo [AVISO] La carpeta ya existe:
  echo         !CARPETA!
  set /p "BORRAR=¿La eliminamos y la recreamos? (s/n): "
  if /i "!BORRAR!"=="s" (
    rmdir /S /Q "!CARPETA!" >nul 2>nul
    if exist "!CARPETA!" (
      echo [ERROR] No se pudo eliminar. Cierra la carpeta y vuelve a intentar.
      pause
      exit /b 1
    )
  ) else (
    echo [ERROR] Elige otra ruta distinta.
    pause
    exit /b 1
  )
)

REM ============ 4. Clonar con reintentos ============
set "N=0"
:CLON_LOOP
set /a N+=1
echo [CLON intento !N!] git clone ...
git clone "!ORIGEN!" "!CARPETA!"
if exist "!CARPETA!\.git" goto :CLON_OK
if !N! LSS 3 (
  echo Reintentando en 5 segundos...
  timeout /t 5 >nul
  goto :CLON_LOOP
)
echo [ERROR] No se pudo clonar despues de 3 intentos.
echo Revisa la URL, tu conexion y que el repo no sea privado sin acceso.
pause
exit /b 1

:CLON_OK
cd /d "!CARPETA!"
if errorlevel 1 (
  echo [ERROR] No se pudo entrar a la carpeta clonada.
  pause
  exit /b 1
)

REM ============ 5. Remotos ============
echo.
echo [1/6] Configurando remotos...
git remote rename origin plantilla >nul 2>nul
git remote add origin "!MIREPO!"

REM ============ 6. Ajustes anti-timeout (HTTP 408) ============
echo [2/6] Aplicando ajustes de red para archivos grandes...
git config http.postBuffer 524288000
git config http.lowSpeedLimit 0
git config http.lowSpeedTime 999999
git config http.version HTTP/1.1
echo       Buffer ampliado y timeouts desactivados.

REM ============ 7. Verificar entregables ============
echo [3/6] Verificando entregables...
if exist "docs\MANUAL_USUARIO.html" echo    [OK] Manual de usuario HTML
if exist "docs\PRESENTACION.html" echo    [OK] Presentacion interactiva
if exist "modelo-power-architect\SysAlquileres-PowerArchitect.architect" echo    [OK] Modelo Power Architect
if exist "dist\SysAlquileres-Movil-v1.1.apk" echo    [OK] APK v1.1
if exist "dist\SysAlquileres-v2.0.0-portable.zip" echo    [OK] ZIP portable Windows
if exist "README.md" echo    [OK] README general

REM ============ 8. Saber si el repo destino esta vacio ============
set "TIENE_REF=0"
for /f "delims=" %%R in ('git ls-remote origin 2^>nul') do set "TIENE_REF=1"
if "!TIENE_REF!"=="0" (
  echo       Repo destino VACIO ("push" seguro).
) else (
  echo       AVISO: el repo destino YA TIENE contenido.
)

REM ============ 9. Push con reintentos ============
echo [4/6] Subiendo al repositorio...
set "INTENTOS=0"
:PUSH_LOOP
set /a INTENTOS+=1
echo.
echo  --- Intento !INTENTOS! de 3 ---
git push -u origin "!RAMA!"
call :VERIFICAR_REMOTO
if "!REMOTE_OK!"=="1" goto :PUSH_OK
if !INTENTOS! LSS 3 (
  echo El push no se confirmo. Reintentando en 8 segundos...
  timeout /t 8 >nul
  goto :PUSH_LOOP
)

REM ============ 10. Ultimo recurso ============
echo.
echo [AVISO] Los 3 intentos normales fallaron.
if "!TIENE_REF!"=="0" (
  echo El repo esta vacio: se usara "push -f" seguro.
  git push -f -u origin "!RAMA!"
  call :VERIFICAR_REMOTO
  if "!REMOTE_OK!"=="1" goto :PUSH_OK
) else (
  set /p "OPCION=Elige: (s) unir historias, (f) forzar, (c) cancelar: "
  if /i "!OPCION!"=="s" (
    git pull --allow-unrelated-histories origin "!RAMA!"
    if errorlevel 1 (
      echo [ERROR] No se pudieron unir las historias.
      pause
      exit /b 1
    )
    git push -u origin "!RAMA!"
    call :VERIFICAR_REMOTO
    if "!REMOTE_OK!"=="1" goto :PUSH_OK
  ) else if /i "!OPCION!"=="f" (
    git push -f -u origin "!RAMA!"
    call :VERIFICAR_REMOTO
    if "!REMOTE_OK!"=="1" goto :PUSH_OK
  ) else (
    goto :CANCELAR
  )
)

echo.
echo [ERROR] No se pudo confirmar la subida.
echo Prueba manualmente dentro de la carpeta:
echo   git config http.postBuffer 524288000
echo   git push -u origin !RAMA!
echo O cambia la URL por SSH: git remote set-url origin git@github.com:USUARIO/REPO.git
pause
exit /b 1

:PUSH_OK
echo.
echo [5/6] Push confirmado: el hash remoto coincide con el local.

REM ============ 11. Resumen final ============
echo [6/6] Resumen final:
echo.
echo Remotos:
git remote -v
echo.
echo Estado:
git status -sb
echo.
echo ============================================================
echo   LISTO: tu repositorio contiene el proyecto completo.
echo   Incluye codigo, manuales HTML, presentacion, modelo
echo   Power Architect, APK v1.1 y empaquetado Windows.
echo.
echo   El .bat NO guarda contraseñas. Si GitHub pide acceso,
echo   usa tu usuario + PAT o la credencial guardada.
echo ============================================================
echo.
pause
endlocal
exit /b 0

REM ============ RUTINAS ============
:VERIFICAR_REMOTO
set "REMOTE_HASH="
set "LOCAL_HASH="
set "REMOTE_OK=0"
for /f "tokens=1" %%H in ('git ls-remote --heads origin "!RAMA!" 2^>nul') do set "REMOTE_HASH=%%H"
for /f "delims=" %%H in ('git rev-parse HEAD 2^>nul') do set "LOCAL_HASH=%%H"
if defined REMOTE_HASH if defined LOCAL_HASH (
  if "!REMOTE_HASH!"=="!LOCAL_HASH!" set "REMOTE_OK=1"
)
exit /b

:CANCELAR
echo.
echo Operacion cancelada por el usuario. Tu copia local queda intacta.
pause
exit /b 1

:SIN_RUTA
echo [ERROR] Debes indicar una ruta destino.
pause
exit /b 1

:SIN_REPO
echo [ERROR] Debes indicar la URL de tu repositorio.
pause
exit /b 1
