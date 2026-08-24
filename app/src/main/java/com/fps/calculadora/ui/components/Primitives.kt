package com.fps.calculadora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono

/**
 * Ícone desenhado a partir do mesmo atributo `d` do SVG do `index.html`.
 *
 * Manter as strings originais evita redesenhar 15 ícones à mão e garante que a
 * iconografia continue idêntica — o `PathParser` do Compose lê o mesmo formato
 * que o navegador.
 */
@Composable
fun SvgIcon(
    pathData: String,
    modifier: Modifier = Modifier,
    tint: Color = FpsColors.Tx1,
    strokeWidth: Float = 1.7f,
    viewportSize: Float = 24f,
) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Box(
        modifier.drawBehind {
            val factor = size.minDimension / viewportSize
            // Centraliza caso a caixa não seja quadrada.
            translate(
                left = (size.width - viewportSize * factor) / 2f,
                top = (size.height - viewportSize * factor) / 2f,
            ) {
                scale(factor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(
                        path = path,
                        color = tint,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    )
}

/**
 * Ícone da aba "Jogos" — grade 2×2 de retângulos arredondados. É desenhado à
 * parte do [SvgIcon] porque o `PathParser` só lê um `d` de `path`, e o
 * original usa quatro `rect rx="1.6"` — cantos arredondados de verdade em vez
 * de aproximar por segmentos de arco escritos à mão.
 */
@Composable
fun GridIcon(
    modifier: Modifier = Modifier,
    tint: Color = FpsColors.Tx1,
    strokeWidth: Float = 1.7f,
    viewportSize: Float = 24f,
) {
    Box(
        modifier.drawBehind {
            val factor = size.minDimension / viewportSize
            translate(
                left = (size.width - viewportSize * factor) / 2f,
                top = (size.height - viewportSize * factor) / 2f,
            ) {
                scale(factor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val corner = androidx.compose.ui.geometry.CornerRadius(1.6f, 1.6f)
                    for ((x, y) in listOf(4f to 4f, 13f to 4f, 4f to 13f, 13f to 13f)) {
                        drawRoundRect(
                            color = tint,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(7f, 7f),
                            cornerRadius = corner,
                            style = stroke,
                        )
                    }
                }
            }
        }
    )
}

/** `.card` — o contêiner padrão das seções. */
@Composable
fun FpsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FpsRadius.Lg))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Lg)),
        content = content,
    )
}

/** `h2` — título de topo das telas Jogos, Upgrade, Comparar e Histórico. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(top = 10.dp, bottom = 4.dp),
        color = FpsColors.Tx1,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** `.sec` — rótulo de seção com a linha que preenche o resto da largura. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text.uppercase(),
            color = FpsColors.Tx3,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.12.em,
        )
        Box(Modifier.weight(1f).height(1.dp).background(FpsColors.Line))
    }
}

/** `.seg` — controle segmentado de largura igual entre as opções. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(FpsColors.Bg0)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(11.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for ((value, label) in options) {
            val on = value == selected
            val background by animateColorAsState(
                if (on) FpsColors.Bg3 else Color.Transparent,
                tween(200),
                label = "segBg",
            )
            val content by animateColorAsState(
                if (on) FpsColors.Tx1 else FpsColors.Tx3,
                tween(200),
                label = "segFg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(background)
                    .clickable(enabled = !on) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = content, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** `.chips` — pílulas roláveis na horizontal. */
@Composable
fun <T> ChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        for ((value, label) in options) {
            Chip(label = label, selected = value == selected) { onSelect(value) }
        }
    }
}

/** `.pill` — uma opção isolada. */
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) FpsColors.Acc else FpsColors.Line2
    Box(
        Modifier
            .defaultMinSize(minHeight = 34.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) FpsColors.AccSoft else FpsColors.Bg0)
            .border(1.dp, border, RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) FpsColors.Acc else FpsColors.Tx2,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** `.row` — linha de escolha de peça: ícone, rótulo, valor e seta. */
@Composable
fun SelectionRow(
    iconPath: String,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FpsColors.AccSoft),
                contentAlignment = Alignment.Center,
            ) {
                SvgIcon(iconPath, Modifier.size(17.dp), tint = FpsColors.Acc)
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    label.uppercase(),
                    color = FpsColors.Tx3,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.08.em,
                )
                Text(
                    value,
                    color = FpsColors.Tx1,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SvgIcon(
                Icons.CHEVRON,
                Modifier.size(15.dp),
                tint = FpsColors.Tx3,
                strokeWidth = 2f,
            )
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line))
        }
    }
}

/** `.inrow` / `.cblk` — bloco com rótulo em caixa alta e um controle abaixo. */
@Composable
fun LabeledBlock(
    label: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                label.uppercase(),
                color = FpsColors.Tx3,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.em,
            )
            content()
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line))
        }
    }
}

/** Barra fina com trilho arredondado — base das barras de gargalo e de monitor. */
@Composable
fun TrackBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 8.dp,
    markers: List<Float> = emptyList(),
    highlightedMarker: Float? = null,
) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "trackFill",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(100.dp))
            .background(FpsColors.Bg0)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(100.dp))
                .background(color)
        )
        // Marcadores de Hz por cima do preenchimento, como no `.bmk`.
        Box(Modifier.fillMaxWidth().height(height).drawBehind {
            for (marker in markers) {
                val x = size.width * marker.coerceIn(0f, 1f)
                drawLine(
                    color = if (marker == highlightedMarker) FpsColors.Tx3 else FpsColors.Line2,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1f,
                )
            }
        })
    }
}

/** Texto monoespaçado pequeno, usado em legendas numéricas. */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FpsColors.Tx3,
    size: androidx.compose.ui.unit.TextUnit = 9.sp,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(text, modifier, color = color, fontSize = size, fontFamily = RobotoMono, fontWeight = weight)
}

/**
 * Os `d` dos SVG do `index.html`, copiados verbatim para o `PathParser`.
 */
object Icons {
    const val CHEVRON = "m9 5 7 7-7 7"
    const val CPU = "M7 7h10v10H7zM10.2 10.2h3.6v3.6h-3.6zM9.5 7V4M14.5 7V4M9.5 20v-3" +
        "M14.5 20v-3M7 9.5H4M7 14.5H4M20 9.5h-3M20 14.5h-3"
    const val MOBO = "M3.5 3.5h17v17h-17zM7 7h5v5H7zM15.5 7v5M18 7v5M7 16h6M16.5 15.5h1"
    const val RAM = "M3 8h18v8.5H3zM6.5 8v5M10.2 8v5M13.8 8v5M17.5 8v5" +
        "M5 16.5V19M9.5 16.5V19M14.5 16.5V19M19 16.5V19"
    const val GPU = "M3 7.5h17.5v9.5H6M3 7.5V19M12.5 10.4v-1M12.5 15v-1M14.3 12.2h1M9.7 12.2h1"
    const val GAME = "M6.8 6.5h10.4c2.6 0 4.3 2 4.3 5.5s-1.2 6-3 6c-1.3 0-2-.8-2.8-2.1" +
        "-.6-1-1.1-1.4-2.2-1.4h-3c-1.1 0-1.6.4-2.2 1.4C7.5 17.2 6.8 18 5.5 18c-1.8 0-3-2.5-3-6" +
        "s1.7-5.5 4.3-5.5zM8 10v3M6.5 11.5h3"
    const val WARNING = "M12 3.5 22 20H2L12 3.5zM12 10v4.5"
    const val INFO = "M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17zM12 11v5"
    const val TARGET = "M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17z" +
        "M12 7.4a4.6 4.6 0 1 0 0 9.2 4.6 4.6 0 0 0 0-9.2z"

    // Barra de navegação e as 4 telas que ela abre.
    const val NAV_CALC = "M4.5 17.5a8.5 8.5 0 1 1 15 0M12 13.5 15.5 9" +
        "M12 13.1a1.4 1.4 0 1 0 0 2.8 1.4 1.4 0 0 0 0-2.8z"
    const val NAV_UPGRADE = "M3 17 9.5 10.5l4 4L21 7M15 7h6v6"
    const val NAV_COMPARE = "M8 7h12m0 0-3.5-3.5M20 7l-3.5 3.5M16 17H4m0 0 3.5-3.5M4 17l3.5 3.5"
    const val NAV_HISTORY = "M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17zM12 7.5V12l3 2"

    const val SAVE = "M6 3.5h12a1 1 0 0 1 1 1V21l-7-4-7 4V4.5a1 1 0 0 1 1-1z"
    const val COPY_CODE = "M8 8h12v12H8zM16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"
    const val IMPORT_CODE = "M12 4v11m0 0 4.5-4.5M12 15l-4.5-4.5M5 20h14"
    const val LOAD = "M12 4v12m0 0 5-5m-5 5-5-5M5 20h14"
    const val TRASH = "M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13M10 11v5M14 11v5"
}
