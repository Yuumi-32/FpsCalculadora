package com.fps.calculadora.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.fps.calculadora.R
import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.CalcResult
import com.fps.calculadora.core.GameDatabase
import com.fps.calculadora.core.PerformanceTier
import com.fps.calculadora.core.PsuEstimate
import com.fps.calculadora.core.monitorPercent
import com.fps.calculadora.core.shortCpuName

/** Tamanho do card, em pixels — o 4:5 de feed do original (`index.html:3023`). */
const val SHARE_CARD_WIDTH = 1080
const val SHARE_CARD_HEIGHT = 1350

// As mesmas cores do tema, em ARGB: o card é desenhado fora do Compose, então
// não dá para ler os tokens de `FpsColors` sem converter cada um.
private const val BG = 0xFF0D0C0A.toInt()
private const val TX1 = 0xFFF2F0E6.toInt()
private const val TX2 = 0xFFB3B0A2.toInt()
private const val TX3 = 0xFF7D7A70.toInt()
private const val ACC = 0xFFE8825A.toInt()
private const val OK = 0xFFA9D562.toInt()
private const val BAD = 0xFFF48A8A.toInt()
private const val WARN = 0xFFF0A836.toInt()

/**
 * Desenha o card de resultado que vai para o compartilhamento — porta o
 * `buildShareCanvas()` do `index.html` (:3019), traço por traço, do `<canvas>`
 * do navegador para o `Canvas` do Android.
 *
 * É uma imagem, e não uma captura da tela, porque o que se manda para um amigo
 * precisa caber num feed e continuar legível: 1080×1350, número grande, a build
 * escrita por extenso e a ressalva de que é estimativa impressa junto. Uma
 * captura recortaria a ressalva fora.
 */
fun renderShareCard(
    context: Context,
    db: GameDatabase,
    state: BuildState,
    result: CalcResult,
    psu: PsuEstimate,
): Bitmap {
    val w = SHARE_CARD_WIDTH.toFloat()
    val h = SHARE_CARD_HEIGHT.toFloat()
    val bitmap = createBitmap(SHARE_CARD_WIDTH, SHARE_CARD_HEIGHT)
    val canvas = Canvas(bitmap)

    val sansBold = Typeface.create("sans-serif", Typeface.BOLD)
    val sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val mono = ResourcesCompat.getFont(context, R.font.roboto_mono_bold)
        ?: Typeface.MONOSPACE
    val monoRegular = ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
        ?: Typeface.MONOSPACE

    val game = db.game(state.gameId)
    val cpu = db.cpu(state.cpuId)
    val gpu = db.gpu(state.gpuId)
    val tier = PerformanceTier.forFps(result.avg)
    val accent = when (tier.colorToken) {
        "bad" -> BAD
        "warn" -> WARN
        else -> OK
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Fundo + o brilho laranja que desce do topo, igual ao `--hero-glow`.
    paint.color = BG
    canvas.drawRect(0f, 0f, w, h, paint)
    paint.shader = RadialGradient(
        w / 2f, -80f, 900f,
        intArrayOf(0x38E8825A, 0x00E8825A), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w, h, paint)
    paint.shader = null

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    paint.color = 0x1AF4F0E4
    canvas.drawRoundRect(RectF(24f, 24f, w - 24f, h - 24f), 40f, 40f, paint)
    paint.style = Paint.Style.FILL

    // Cabeçalho: ponto laranja, nome do app e a resolução/monitor à direita.
    paint.color = ACC
    canvas.drawCircle(96f, 106f, 9f, paint)
    val header = textPaint(sansBold, 26f, TX3)
    canvas.drawText("ESTIMADOR DE FPS", 122f, 115f, header)
    header.textAlign = Paint.Align.RIGHT
    canvas.drawText("${state.resolution.label} · ${state.monitorHz} Hz", w - 92f, 115f, header)

    val maxLine = w - 184f
    val title = textPaint(sansBold, 56f, TX1)
    canvas.drawText(fit(game.name, title, maxLine), 92f, 214f, title)
    val hardware = textPaint(sansMedium, 33f, TX2)
    canvas.drawText(fit("${cpu.name} + ${gpu.name}", hardware, maxLine), 92f, 270f, hardware)
    val presetName = db.preset(state.preset)?.name.orEmpty()
    val ram = db.constants.ramLabels[state.ram] ?: state.ram
    val platform = textPaint(sansMedium, 28f, TX3)
    canvas.drawText(
        fit("${db.mobo(state.moboId).name} · $ram · $presetName", platform, maxLine),
        92f, 316f, platform,
    )

    // O número, que é o motivo do card existir.
    val big = textPaint(mono, 270f, accent, Paint.Align.CENTER)
    canvas.drawText(result.avg.toString(), w / 2f, 660f, big)
    canvas.drawText("FPS MÉDIO", w / 2f, 712f, textPaint(sansBold, 28f, TX3, Paint.Align.CENTER))
    canvas.drawText(
        "${result.avgLow} – ${result.avgHigh} fps",
        w / 2f, 752f, textPaint(monoRegular, 26f, TX3, Paint.Align.CENTER),
    )

    // Badge do nível de desempenho.
    val badgeLabel = tier.label.uppercase()
    val badgePaint = textPaint(sansBold, 30f, accent, Paint.Align.CENTER)
    val badgeWidth = badgePaint.measureText(badgeLabel) + 72f
    paint.color = (accent and 0x00FFFFFF) or 0x29000000
    canvas.drawRoundRect(
        RectF(w / 2f - badgeWidth / 2f, 786f, w / 2f + badgeWidth / 2f, 848f), 31f, 31f, paint,
    )
    canvas.drawText(badgeLabel, w / 2f, 828f, badgePaint)

    // As três colunas de estatística, na mesma ordem da tela.
    val columns = listOf(
        Triple("1% LOW", result.min.toString(), "${result.minLow} – ${result.minHigh}") to BAD,
        Triple(
            "VS MONITOR",
            "${monitorPercent(result.avg, state.monitorHz)}%",
            "de ${state.monitorHz} Hz",
        ) to TX1,
        Triple("MÁXIMO", result.max.toString(), "${result.maxLow} – ${result.maxHigh}") to OK,
    )
    columns.forEachIndexed { index, (column, color) ->
        val (label, value, sub) = column
        val cx = w / 2f + (index - 1) * 310f
        canvas.drawText(label, cx, 956f, textPaint(sansBold, 23f, TX3, Paint.Align.CENTER))
        canvas.drawText(value, cx, 1030f, textPaint(mono, 62f, color, Paint.Align.CENTER))
        canvas.drawText(sub, cx, 1068f, textPaint(monoRegular, 22f, TX3, Paint.Align.CENTER))
    }

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    paint.color = 0x24F4F0E4
    canvas.drawLine(92f, 1128f, w - 92f, 1128f, paint)
    paint.style = Paint.Style.FILL

    canvas.drawText(
        "Fonte recomendada: ${psu.recommended} W",
        w / 2f, 1188f, textPaint(sansBold, 30f, TX2, Paint.Align.CENTER),
    )
    canvas.drawText(
        "Estimativa · base ${db.meta.version} (${db.meta.updated}) · varia por cena e drivers",
        w / 2f, 1234f, textPaint(sansMedium, 24f, TX3, Paint.Align.CENTER),
    )
    return bitmap
}

/**
 * Nome do arquivo PNG. Leva CPU e GPU em vez do `fps-estimado.png` fixo do
 * original: na galeria e na pasta de downloads, dois cards de builds
 * diferentes precisam ser distinguíveis sem abrir os dois.
 */
fun shareCardFileName(db: GameDatabase, state: BuildState): String {
    val slug = "${shortCpuName(db.cpu(state.cpuId).name)}-${db.gpu(state.gpuId).name}"
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return "fps-estimado-$slug.png"
}

private fun textPaint(
    typeface: Typeface,
    size: Float,
    color: Int,
    align: Paint.Align = Paint.Align.LEFT,
): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.typeface = typeface
    textSize = size
    this.color = color
    textAlign = align
}

/** Corta com reticências o que não couber na largura — o `fitTxt()` do original. */
private fun fit(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var candidate = text
    while (candidate.length > 2 && paint.measureText("$candidate…") > maxWidth) {
        candidate = candidate.dropLast(1)
    }
    return "$candidate…"
}
