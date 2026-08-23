package com.fps.calculadora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.HzMarker
import com.fps.calculadora.core.PerformanceTier
import com.fps.calculadora.core.monitorFraction
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.MonoNumber
import com.fps.calculadora.ui.theme.RobotoMono
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometria do arco, idêntica ao SVG do `index.html` (`GAUGE` :2068).
 * As medidas são as do `viewBox` 320×212 e escalam junto com a largura real.
 */
private object GaugeSpec {
    const val VIEW_W = 320f
    const val VIEW_H = 212f
    const val CX = 160f
    const val CY = 158f
    const val R = 120f
    const val START_DEG = -110f
    const val END_DEG = 110f
    const val STROKE = 13f

    /** Rótulos "0" e "144 Hz" ficam na base, fora do arco (`y=209` no SVG). */
    const val LABEL_Y = 209f

    /** O centro do texto fica a 27% da altura (`.gauge-center { top: 27% }`). */
    const val CENTER_TOP_RATIO = 0.27f

    const val SWEEP_DEG = END_DEG - START_DEG
}

/**
 * Ângulo do gauge (0° no topo, positivo no sentido horário) para coordenadas do
 * viewBox — o `gpol()` do JS.
 */
private fun polar(angleDeg: Float, radius: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        x = GaugeSpec.CX + radius * sin(rad).toFloat(),
        y = GaugeSpec.CY - radius * cos(rad).toFloat(),
    )
}

/**
 * O gauge semicircular do resultado: arco de −110° a +110° preenchido na
 * proporção do refresh do monitor, com marcadores de Hz, número animado e selo
 * da faixa de desempenho.
 *
 * Nenhum número é decidido aqui: [fps], [tier] e a fração vêm do `:core`.
 *
 * @param previousFps último valor exibido, para a seta de variação. `null` na
 *   primeira renderização, quando não há de onde variar.
 * @param animated `false` desliga as transições, para quando o aparelho está com
 *   animações desativadas (equivalente ao `prefers-reduced-motion` do CSS).
 */
@Composable
fun FpsGauge(
    fps: Int,
    rangeLow: Int,
    rangeHigh: Int,
    monitorHz: Int,
    hzMarkers: List<HzMarker>,
    tier: PerformanceTier,
    modifier: Modifier = Modifier,
    previousFps: Int? = null,
    animated: Boolean = true,
) {
    val fraction = monitorFraction(fps, monitorHz).toFloat()

    // Mesmas durações do CSS: 0,55 s no arco, 0,4 s na cor, 0,42 s no número.
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(if (animated) 550 else 0, easing = FastOutSlowInEasing),
        label = "gaugeFraction",
    )
    val animatedColor by animateColorAsState(
        targetValue = tier.color(),
        animationSpec = tween(if (animated) 400 else 0),
        label = "gaugeColor",
    )
    val animatedFps by animateFloatAsState(
        targetValue = fps.toFloat(),
        animationSpec = tween(if (animated) 420 else 0, easing = EaseOutCubic),
        label = "gaugeNumber",
    )

    val textMeasurer = rememberTextMeasurer()

    // `max-width: 320px` no CSS — o gauge não estica além do desenho original.
    BoxWithConstraints(modifier.widthIn(max = 320.dp)) {
        val gaugeHeight = maxWidth * (GaugeSpec.VIEW_H / GaugeSpec.VIEW_W)

        Canvas(Modifier.fillMaxWidth().height(gaugeHeight)) {
            val scale = size.width / GaugeSpec.VIEW_W
            drawGaugeArc(scale, 1f, FpsColors.Bg3)
            drawGaugeArc(scale, animatedFraction, animatedColor)
            drawTicks(scale, monitorHz, hzMarkers, textMeasurer)
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = gaugeHeight * GaugeSpec.CENTER_TOP_RATIO),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            GaugeNumber(
                displayed = animatedFps,
                target = fps,
                previousFps = previousFps,
            )
            Text(
                "FPS MÉDIO",
                color = FpsColors.Tx3,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.14.em,
            )
            Text(
                "$rangeLow – $rangeHigh fps",
                color = FpsColors.Tx3,
                fontSize = 11.sp,
                fontFamily = RobotoMono,
            )
            PerformanceBadge(
                tier = tier,
                animated = animated,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** O número grande, com a seta de variação flutuando à direita. */
@Composable
private fun GaugeNumber(displayed: Float, target: Int, previousFps: Int?) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = "${displayed.toInt()}",
            style = MonoNumber,
            color = FpsColors.Tx1,
            fontSize = 54.sp,
        )
        if (previousFps != null && previousFps != target) {
            val diff = target - previousFps
            Text(
                text = (if (diff > 0) "↑" else "↓") + abs(diff),
                color = if (diff > 0) FpsColors.Ok else FpsColors.Bad,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RobotoMono,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(start = 7.dp),
            )
        }
    }
}

/** Selo da faixa: pílula que pulsa nas faixas altas (`.badge.glow`, 2,8 s). */
@Composable
fun PerformanceBadge(
    tier: PerformanceTier,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val pulse = if (tier.glow && animated) {
        val transition = rememberInfiniteTransition(label = "badgePulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "badgeGlow",
        ).value
    } else {
        0f
    }

    val background = tier.backgroundColor()
    Text(
        text = tier.label.uppercase(),
        color = tier.color(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.06.em,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(
                color = background.copy(alpha = background.alpha + 0.12f * pulse),
                shape = RoundedCornerShape(100.dp),
            )
            .padding(horizontal = 13.dp, vertical = 4.dp),
    )
}

/* ── Desenho ─────────────────────────────────────────────────────────── */

/** Arco de raio fixo, preenchido de `START_DEG` até a fração pedida. */
private fun DrawScope.drawGaugeArc(scale: Float, fraction: Float, color: Color) {
    if (fraction <= 0f) return
    val radius = GaugeSpec.R * scale
    drawArc(
        color = color,
        // O ângulo do gauge tem 0° no topo; o do Canvas, às 3 horas.
        startAngle = GaugeSpec.START_DEG - 90f,
        sweepAngle = GaugeSpec.SWEEP_DEG * fraction.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(
            x = (GaugeSpec.CX - GaugeSpec.R) * scale,
            y = (GaugeSpec.CY - GaugeSpec.R) * scale,
        ),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = GaugeSpec.STROKE * scale, cap = StrokeCap.Round),
    )
}

/** Traços de Hz do monitor, mais os rótulos das duas pontas do arco. */
private fun DrawScope.drawTicks(
    scale: Float,
    monitorHz: Int,
    markers: List<HzMarker>,
    textMeasurer: TextMeasurer,
) {
    if (monitorHz <= 0) return

    for (marker in markers) {
        val angle = GaugeSpec.START_DEG +
            (marker.value.toFloat() / monitorHz) * GaugeSpec.SWEEP_DEG
        drawLine(
            // O traço do próprio refresh do monitor é mais claro que os demais.
            color = if (marker.value == monitorHz) FpsColors.Tx3 else FpsColors.Line2,
            start = polar(angle, GaugeSpec.R - 15f).scaled(scale),
            end = polar(angle, GaugeSpec.R - 8f).scaled(scale),
            strokeWidth = 1.5f * scale,
        )
    }

    // `toSp()` converte pixels do desenho para a unidade do texto; usar `.sp`
    // direto aplicaria a densidade da tela duas vezes.
    val labelStyle = TextStyle(
        color = FpsColors.Tx3,
        fontSize = (9.5f * scale).toSp(),
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium,
    )
    val labels = listOf(
        GaugeSpec.START_DEG to "0",
        GaugeSpec.END_DEG to "$monitorHz Hz",
    )
    for ((angle, label) in labels) {
        val anchor = polar(angle, GaugeSpec.R).scaled(scale)
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = anchor.x - measured.size.width / 2f,
                y = GaugeSpec.LABEL_Y * scale - measured.size.height,
            ),
        )
    }
}

private fun Offset.scaled(scale: Float) = Offset(x * scale, y * scale)

/* ── Ponte entre os tokens do :core e as cores do tema ───────────────── */

/** Resolve o `colorToken` que o `:core` devolve para a cor real do tema. */
fun PerformanceTier.color(): Color = when (colorToken) {
    "ok" -> FpsColors.Ok
    "warn" -> FpsColors.Warn
    "bad" -> FpsColors.Bad
    else -> FpsColors.Tx1
}

fun PerformanceTier.backgroundColor(): Color = when (colorToken) {
    "ok" -> FpsColors.OkBg
    "warn" -> FpsColors.WarnBg
    "bad" -> FpsColors.BadBg
    else -> FpsColors.Bg2
}
