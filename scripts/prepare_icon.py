"""Recorta el icono generado, quita el fondo blanco y produce todos los formatos.

Uso:  python scripts/prepare_icon.py
Entrada: icono/Gemini_Generated_Image_*.jpg
Salida:  icono/icono-1024.png, icono/icono.ico, mipmaps de Android y
         src/main/resources/images/app-icon.png
"""
from pathlib import Path
from collections import deque

import numpy as np
from PIL import Image, ImageFilter, ImageDraw

RAIZ = Path(__file__).resolve().parents[1]
ICONO = RAIZ / "icono"
RES = RAIZ / "android" / "app" / "src" / "main" / "res"


def recortar(ruta: Path) -> Image.Image:
    imagen = Image.open(ruta).convert("RGB")
    pix = np.asarray(imagen).astype(np.int16)
    alto, ancho = pix.shape[:2]
    blanco = pix.min(axis=2) > 240

    # Difusión desde los bordes: solo se elimina el blanco CONECTADO con el exterior.
    visitado = np.zeros((alto, ancho), dtype=bool)
    cola = deque()
    for x in range(ancho):
        for y in (0, alto - 1):
            if blanco[y, x] and not visitado[y, x]:
                visitado[y, x] = True
                cola.append((y, x))
    for y in range(alto):
        for x in (0, ancho - 1):
            if blanco[y, x] and not visitado[y, x]:
                visitado[y, x] = True
                cola.append((y, x))

    while cola:
        y, x = cola.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < alto and 0 <= nx < ancho and blanco[ny, nx] and not visitado[ny, nx]:
                visitado[ny, nx] = True
                cola.append((ny, nx))

    alfa = np.where(visitado, 0, 255).astype(np.uint8)

    # Suaviza el borde recortado (antialiasing) sin tocar blancos internos.
    dilatado = np.asarray(
        Image.fromarray((visitado * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(9))
    ) > 127
    cerca_blanco = pix.min(axis=2) > 220
    borde = dilatado & ~visitado & cerca_blanco
    alfa[borde] = np.clip((255 - pix.min(axis=2)[borde]) * 9, 0, 255).astype(np.uint8)

    rgba = np.dstack([np.asarray(imagen), alfa])
    salida = Image.fromarray(rgba, "RGBA")

    caja = salida.getbbox()
    recortada = salida.crop(caja)
    lado = max(recortada.size)
    margen = int(lado * 0.02)
    lado += margen * 2
    cuadrada = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
    cuadrada.paste(recortada, ((lado - recortada.width) // 2, (lado - recortada.height) // 2))
    return cuadrada.resize((1024, 1024), Image.LANCZOS)


def guardar(master: Image.Image) -> None:
    master.save(ICONO / "icono-1024.png")

    # Windows: ICO multi-tamaño
    master.save(ICONO / "icono.ico",
                sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])

    # Icono de ventana JavaFX
    carpeta = RAIZ / "src" / "main" / "resources" / "images"
    carpeta.mkdir(parents=True, exist_ok=True)
    master.resize((256, 256), Image.LANCZOS).save(carpeta / "app-icon.png")

    # Android: launcher + variante redonda + capa frontal adaptativa
    densidades = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for nombre, tam in densidades.items():
        destino = RES / f"mipmap-{nombre}"
        destino.mkdir(parents=True, exist_ok=True)
        icono = master.resize((tam, tam), Image.LANCZOS)
        icono.save(destino / "ic_launcher.png")

        mascara = Image.new("L", (tam, tam), 0)
        ImageDraw.Draw(mascara).ellipse((0, 0, tam - 1, tam - 1), fill=255)
        redonda = icono.copy()
        redonda.putalpha(mascara)
        redonda.save(destino / "ic_launcher_round.png")

    frontal = master.resize((432, 432), Image.LANCZOS)
    (RES / "mipmap-xxxhdpi").mkdir(parents=True, exist_ok=True)
    frontal.save(RES / "mipmap-xxxhdpi" / "ic_launcher_foreground.png")

    v26 = RES / "mipmap-anydpi-v26"
    v26.mkdir(parents=True, exist_ok=True)
    (v26 / "ic_launcher.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/primario" />\n'
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
        '</adaptive-icon>\n', encoding="utf-8")
    (v26 / "ic_launcher_round.xml").write_text(
        (v26 / "ic_launcher.xml").read_text(encoding="utf-8"), encoding="utf-8")


def main() -> None:
    fuentes = sorted(ICONO.glob("*.jpg")) + sorted(ICONO.glob("*.png"))
    fuentes = [f for f in fuentes if "icono-1024" not in f.name]
    if not fuentes:
        raise SystemExit("No hay imagen en la carpeta icono/")
    recortado = recortar(fuentes[0])
    guardar(recortado)
    print("Fuente:", fuentes[0].name)
    print("Master: icono/icono-1024.png")
    print("ICO: icono/icono.ico")
    print("Android: mipmap-* generados")


if __name__ == "__main__":
    main()
