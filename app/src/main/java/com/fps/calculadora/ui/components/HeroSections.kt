package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.Balance
import com.fps.calculadora.core.CalcResult
import com.fps.calculadora.core.HzMarker
import com.fps.calculadora.core.monitorFraction
import com.fps.calculadora.core.monitorPercent
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono
import kotlin.math.min

/** `.stats` — 1% low, comparação com o monitor e máximo. */
@Composable
fun StatsRow(result: CalcResult, monitorHz: Int, modifier: Modifier = Modifier) {
    val percent = monitorPercent(result.avg, monitorHz)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            label = "1% low",
            value = "${result.min}",
            valueColor = FpsColors.Bad,
            footnote = "${result.minLow} – ${result.minHigh}",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "vs monitor",
            value = "$percent%",
            // Bater ou passar do refresh do monitor é o objetivo — fica verde.
            valueColor = if (percent >= 100) FpsColors.Ok else FpsColors.Tx1,
            footnote = "de $monitorHz Hz",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "máximo",
            value = "${result.max}",
            valueColor = FpsColors.Ok,
            footnote = "${result.maxLow} – ${result.maxHigh}",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    footnote: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg2)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md))
            .padding(start = 6.dp, end = 6.dp, top = 9.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label.uppercase(),
            color = FpsColors.Tx3,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.height(4.dp))
        Text(
            value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RobotoMono,
        )
        Box(Modifier.height(4.dp))
        MonoLabel(footnote)
    }
}

/**
 * `.bars` — 1% low, média e máximo contra o refresh do monitor, com os
 * marcadores de Hz e a régua embaixo.
 */
@Composable
fun MonitorBars(
    result: CalcResult,
    monitorHz: Int,
    averageColor: Color,
    markers: List<HzMarker>,
    modifier: Modifier = Modifier,
) {
    val markerFractions = markers.map { min(it.value.toFloat() / monitorHz, 1f) }
    val highlighted = markers.firstOrNull { it.value == monitorHz }
        ?.let { min(it.value.toFloat() / monitorHz, 1f) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg2)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md))
            .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 6.dp),
    ) {
        val rows = listOf(
            Triple("1% low", result.min, FpsColors.Bad),
            Triple("Média", result.avg, averageColor),
            Triple("Máximo", result.max, FpsColors.Ok),
        )
        for ((index, row) in rows.withIndex()) {
            val (label, value, color) = row
            Column(Modifier.padding(bottom = if (index == rows.lastIndex) 0.dp else 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        label,
                        color = FpsColors.Tx2,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MonoLabel("$value", color = color, size = 10.sp, weight = FontWeight.Bold)
                }
                TrackBar(
                    fraction = monitorFraction(value, monitorHz).toFloat(),
                    color = color,
                    height = 7.dp,
                    markers = markerFractions,
                    highlightedMarker = highlighted,
                )
            }
        }
        AxisLabels(markers, monitorHz, Modifier.padding(top = 4.dp))
    }
}

/**
 * `.axis` — rótulos de Hz posicionados na mesma fração horizontal dos
 * marcadores, cada um centralizado no próprio traço.
 */
@Composable
private fun AxisLabels(markers: List<HzMarker>, monitorHz: Int, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier.fillMaxWidth().height(14.dp),
        content = {
            for (marker in markers) {
                MonoLabel(marker.label, size = 8.5.sp)
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        layout(constraints.maxWidth, 14.dp.roundToPx()) {
            placeables.forEachIndexed { index, placeable ->
                val fraction = min(markers[index].value.toFloat() / monitorHz, 1f)
                // `translateX(-50%)` do CSS, preso às bordas para não vazar.
                val x = (constraints.maxWidth * fraction - placeable.width / 2f)
                    .toInt()
                    .coerceIn(0, constraints.maxWidth - placeable.width)
                placeable.placeRelative(x, 0)
            }
        }
    }
}

/** `.bneck` — quem está segurando o desempenho, CPU ou GPU. */
@Composable
fun BalanceCard(
    balance: Balance,
    result: CalcResult,
    cpuName: String,
    gpuName: String,
    modifier: Modifier = Modifier,
) {
    val neutral = Color(0xFFF4F0E4).copy(alpha = 0.28f)
    val verdict = if (balance.cpuLimited) "Gargalo de CPU" else "Limitado pela GPU"
    val verdictColor = if (balance.cpuLimited) FpsColors.Warn else FpsColors.Ok
    val note = if (balance.cpuLimited) {
        "$cpuName limita este jogo em ${result.cpuCap} FPS — a $gpuName renderizaria " +
            "~${result.gpuFps} FPS (${balance.gpuIdle}% dela fica ociosa)."
    } else {
        "Normal em jogos: a GPU dita o FPS. $cpuName ainda tem ${balance.cpuHeadroom}% " +
            "de folga (teto ~${result.cpuCap} FPS)."
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg2)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md))
            .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "EQUILÍBRIO CPU × GPU",
                color = FpsColors.Tx3,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.em,
            )
            Text(verdict, color = verdictColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
        LoadRow(
            label = "GPU",
            load = balance.gpuLoad,
            color = if (balance.cpuLimited) neutral else FpsColors.Acc,
        )
        LoadRow(
            label = "CPU",
            load = balance.cpuLoad,
            color = if (balance.cpuLimited) FpsColors.Warn else neutral,
        )
        Text(
            note,
            color = FpsColors.Tx3,
            fontSize = 10.sp,
            lineHeight = 15.5.sp,
            modifier = Modifier.padding(top = 9.dp),
        )
    }
}

@Composable
private fun LoadRow(label: String, load: Int, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(top = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        MonoLabel(label, Modifier.width(30.dp), size = 9.sp, weight = FontWeight.Bold)
        TrackBar(
            fraction = load / 100f,
            color = color,
            modifier = Modifier.weight(1f),
            height = 8.dp,
        )
        MonoLabel(
            "$load%",
            Modifier.width(40.dp),
            color = FpsColors.Tx1,
            size = 11.sp,
            weight = FontWeight.Bold,
        )
    }
}

/** `.hero-meta` — o que o Frame Generation faz com a latência percebida. */
@Composable
fun LatencyLine(result: CalcResult, modifier: Modifier = Modifier) {
    val text: AnnotatedString = buildAnnotatedString {
        if (result.frameGenMult > 1) {
            withStyle(SpanStyle(color = FpsColors.Tx2, fontWeight = FontWeight.SemiBold)) {
                append("Latência: ")
            }
            append(
                "sente-se como ${result.baseMin}–${result.baseFps} FPS reais " +
                    "(antes do Frame Gen ×${"%.2f".format(result.frameGenMult)})"
            )
        } else {
            withStyle(SpanStyle(color = FpsColors.Tx2, fontWeight = FontWeight.SemiBold)) {
                append("Latência ")
            }
            append("equivalente ao FPS exibido — sem Frame Generation")
        }
    }
    Text(
        text,
        modifier.fillMaxWidth(),
        color = FpsColors.Tx3,
        fontSize = 10.5.sp,
        textAlign = TextAlign.Center,
        letterSpacing = 0.02.em,
    )
}

/** `.warns` — os avisos que a `calc()` levantou, na mesma ordem do original. */
@Composable
fun WarningList(warnings: List<String>, modifier: Modifier = Modifier) {
    if (warnings.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        for (warning in warnings) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FpsRadius.Md))
                    .background(FpsColors.WarnBg)
                    .border(
                        1.dp,
                        FpsColors.Warn.copy(alpha = 0.25f),
                        RoundedCornerShape(FpsRadius.Md),
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SvgIcon(
                    Icons.WARNING,
                    Modifier.size(14.dp).padding(top = 1.dp),
                    tint = FpsColors.Warn,
                    strokeWidth = 1.8f,
                )
                Text(
                    warning,
                    color = FpsColors.Warn,
                    fontSize = 11.sp,
                    lineHeight = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
