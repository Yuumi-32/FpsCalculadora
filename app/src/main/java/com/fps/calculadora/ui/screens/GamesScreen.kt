package com.fps.calculadora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.GamesSort
import com.fps.calculadora.core.PerformanceTier
import com.fps.calculadora.core.monitorFraction
import com.fps.calculadora.core.shortCpuName
import com.fps.calculadora.ui.components.FpsCard
import com.fps.calculadora.ui.components.MonoLabel
import com.fps.calculadora.ui.components.ScreenTitle
import com.fps.calculadora.ui.components.SegmentedControl
import com.fps.calculadora.ui.components.TrackBar
import com.fps.calculadora.ui.components.color
import com.fps.calculadora.ui.state.CalcViewModel
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.RobotoMono

/**
 * "Seu PC em todos os jogos" — o `#panel-games` do `index.html` (:1071).
 *
 * Mantém a build atual inteira e varia só o jogo, jogo a jogo — é
 * [com.fps.calculadora.core.rankAllGames] no `:core` que faz essa conta;
 * aqui só se decide como desenhar.
 */
@Composable
fun GamesScreen(vm: CalcViewModel, onOpenGame: () -> Unit, modifier: Modifier = Modifier) {
    val state = vm.state
    val db = vm.db
    val cpu = db.cpu(state.cpuId)
    val gpu = db.gpu(state.gpuId)
    val presetName = db.preset(state.preset)?.name.orEmpty()
    val ranking = remember(state, vm.gamesSort) { vm.gamesRanking() }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ScreenTitle("Seu PC em todos os jogos")
        Text(
            "${shortCpuName(cpu.name)} · ${gpu.name} · ${state.resolution.label} $presetName " +
                "— toque num jogo para abrir na calculadora.",
            color = FpsColors.Tx3,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        SegmentedControl(
            options = listOf(GamesSort.FPS to "Maior FPS", GamesSort.NAME to "A–Z"),
            selected = vm.gamesSort,
            onSelect = { vm.gamesSort = it },
        )
        FpsCard(Modifier.padding(top = 12.dp, bottom = 20.dp)) {
            for ((index, entry) in ranking.withIndex()) {
                GameRow(
                    rank = index + 1,
                    name = entry.game.name,
                    fps = entry.result.avg,
                    monitorHz = state.monitorHz,
                    showDivider = index != ranking.lastIndex,
                    onClick = {
                        vm.update { it.copy(gameId = entry.game.id) }
                        onOpenGame()
                    },
                )
            }
        }
    }
}

@Composable
private fun GameRow(
    rank: Int,
    name: String,
    fps: Int,
    monitorHz: Int,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val tier = PerformanceTier.forFps(fps)
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MonoLabel("$rank", Modifier.width(22.dp), color = FpsColors.Tx3, size = 11.sp, weight = FontWeight.Bold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    name,
                    color = FpsColors.Tx1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TrackBar(fraction = monitorFraction(fps, monitorHz).toFloat(), color = tier.color(), height = 5.dp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$fps", color = tier.color(), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = RobotoMono)
                Text(tier.label, color = tier.color(), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line))
        }
    }
}
