package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.BuildPreset
import com.fps.calculadora.ui.theme.FpsColors

/** As três resoluções oferecidas na abertura — as ultrawide ficam de fora, como no original. */
private val ONBOARDING_RESOLUTIONS = listOf("1080p", "1440p", "4k")

/**
 * Boas-vindas da primeira abertura — o `#onbd` do `index.html` (:1166).
 *
 * Cobre a tela inteira e oferece as nove builds prontas como ponto de partida,
 * porque a pergunta que trava alguém na primeira abertura não é "quanto FPS eu
 * faço", é "qual é o meu PC". Escolher um card não é um compromisso: aplica a
 * build e leva direto para a calculadora, onde tudo continua editável.
 *
 * É desenhada por cima das abas, e não como uma folha, de propósito: enquanto
 * ela está no ar não existe outra coisa para fazer no app.
 */
@Composable
fun OnboardingOverlay(
    presets: Map<String, BuildPreset>,
    cpuName: (String) -> String,
    gpuName: (String) -> String,
    onPick: (BuildPreset) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(FpsColors.Bg0)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Column(Modifier.widthIn(max = 480.dp).align(Alignment.TopCenter)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(FpsColors.Acc))
                Text(
                    "ESTIMADOR DE FPS",
                    color = FpsColors.Tx3,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.em,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            Text(
                "Qual é o seu PC?",
                color = FpsColors.Tx1,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                "Escolha um ponto de partida para já ver o desempenho estimado — " +
                    "depois dá para ajustar cada peça.",
                color = FpsColors.Tx3,
                fontSize = 12.5.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            for (resolution in ONBOARDING_RESOLUTIONS) {
                val row = TIER_NAMES.keys.mapNotNull { tier ->
                    presets["${resolution}_$tier"]?.let { tier to it }
                }
                if (row.isEmpty()) continue

                Text(
                    resolution.uppercase(),
                    color = FpsColors.Tx3,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.em,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for ((tier, preset) in row) {
                        PresetBuildCard(
                            resolution = resolution.uppercase(),
                            tierName = TIER_NAMES[tier] ?: tier,
                            cpuLabel = cpuName(preset.cpu),
                            gpuLabel = gpuName(preset.gpu),
                            onClick = { onPick(preset) },
                            // Três por linha num celular estreito: o mínimo do
                            // carrossel não cabe, e o nome da peça quebra sozinho.
                            modifier = Modifier.weight(1f),
                            minWidth = 0.dp,
                            hardwareWidth = null,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FpsColors.Bg1)
                    .border(1.dp, FpsColors.Line2, RoundedCornerShape(14.dp))
                    .clickable(onClick = onSkip)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Montar do zero →",
                    color = FpsColors.Tx1,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
