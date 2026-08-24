package com.fps.calculadora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.CompareRow
import com.fps.calculadora.core.HistoryEntry
import com.fps.calculadora.core.toBuildState
import com.fps.calculadora.ui.components.FpsCard
import com.fps.calculadora.ui.components.MonoLabel
import com.fps.calculadora.ui.components.PickerOption
import com.fps.calculadora.ui.components.PickerSheet
import com.fps.calculadora.ui.components.ScreenTitle
import com.fps.calculadora.ui.components.SegmentedControl
import com.fps.calculadora.ui.components.TrackBar
import com.fps.calculadora.ui.components.buildHwLine
import com.fps.calculadora.ui.components.formatHistoryDate
import com.fps.calculadora.ui.state.CalcViewModel
import com.fps.calculadora.ui.state.CompareMode
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono

/**
 * "Comparar builds" — o `#panel-comp` do `index.html` (:1090). A build atual
 * (A) contra um build salvo no histórico (B), no jogo atual ou em todos.
 */
@Composable
fun CompareScreen(vm: CalcViewModel, modifier: Modifier = Modifier) {
    val entries by vm.historyEntries.collectAsState(initial = emptyList())
    val entryB = entries.firstOrNull { it.id == vm.compareBuildId }
    var pickerOpen by remember { mutableStateOf(false) }

    val db = vm.db
    val game = db.game(vm.state.gameId)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ScreenTitle("Comparar builds")
        Text(
            "Build atual (A) contra um build salvo no histórico (B).",
            color = FpsColors.Tx3,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SlotCard(
                tag = "A · Atual",
                name = game.name,
                hw = buildHwLine(db, vm.state),
                modifier = Modifier.weight(1f),
                onClick = null,
            )
            Text("VS", color = FpsColors.Tx3, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            SlotCard(
                tag = "B · Salvo",
                name = entryB?.let { db.game(it.state.gameId).name } ?: "Escolher do histórico",
                hw = entryB?.let { buildHwLine(db, it.state.toBuildState()) }
                    ?: if (entries.isEmpty()) "Nenhum build salvo ainda" else "Toque para selecionar",
                modifier = Modifier.weight(1f),
                onClick = { pickerOpen = true },
            )
        }

        if (entryB == null) {
            EmptyCompareState(hasHistory = entries.isNotEmpty(), modifier = Modifier.padding(top = 14.dp))
        } else {
            val buildB = entryB.state.toBuildState()
            SegmentedControl(
                options = listOf(CompareMode.GAME to "Jogo atual", CompareMode.ALL_GAMES to "Todos os jogos"),
                selected = vm.compareMode,
                onSelect = { vm.compareMode = it },
                modifier = Modifier.padding(top = 14.dp),
            )
            Box(Modifier.padding(top = 12.dp, bottom = 20.dp)) {
                if (vm.compareMode == CompareMode.GAME) {
                    GameCompareBody(vm, buildB, entryB)
                } else {
                    AllGamesCompareBody(vm, buildB)
                }
            }
        }
    }

    if (pickerOpen) {
        PickerSheet(
            title = "Escolher do histórico",
            options = entries.map { entry ->
                val build = entry.state.toBuildState()
                PickerOption(
                    id = entry.id.toString(),
                    name = "${db.game(build.gameId).name} — ${vm.fpsFor(build)} FPS",
                    group = "Histórico",
                    trailing = formatHistoryDate(entry.ts),
                )
            },
            selectedId = vm.compareBuildId?.toString().orEmpty(),
            onSelect = { id -> vm.compareBuildId = id.toLongOrNull(); pickerOpen = false },
            onDismiss = { pickerOpen = false },
            searchable = false,
        )
    }
}

@Composable
private fun SlotCard(tag: String, name: String, hw: String, modifier: Modifier = Modifier, onClick: (() -> Unit)?) {
    Column(
        modifier
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(12.dp),
    ) {
        Text(tag, color = FpsColors.Tx3, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            color = FpsColors.Tx1,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(hw, color = FpsColors.Tx3, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyCompareState(hasHistory: Boolean, modifier: Modifier = Modifier) {
    FpsCard(modifier) {
        Column(Modifier.padding(20.dp)) {
            Text("Escolha o build B", color = FpsColors.Tx1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                if (hasHistory) {
                    "Toque no cartão B acima para escolher um build salvo."
                } else {
                    "Salve um build na aba Calcular para poder comparar."
                },
                color = FpsColors.Tx3,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun GameCompareBody(vm: CalcViewModel, buildB: BuildState, entryB: HistoryEntry) {
    val rows = remember(vm.state, buildB) { vm.compareGame(buildB) }
    Column {
        FpsCard {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                for (row in rows) CompareRowView(row)
            }
        }
        Text(
            "A = build atual da calculadora · B = ${vm.db.game(buildB.gameId).name} salvo em " +
                formatHistoryDate(entryB.ts),
            color = FpsColors.Tx3,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun CompareRowView(row: CompareRow) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.label, color = FpsColors.Tx2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            row.deltaPercent?.let { d ->
                val color = if (row.deltaFavorsA) FpsColors.Ok else FpsColors.Bad
                Text(
                    "A ${if (d > 0) "+" else ""}$d%",
                    color = color,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMono,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        CompareBar("A", row.valueA, row.maxValue, row.unit, FpsColors.Acc, row.aWins)
        Spacer(Modifier.height(4.dp))
        CompareBar("B", row.valueB, row.maxValue, row.unit, FpsColors.Info, row.bWins)
    }
}

@Composable
private fun CompareBar(label: String, value: Int, max: Int, unit: String, color: Color, winner: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MonoLabel(label, Modifier.width(14.dp), color = color, size = 9.sp, weight = FontWeight.Bold)
        TrackBar(
            fraction = if (max > 0) value.toFloat() / max else 0f,
            color = color,
            modifier = Modifier.weight(1f),
            height = 8.dp,
        )
        MonoLabel(
            "$value$unit",
            Modifier.width(50.dp),
            color = if (winner) FpsColors.Tx1 else FpsColors.Tx3,
            size = 11.sp,
            weight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AllGamesCompareBody(vm: CalcViewModel, buildB: BuildState) {
    val summary = remember(vm.state, buildB) { vm.compareAllGames(buildB) }
    Column {
        Text(
            buildAnnotatedSummary(summary.bWinsCount, summary.entries.size, summary.averageDeltaPercent),
            color = FpsColors.Tx2,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        FpsCard {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                for ((index, entry) in summary.entries.withIndex()) {
                    val color = when {
                        entry.deltaPercent > 1 -> FpsColors.Ok
                        entry.deltaPercent < -1 -> FpsColors.Bad
                        else -> FpsColors.Tx3
                    }
                    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                entry.game.name,
                                color = FpsColors.Tx1,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${if (entry.deltaPercent > 0) "+" else ""}${entry.deltaPercent}%",
                                color = color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMono,
                            )
                        }
                        Text(
                            "A ${entry.avgA} → B ${entry.avgB} FPS",
                            color = FpsColors.Tx3,
                            fontSize = 10.sp,
                            fontFamily = RobotoMono,
                        )
                    }
                    if (index != summary.entries.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line))
                    }
                }
            }
        }
        Text(
            "Cada linha mantém as configurações de cada build, trocando só o jogo.",
            color = FpsColors.Tx3,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

private fun buildAnnotatedSummary(bWins: Int, total: Int, avgDelta: Int): String {
    val sign = if (avgDelta > 0) "+" else ""
    return "B ganha em $bWins de $total jogos · diferença média $sign$avgDelta%"
}
