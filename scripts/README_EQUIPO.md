# Script para compañeros de equipo

Archivo: `scripts/PREPARAR_EQUIPO.bat`

## Qué hace

1. Clona el repositorio plantilla (`https://github.com/Yorlxx/SysAlquileres.git`) en la carpeta que indiques.
2. Renombra el remoto original a `plantilla`.
3. Agrega el repositorio de cada compañero como `origin`.
4. Sube la rama `main` con todo el proyecto.
5. Muestra un resumen de entregables y el estado final.

Incluye código, manuales HTML, presentación, modelo Power Architect, APK v1.1 y el ZIP portable de Windows.

## Cómo usarlo

1. Crear un repositorio **vacío** en la cuenta GitHub de cada compañero.
2. Copiar `PREPARAR_EQUIPO.bat` a esa PC (o ejecutarlo desde la carpeta `scripts` del proyecto).
3. Doble clic.
4. Responder:
   - **Ruta destino**: por ejemplo `C:\Entrega\SysAlquileres`.
   - **URL del repo propio**: `https://github.com/USUARIO/REPO.git`.
   - Presionar Enter en las preguntas opcionales para usar los valores por defecto.

Si el repo destino no está vacío, el script ofrece tres opciones: unir historias, forzar el push o cancelar.

## Credenciales

El script **no contiene contraseñas ni tokens**. Si GitHub las pide, usar el usuario y una PAT (o la credencial guardada por Git Credential Manager).

Si el repositorio plantilla es privado, cada compañero debe ser agregado como colaborador desde *Settings → Collaborators* en GitHub.

## Verificación rápida

Al terminar, dentro de la carpeta destino:

```powershell
git remote -v
git status -sb
git ls-files dist\*.apk
```

La última línea debe mostrar `dist/SysAlquileres-Movil-v1.1.apk`, confirmando que el APK quedó versionado.
