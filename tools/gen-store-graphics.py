"""Gera os gráficos da ficha da Google Play a partir do código, para poder
regerar tudo depois de mexer em cor, texto ou fonte.

    python tools/gen-store-graphics.py

Saída (docs/play/graficos/):
    icone-512.png                 ícone da loja, 512x512, quadrado cheio
    feature-graphic-1024x500.png  gráfico de destaque, sem transparência

A Play arredonda os cantos do ícone sozinha: o arquivo tem que ser um
quadrado inteiro, opaco, sem cantos já arredondados. O feature graphic pode
ser cortado nas laterais em algumas telas, então nada importante encosta na
borda.

Tudo é desenhado em 4x e reduzido no fim — o PIL não tem antialiasing nas
formas, e sem isso as curvas do medidor saem serrilhadas.
"""

import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

SS = 4  # supersampling

BG0 = (13, 12, 10)
BG1 = (22, 21, 17)
BG2 = (30, 29, 24)
ICON_BG = (26, 26, 24)
TX1 = (242, 240, 230)
TX2 = (184, 179, 164)
TX3 = (131, 126, 113)
ACC = (232, 130, 90)
ACC_DEEP = (194, 96, 51)
LINE = (48, 46, 40)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "play", "graficos")

# Arial Bold é o que mais se aproxima do wordmark do ícone do launcher.
FONT_BOLD = "C:/Windows/Fonts/arialbd.ttf"
FONT_REG = "C:/Windows/Fonts/arial.ttf"
FONT_MONO = os.path.join(ROOT, "app", "src", "main", "res", "font", "roboto_mono_bold.ttf")
FONT_MONO_REG = os.path.join(ROOT, "app", "src", "main", "res", "font", "roboto_mono_medium.ttf")


def font(path, size):
    return ImageFont.truetype(path, int(size * SS))


def vertical_gradient(size, top, bottom):
    """Gradiente vertical suave: 2 pixels esticados fazem o degradê sozinhos."""
    w, h = size
    base = Image.new("RGB", (1, 2))
    base.putpixel((0, 0), top)
    base.putpixel((0, 1), bottom)
    return base.resize((w, h), Image.BILINEAR)


def glow(size, center, radius, color, opacity):
    """Brilho radial: elipse borrada por cima do fundo."""
    w, h = size
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    cx, cy = center
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=int(255 * opacity))
    mask = mask.filter(ImageFilter.GaussianBlur(radius * 0.55))
    layer = Image.new("RGB", (w, h), color)
    return layer, mask


def text_at(draw, xy, text, fnt, fill, anchor="la", spacing_px=0):
    """Desenha texto opcionalmente com espaçamento extra entre letras."""
    if not spacing_px:
        draw.text(xy, text, font=fnt, fill=fill, anchor=anchor)
        return
    total = sum(draw.textlength(c, font=fnt) + spacing_px * SS for c in text) - spacing_px * SS
    x, y = xy
    if anchor[0] == "m":
        x -= total / 2
    elif anchor[0] == "r":
        x -= total
    for c in text:
        draw.text((x, y), c, font=fnt, fill=fill, anchor="l" + anchor[1])
        x += draw.textlength(c, font=fnt) + spacing_px * SS


def fit_font(draw, text, path, target_w):
    """Maior corpo de fonte em que o texto ainda cabe na largura pedida."""
    size = 10
    while size < 400:
        f = font(path, size)
        if draw.textlength(text, font=f) >= target_w * SS:
            return font(path, size - 1)
        size += 1
    return font(path, size)


# ─────────────────────────────── ícone 512 ───────────────────────────────

def build_icon(px=512):
    w = h = px * SS
    img = vertical_gradient((w, h), (34, 33, 28), (18, 17, 14))
    layer, mask = glow((w, h), (w * 0.5, h * 0.30), w * 0.55, (232, 130, 90), 0.10)
    img = Image.composite(layer, img, mask)

    d = ImageDraw.Draw(img)
    f = fit_font(d, "Fps", FONT_BOLD, px * 0.76)
    # Centro óptico: "Fps" tem descida no p, então centraliza pela caixa real.
    bb = d.textbbox((0, 0), "Fps", font=f)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    d.text((w / 2 - tw / 2 - bb[0], h / 2 - th / 2 - bb[1]), "Fps", font=f, fill=ACC)

    return img.resize((px, px), Image.LANCZOS).convert("RGB")


# ──────────────────────── feature graphic 1024x500 ────────────────────────

def rounded_rect(draw, box, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius * SS, fill=fill, outline=outline, width=int(width * SS))


def build_feature(w_px=1024, h_px=500):
    w, h = w_px * SS, h_px * SS
    img = vertical_gradient((w, h), BG2, BG0)
    layer, mask = glow((w, h), (w * 0.82, h * 0.52), w * 0.26, ACC, 0.15)
    img = Image.composite(layer, img, mask)
    layer, mask = glow((w, h), (w * 0.08, h * 0.10), w * 0.28, (90, 155, 230), 0.06)
    img = Image.composite(layer, img, mask)
    d = ImageDraw.Draw(img)

    # ── selo do app (mesmo wordmark do ícone) ──
    bx, by, bs = 64, 60, 96
    rounded_rect(d, [bx * SS, by * SS, (bx + bs) * SS, (by + bs) * SS], 24, fill=ICON_BG, outline=LINE, width=2)
    f = fit_font(d, "Fps", FONT_BOLD, bs * 0.68)
    bb = d.textbbox((0, 0), "Fps", font=f)
    d.text(((bx + bs / 2) * SS - (bb[2] - bb[0]) / 2 - bb[0],
            (by + bs / 2) * SS - (bb[3] - bb[1]) / 2 - bb[1]), "Fps", font=f, fill=ACC)

    # ── título e subtítulo ──
    d.text((184 * SS, 74 * SS), "FPS Calculadora", font=font(FONT_BOLD, 54), fill=TX1)
    d.text((186 * SS, 136 * SS), "Quanto FPS o seu PC entrega, jogo a jogo",
           font=font(FONT_REG, 24), fill=TX2)

    # ── chips ──
    chips = ["100% OFFLINE", "SEM ANÚNCIOS", "RYZEN · INTEL · GEFORCE · RADEON"]
    fc = font(FONT_MONO_REG, 14)
    x = 66
    for c in chips:
        tw = d.textlength(c, font=fc) / SS
        cw = tw + 30
        rounded_rect(d, [x * SS, 218 * SS, (x + cw) * SS, 254 * SS], 18, fill=BG1, outline=LINE, width=2)
        d.text(((x + cw / 2) * SS, 236 * SS), c, font=fc, fill=TX2, anchor="mm")
        x += cw + 12

    # ── linhas de resultado (imitam os cards do app) ──
    rows = [("1080p · Ultra", "142", "FPS"), ("1440p · Alto", "118", "FPS"), ("4K · Médio", "76", "FPS")]
    y = 292
    for label, value, unit in rows:
        rounded_rect(d, [66 * SS, y * SS, 470 * SS, (y + 44) * SS], 12, fill=BG1, outline=LINE, width=2)
        d.text((84 * SS, (y + 22) * SS), label, font=font(FONT_REG, 19), fill=TX2, anchor="lm")
        d.text((432 * SS, (y + 22) * SS), value, font=font(FONT_MONO, 22), fill=ACC, anchor="rm")
        d.text((452 * SS, (y + 22) * SS), unit, font=font(FONT_MONO_REG, 12), fill=TX3, anchor="mm")
        y += 52

    # ── medidor à direita ──
    cx, cy, r = 836, 252, 116
    box = [(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS]
    d.arc(box, start=135, end=405, fill=BG2, width=int(16 * SS))
    d.arc(box, start=135, end=135 + 270 * 0.62, fill=ACC, width=int(16 * SS))
    d.text((cx * SS, (cy - 14) * SS), "144", font=font(FONT_MONO, 62), fill=TX1, anchor="mm")
    text_at(d, (cx * SS, (cy + 40) * SS), "FPS MÉDIO", font(FONT_MONO_REG, 14), TX3, anchor="mm", spacing_px=2)

    return img.resize((w_px, h_px), Image.LANCZOS).convert("RGB")


def main():
    os.makedirs(OUT, exist_ok=True)
    icon = build_icon()
    icon.save(os.path.join(OUT, "icone-512.png"), "PNG")
    feat = build_feature()
    feat.save(os.path.join(OUT, "feature-graphic-1024x500.png"), "PNG")
    for name in ("icone-512.png", "feature-graphic-1024x500.png"):
        p = os.path.join(OUT, name)
        print(f"{name}: {Image.open(p).size[0]}x{Image.open(p).size[1]}, {os.path.getsize(p) / 1024:.0f} KB")


if __name__ == "__main__":
    main()
