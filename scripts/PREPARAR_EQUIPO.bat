@echo off
setlocal EnableExtensions EnableDelayedExpansion
title PREPARAR EQUIPO - SysAlquileres
color 0B

echo ============================================================
echo   SysAlquileres - Preparar copia para companero de equipo
echo   Clona el proyecto plantilla y lo sube a TU propio GitHub
echo ============================================================
echo.

REM ---------- 1. Verificar Git ----------
where git >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Git no esta instalado o no esta en el PATH.
  echo Descargalo de: https://git-scm.com/download/win
  echo.
  pause
  exit /b 1
)

REM ---------- 2. Pedir datos ----------
set "ORIGEN=https://github.com/Yorlxx/SysAlquileres.git"
set "RAMA=main"

set /p "CARPETA=Ruta destino (ej. C:\Entrega\SysAlquileres): "
if "!CARPETA!"=="" (
  echo [ERROR] Debes indicar una ruta destino.
  pause
  exit /b 1
)

set /p "MIREPO=URL de TU repo GitHub (https://github.com/TU_USUARIO/TU_REPO.git): "
if "!MIREPO!"=="" (
  echo [ERROR] Debes indicar la URL de tu repositorio.
  pause
  exit /b 1
)

set /p "ORIGEN2=URL del repo plantilla (Enter para usar la original): "
if not "!ORIGEN2!"=="" set "ORIGEN=!ORIGEN2!"

set /p "RAMA2=Rama destino (Enter para usar main): "
if not "!RAMA2!"=="" set "RAMA=!RAMA2!"

REM ---------- 3. Preparar carpeta ----------
if exist "!CARPETA!" (
  echo.
  echo [AVISO] La carpeta ya existe:
  echo         !CARPETA!
  set /p "BORRAR=¿La eliminamos y recreamos? (s/n): "
  if /i "!BORRAR!"=="s" (
    rmdir /S /Q "!CARPETA!" >nul 2>nul
    if exist "!CARPETA!" (
      echo [ERROR] No se pudo eliminar la carpeta. Cierrala y vuelve a intentar.
      pause
      exit /b 1
    )
  ) else (
    echo [ERROR] Elige otra ruta distinta.
    pause
    exit /b 1
  )
)

REM ---------- 4. Clonar ----------
echo.
echo [1/5] Clonando el proyecto plantilla...
git clone "!ORIGEN!" "!CARPETA!"
if errorlevel 1 (
  echo [ERROR] No se pudo clonar. Revisa la URL y tu conexion.
  pause
  exit /b 1
)

cd /d "!CARPETA!"
if errorlevel 1 (
  echo [ERROR] No se pudo entrar a la carpeta clonada.
  pause
  exit /b 1
)

REM ---------- 5. Configurar remotos ----------
echo [2/5] Configurando remotos...
git remote rename origin plantilla
git remote add origin "!MIREPO!"

REM ---------- 6. Verificar entregables ----------
echo [3/5] Verificando entregables incluidos...
if exist "docs\MANUAL_USUARIO.html" echo    [OK] Manual de usuario HTML
if exist "docs\PRESENTACION.html" echo    [OK] Presentacion interactiva
if exist "modelo-power-architect\SysAlquileres-PowerArchitect.architect" echo    [OK] Modelo Power Architect
if exist "dist\SysAlquileres-Movil-v1.1.apk" echo    [OK] APK v1.1
if exist "dist\SysAlquileres-v2.0.0-portable.zip" echo    [OK] ZIP portable Windows
if exist "README.md" echo    [OK] README general

REM ---------- 7. Subir al repo del companero ----------
echo [4/5] Subiendo a tu repositorio...
git push -u origin "!RAMA!"
if errorlevel 1 (
  echo.
  echo [AVISO] El push fallo. Tu repositorio podria no estar vacio.
  set /p "OPCION=Elige: (s) unir historias, (f) forzar, (c) cancelar: "
  if /i "!OPCION!"=="s" (
    git pull --allow-unrelated-histories origin "!RAMA!"
    if errorlevel 1 (
      echo [ERROR] No se pudieron unir las historias.
      pause
      exit /b 1
    )
    git push -u origin "!RAMA!"
  ) else if /i "!OPCION!"=="f" (
    git push -f -u origin "!RAMA!"
  ) else (
    echo Operacion cancelada por el usuario.
    pause
    exit /b 1
  )
  if errorlevel 1 (
    echo [ERROR] No se pudo subir. Revisa permisos o credenciales de GitHub.
    pause
    exit /b 1
  )
)

REM ---------- 8. Resumen ----------
echo [5/5] Resumen final:
echo.
echo Remotos configurados:
git remote -v
echo.
echo Estado de la rama:
git status -sb
echo.
echo ============================================================
echo   LISTO: tu repositorio ya contiene el proyecto completo.
echo   Incluye codigo, manuales HTML, presentacion, modelo
echo   Power Architect, APK v1.1 y el empaquetado Windows.
echo.
echo   Si GitHub pide credenciales, usa tu usuario y una PAT
echo   o la credencial guardada. NUNCA escribas passwords
echo   dentro de este archivo .bat.
echo ============================================================
echo.
pause
endlocal
