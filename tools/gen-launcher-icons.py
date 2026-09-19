"""Deriva os ícones que faltavam a partir do ícone do launcher que já existe.

    python tools/gen-launcher-icons.py

Entrada:
    mipmap-xxxhdpi/ic_launcher.png   a arte original, 192x192 (não é tocada)

Saída (app/src/main/res/):
    mipmap-*/ic_launcher_round.png  ícone legado redondo, de verdade
    mipmap-*/ic_launcher_mark.png   o wordmark sozinho, branco sobre transparente

O XML que junta as camadas (mipmap-anydpi-v26/) é escrito à mão e não sai daqui.

O `ic_launcher_mark` serve às duas camadas de frente do ícone adaptativo: a
colorida (`drawable/ic_launcher_foreground.xml` pinta por cima com `tint`) e a
monocromática do tema do Android 13+ (que o próprio sistema tinge). Um bitmap em
tons de cinza com alfa, em vez de dois coloridos, tira ~50 KB do APK e deixa de
ter duas cópias do mesmo desenho para sair de sincronia.

Nada aqui redesenha o wordmark: ele é **recortado** do PNG original, pela
distância de cada pixel entre a cor de fundo e a cor de destaque. Redesenhar
com a fonte daria um "Fps" parecido, não o mesmo — e o ícone é a única peça do
app que a pessoa vê antes de instalar. De quebra, o script deixa de depender de
ter a Arial instalada.

O que cada arquivo resolve:

1. **O redondo tem que ser redondo.** Até aqui `ic_launcher_round.png` era cópia
   byte a byte do quadrado, então quem tem launcher de ícone redondo recebia um
   quadrado espremido dentro da máscara. Como a arte original já tem cantos
   arredondados e o círculo inscrito cabe inteiro dentro deles, recortar em
   círculo não perde um pixel do desenho.
2. **O adaptativo corta.** A tela de 108dp só garante os 66dp centrais: o que
   passar disso some em alguma máscara de launcher. O wordmark ocupa 50% da
   largura aqui, contra os ~76% do ícone quadrado, senão o "F" e o "s" perdem
   as pontas no aparelho que usa máscara de círculo.
3. **A camada de frente é transparente.** O fundo é a outra camada — é o
   deslocamento entre as duas que faz o efeito de paralaxe do launcher.
"""

import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SOURCE = os.path.join(RES, "mipmap-xxxhdpi", "ic_launcher.png")

# As duas cores da arte original, conferidas pixel a pixel no PNG.
ICON_BG = (26, 26, 24)
ACC = (232, 130, 90)

# Densidade -> (lado do ícone legado, lado da tela de 108dp do adaptativo).
DENSITIES = {
    "mdpi": (48, 108),
    "hdpi": (72, 162),
    "xhdpi": (96, 216),
    "xxhdpi": (144, 324),
    "xxxhdpi": (192, 432),
}

# Fração da largura da tela adaptativa que o wordmark pode ocupar. A zona segura
# é um círculo de 66dp em 108dp: com 50% de largura a diagonal da caixa do texto
# fica dentro dela com folga.
FG_WORDMARK = 0.50

# Abaixo de quanto da distância fundo→destaque um pixel ainda é fundo.
NOISE_FLOOR = 0.12

# Quanto do diâmetro o desenho ocupa dentro do ícone redondo.
ROUND_ART = 0.85

# Trabalha grande e reduz no fim: reduzir uma vez só, no tamanho final, evita
# empilhar perda de nitidez em cada etapa.
WORK = 1024


def wordmark_sprite(source):
    """Recorta o "Fps" do ícone: a máscara é o quanto cada pixel caminhou do
    fundo em direção ao laranja, o que preserva o antialiasing das curvas."""
    src = source.convert("RGBA")
    span = sum((a - b) ** 2 for a, b in zip(ACC, ICON_BG)) ** 0.5
    mask = Image.new("L", src.size, 0)
    pixels = src.load()
    out = mask.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = pixels[x, y]
            # Só pixel totalmente opaco. Os semitransparentes estão todos na
            # borda arredondada do ícone, e lá o RGB já vem misturado com preto
            # — entrariam na máscara como um anel fantasma nos quatro cantos.
            if a < 250:
                continue
            dist = ((r - ICON_BG[0]) ** 2 + (g - ICON_BG[1]) ** 2 + (b - ICON_BG[2]) ** 2) ** 0.5
            # O fundo do PNG não é 100% chapado (varia uns 4% de um canto ao
            # outro), então tudo abaixo do piso é considerado fundo. Acima
            # dele a rampa é reescalada, para a borda do "Fps" não perder o
            # antialiasing junto com o ruído.
            level = (dist / span - NOISE_FLOOR) / (1 - NOISE_FLOOR)
            out[x, y] = max(0, min(255, round(255 * level)))
    return mask.crop(mask.getbbox())


def mark(sprite, px):
    """O wordmark branco, centralizado numa tela transparente do tamanho do
    adaptativo. Sai em "LA" (cinza + alfa): a cor vem do `tint` no XML, então
    guardar três canais de laranja idêntico seria desperdício."""
    target_w = round(FG_WORDMARK * WORK)
    scaled = sprite.resize(
        (target_w, round(sprite.height * target_w / sprite.width)),
        Image.LANCZOS,
    )
    canvas = Image.new("LA", (WORK, WORK), (255, 0))
    # Centraliza pela caixa real do desenho, e não pela linha de base da fonte:
    # o "p" tem descida, e alinhar pela base joga o conjunto para cima.
    canvas.paste(Image.new("LA", scaled.size, (255, 255)),
                 ((WORK - scaled.width) // 2, (WORK - scaled.height) // 2), scaled)
    return canvas.resize((px, px), Image.LANCZOS)


def round_icon(source, px):
    """A arte original dentro de um círculo cheio — é o que o launcher de ícone
    redondo espera receber, já que ele não aplica máscara nenhuma.

    A arte encolhe um pouco antes de entrar. Recortada em tamanho cheio o "Fps"
    encosta na circunferência: o wordmark ocupa 87% da largura do quadrado, e a
    corda do círculo na altura dele mede 88%. Passa, mas raspando."""
    art = source.convert("RGBA")
    inner = round(WORK * ROUND_ART)
    circle = Image.new("L", (WORK, WORK), 0)
    ImageDraw.Draw(circle).ellipse([0, 0, WORK - 1, WORK - 1], fill=255)

    disc = Image.new("RGBA", (WORK, WORK), ICON_BG + (255,))
    scaled = art.resize((inner, inner), Image.LANCZOS)
    disc.alpha_composite(scaled, ((WORK - inner) // 2, (WORK - inner) // 2))
    # O recorte vem depois de compor: os cantos do quadrado reduzido ainda
    # passam da circunferência (a diagonal de 85% mede 120% do diâmetro), e sem
    # a máscara eles apareceriam como quatro orelhas para fora do círculo.
    disc.putalpha(circle)
    return disc.resize((px, px), Image.LANCZOS)


def main():
    source = Image.open(SOURCE)
    sprite = wordmark_sprite(source)
    print(f"wordmark recortado de {os.path.basename(SOURCE)}: {sprite.width}x{sprite.height} px")

    written = []
    for density, (legacy_px, adaptive_px) in DENSITIES.items():
        out = os.path.join(RES, f"mipmap-{density}")
        os.makedirs(out, exist_ok=True)
        files = {
            "ic_launcher_round.png": round_icon(source, legacy_px),
            "ic_launcher_mark.png": mark(sprite, adaptive_px),
        }
        for name, img in files.items():
            path = os.path.join(out, name)
            img.save(path, "PNG", optimize=True)
            written.append(path)

    total = sum(os.path.getsize(p) for p in written)
    print(f"{len(written)} arquivos, {total / 1024:.0f} KB no total")
    for p in sorted(written):
        print(f"  {os.path.relpath(p, ROOT)}  {Image.open(p).size[0]}px  {os.path.getsize(p) / 1024:.1f} KB")


if __name__ == "__main__":
    main()
