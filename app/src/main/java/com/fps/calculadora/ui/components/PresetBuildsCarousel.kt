package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.BuildPreset
import com.fps.calculadora.core.shortCpuName
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono

private val TIER_NAMES = mapOf("eco" to "Econômico", "mid" to "Médio", "max" to "Máximo")

/**
 * "Builds prontos" — porta o `buildPresetCards()` do `index.html` (:2490): um
 * carrossel horizontal com os 9 presets do catálogo (3 resoluções × 3 níveis),
 * pra montar uma build inteira de um toque.
 */
@Composable
fun PresetBuildsCarousel(
    presets: Map<String, BuildPreset>,
    cpuName: (String) -> String,
    gpuName: (String) -> String,
    onSelect: (BuildPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (presets.isEmpty()) return
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((key, preset) in presets) {
            val (res, tier) = key.split("_", limit = 2).let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }
            PresetBuildCard(
                resolution = res.uppercase(),
                tierName = TIER_NAMES[tier] ?: tier,
                cpuLabel = cpuName(preset.cpu),
                gpuLabel = gpuName(preset.gpu),
                onClick = { onSelect(preset) },
            )
        }
    }
}

@Composable
private fun PresetBuildCard(
    resolution: String,
    tierName: String,
    cpuLabel: String,
    gpuLabel: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .widthIn(min = 138.dp)
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            resolution,
            color = FpsColors.Acc,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
            fontFamily = RobotoMono,
        )
        Text(
            tierName,
            color = FpsColors.Tx1,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 3.dp),
        )
        Text(
            "$cpuLabel\n$gpuLabel",
            color = FpsColors.Tx3,
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(top = 4.dp).width(114.dp),
        )
    }
}
