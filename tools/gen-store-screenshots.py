"""Gera as capturas de tela da ficha da Play a partir do próprio HTML do app.

    python tools/gen-store-screenshots.py

Saída: docs/play/screenshots/*.png, 1080x1920 (9:16, o limite da Play é o lado
maior valer no máximo o dobro do menor).

Como funciona: o `index.html` do app é copiado para uma pasta temporária, uma
vez por tela, com um script injetado que semeia o localStorage (estado da
build, histórico, flag de onboarding) e clica na aba desejada. Um servidor
HTTP local serve os arquivos — em `file://` o Chrome bloqueia o localStorage e
a semeadura seria ignorada em silêncio. O Chrome headless então tira a foto e o
resultado é reduzido para 1080x1920.

Cuidado com o `--window-size`: no headless novo do Windows a captura sai
exatamente do tamanho pedido, mas a **página é desenhada na largura mínima da
janela (~490 CSS)** por menor que seja o pedido — pedir 360 de largura só
recorta a captura e o resultado sai com tudo cortado do lado direito. Por isso
a viewport é 490x872, que já é 9:16, e a redução para 1080x1920 vem depois.

Rolar a página também não funciona: qualquer `scrollTo`/`scrollIntoView` antes
da captura devolve uma imagem preta. Para fotografar o pé de uma tela, esconda
os cards de cima (veja a captura 06) em vez de rolar.

As fontes vêm do Google Fonts porque o app usa as fontes do sistema (Roboto e
Roboto Mono no Android); sem isso o Windows renderiza com Segoe UI e Consolas
e a captura não fica igual ao aparelho.
"""

import http.server
import json
import os
import shutil
import socket
import subprocess
import tempfile
import threading

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main", "assets", "www", "index.html")
OUT = os.path.join(ROOT, "docs", "play", "screenshots")

CHROME_CANDIDATES = [
    r"C:/Program Files/Google/Chrome/Application/chrome.exe",
    r"C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    r"C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
]

CSS_W, CSS_H = 490, 872  # viewport útil (9:16); 490 é o mínimo do headless
DSF = 3                  # captura em 1470x2616 e reduz para o tamanho final
FINAL_W, FINAL_H = 1080, 1920

# Build de referência do app (RTX 5070 + Ryzen 7 5700X), igual ao estado padrão.
BUILD_1440 = {"game": 2, "cpu": 16, "gpu": 45, "mobo": 3, "ram": "ddr4_32",
              "res": "1440p", "preset": "ultra", "rt": "off", "fg": 1,
              "dlss": 1.0, "monHz": 144, "hoursDay": 3, "tariff": 0.95}
# R5 5500 + RTX 4080 em 1080p: fica preso na CPU, então a tela de upgrade
# mostra ganho de verdade nas duas listas em vez de repetir o mesmo número.
BUILD_1080 = {**BUILD_1440, "gpu": 38, "cpu": 12, "res": "1080p", "monHz": 165}
BUILD_4K = {**BUILD_1440, "gpu": 48, "cpu": 20, "res": "4k", "preset": "high", "monHz": 120}

HIST = [
    {"id": 1756200000000, "ts": 1756200000000, "s": BUILD_1080},
    {"id": 1756100000000, "ts": 1756100000000, "s": BUILD_4K},
]

SHOTS = [
    dict(file="01-calculadora.png", tab="calc", state=BUILD_1440, hist=HIST, onboarded=True),
    dict(file="02-jogos.png", tab="games", state=BUILD_1440, hist=HIST, onboarded=True),
    dict(file="03-upgrade.png", tab="upg", state=BUILD_1080, hist=HIST, onboarded=True),
    # Escolhe o build B pelo mesmo caminho do usuário: abre o seletor e clica
    # na primeira opção. Mexer na variável compBId direto não pega, porque ela
    # é um `let` no escopo do script da página.
    dict(file="04-comparar.png", tab="comp", state=BUILD_1440, hist=HIST, onboarded=True,
         js="document.getElementById('slotB').click();"
            "var o=document.querySelector('#sheetBody .opt');if(o)o.click();"),
    dict(file="05-historico.png", tab="hist", state=BUILD_1440, hist=HIST, onboarded=True),
    # Rolar a página deixa a captura preta neste modo do headless, então a
    # parte de baixo da calculadora aparece escondendo os cards de cima.
    dict(file="06-pecas.png", tab="calc", state=BUILD_1440, hist=HIST, onboarded=True,
         js="['hero','resCmp','mini'].forEach(function(id){"
            "var e=document.getElementById(id);if(e)e.style.display='none';});"),
    # O seletor de GPU mostra o tamanho do catálogo melhor que qualquer texto.
    dict(file="07-catalogo.png", tab="calc", state=BUILD_1440, hist=HIST, onboarded=True,
         js="document.getElementById('rowGPU').click();"
            "var s=document.getElementById('sheetSearch');s.value='RTX 50';"
            "s.dispatchEvent(new Event('input'));"),
    dict(file="08-boas-vindas.png", tab=None, state=BUILD_1440, hist=[], onboarded=False),
]

FONTS = ('<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>'
         '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?'
         'family=Roboto:wght@400;500;700;900&family=Roboto+Mono:wght@400;500;700&display=swap">'
         '<style>:root{--sans:"Roboto",sans-serif !important;'
         '--mono:"Roboto Mono",monospace !important}</style>')


def head_script(shot):
    seed = {"fps:state:v2": json.dumps(shot["state"]), "fps:hist:v1": json.dumps(shot["hist"])}
    if shot["onboarded"]:
        seed["fps:onboard:v1"] = "1"
    js = "".join(f"localStorage.setItem({json.dumps(k)},{json.dumps(v)});" for k, v in seed.items())
    if not shot["onboarded"]:
        # Todas as capturas usam o mesmo perfil do Chrome e a mesma origem, então
        # a flag gravada por uma tela anterior sobrevive: sem apagar, o
        # onboarding nunca aparece.
        js += "localStorage.removeItem('fps:onboard:v1');"
    return "<script>try{" + js + "}catch(e){}</script>"


def body_script(shot):
    tab, js = shot.get("tab"), shot.get("js")
    steps = []
    if tab:
        steps.append(f'var b=document.querySelector(\'[data-tab="{tab}"]\');if(b)b.click();')
    if js:
        steps.append(js)
    if not steps:
        return ""
    # Dois quadros de folga: o app renderiza a aba no clique e só depois o
    # layout estabiliza — mexer no DOM antes disso o render seguinte desfaz.
    return ("<script>window.addEventListener('load',function(){"
            "requestAnimationFrame(function(){requestAnimationFrame(function(){"
            + "".join(steps) + "})})});</script>")


def find_chrome():
    for p in CHROME_CANDIDATES:
        if os.path.exists(p):
            return p
    raise SystemExit("Chrome ou Edge não encontrado — ajuste CHROME_CANDIDATES.")


def free_port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def serve(directory, port):
    handler = lambda *a, **kw: http.server.SimpleHTTPRequestHandler(*a, directory=directory, **kw)
    httpd = http.server.ThreadingHTTPServer(("127.0.0.1", port), handler)
    threading.Thread(target=httpd.serve_forever, daemon=True).start()
    return httpd


def main():
    html = open(SRC, encoding="utf-8").read()
    os.makedirs(OUT, exist_ok=True)
    chrome = find_chrome()
    tmp = tempfile.mkdtemp(prefix="fps-shots-")
    profile = os.path.join(tmp, "profile")
    port = free_port()
    httpd = serve(tmp, port)
    try:
        for shot in SHOTS:
            page = shot["file"].replace(".png", ".html")
            doc = html.replace("</head>", FONTS + head_script(shot) + "</head>", 1)
            doc = doc.replace("</body>", body_script(shot) + "</body>", 1)
            with open(os.path.join(tmp, page), "w", encoding="utf-8") as f:
                f.write(doc)
            dest = os.path.join(OUT, shot["file"])
            subprocess.run([
                chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                "--no-first-run", "--no-default-browser-check",
                f"--user-data-dir={profile}",
                f"--force-device-scale-factor={DSF}",
                f"--window-size={CSS_W},{CSS_H}",
                "--virtual-time-budget=6000",
                f"--screenshot={dest}",
                f"http://127.0.0.1:{port}/{page}",
            ], check=True, capture_output=True)
            from PIL import Image
            with Image.open(dest) as im:
                if im.size != (CSS_W * DSF, CSS_H * DSF):
                    raise SystemExit(f"{shot['file']} saiu {im.size}")
                im.convert("RGB").resize((FINAL_W, FINAL_H), Image.LANCZOS).save(dest, "PNG")
            print(f"{shot['file']}: {FINAL_W}x{FINAL_H}, {os.path.getsize(dest)/1024:.0f} KB")
    finally:
        httpd.shutdown()
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
