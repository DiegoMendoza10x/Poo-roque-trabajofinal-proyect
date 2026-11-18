# ============================================================
#  Genera el ejecutable portable de SysAlquileres (Windows)
#  Uso:  powershell -ExecutionPolicy Bypass -File scripts\build-exe.ps1
#  Requiere: JDK 21 con jpackage (incluido en el JDK de Oracle)
# ============================================================
$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
Push-Location $raiz
try {
    Write-Host "[1/4] Compilando proyecto..." -ForegroundColor Cyan
    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Falló la compilación con Maven" }

    Write-Host "[2/4] Copiando dependencias..." -ForegroundColor Cyan
    if (Test-Path "target\app") { Remove-Item "target\app" -Recurse -Force }
    New-Item -ItemType Directory -Path "target\app" | Out-Null
    Copy-Item "target\SysAlquileres-1.0.0.jar" "target\app\"
    mvn -q dependency:copy-dependencies "-DoutputDirectory=target\app" -DincludeScope=runtime
    if ($LASTEXITCODE -ne 0) { throw "Falló la copia de dependencias" }

    Write-Host "[3/4] Generando imagen de aplicación con jpackage..." -ForegroundColor Cyan
    if (Test-Path "dist") { Remove-Item "dist" -Recurse -Force }
    $modulos = "java.base,java.desktop,java.sql,java.naming,java.logging," +
               "java.management,java.xml,java.prefs,java.scripting,jdk.unsupported," +
               "jdk.crypto.ec,jdk.crypto.cryptoki"

    jpackage --type app-image `
        --name SysAlquileres `
        --app-version 2.0.0 `
        --input "target\app" `
        --main-jar "SysAlquileres-1.0.0.jar" `
        --main-class "pe.edu.upeu.alquileres.App" `
        --dest "dist" `
        --vendor "UPeU" `
        --description "Sistema de gestion de alquileres de departamentos" `
        --icon "icono\icono.ico" `
        --java-options "-Dfile.encoding=UTF-8" `
        --add-modules $modulos
    if ($LASTEXITCODE -ne 0) { throw "Falló jpackage" }

    Write-Host "[4/4] Preparando ZIP portable..." -ForegroundColor Cyan
    Remove-Item "dist\SysAlquileres\data" -Recurse -Force -ErrorAction SilentlyContinue
    Get-ChildItem "dist\SysAlquileres" -Filter "*prueba*" -ErrorAction SilentlyContinue |
        Remove-Item -Force
    Compress-Archive -Path "dist\SysAlquileres" `
        -DestinationPath "dist\SysAlquileres-v2.0.0-portable.zip" -Force

    Write-Host ""
    Write-Host "LISTO:" -ForegroundColor Green
    Write-Host "  Ejecutable: dist\SysAlquileres\SysAlquileres.exe"
    Write-Host "  Para entregar: dist\SysAlquileres-v2.0.0-portable.zip"
    Write-Host "  El ZIP incluye el JRE; no requiere Java instalado."
}
finally {
    Pop-Location
}
