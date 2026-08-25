package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.GoalAdvice
import com.fps.calculadora.core.GoalOption
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono

/**
 * "Meta de FPS" — porta o `openGoalSheet()`/`fillGoal()` do `index.html`
 * (:3209). O usuário escolhe um FPS-alvo e a folha sugere até 3 combinações de
 * preset/upscaler/Frame Gen/RT que chegam lá, priorizando a maior qualidade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSheet(
    initialTarget: Int,
    monitorHz: Int,
    gameName: String,
    resolutionLabel: String,
    goalAdvice: (Int) -> GoalAdvice,
    onApply: (GoalOption) -> Unit,
    onGoToUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var target by remember { mutableIntStateOf(initialTarget) }
    val advice = remember(target) { goalAdvice(target) }
    val targets = remember(monitorHz) { setOf(60, 90, 120, 144, 240, monitorHz).sorted() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FpsColors.Bg1,
        contentColor = FpsColors.Tx1,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Meta de FPS",
                color = FpsColors.Tx1,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                for (t in targets) {
                    Chip(
                        label = if (t == monitorHz) "$t FPS · monitor" else "$t FPS",
                        selected = t == target,
                        onClick = { target = t },
                    )
                }
            }

            Text(
                "Hoje: ${advice.currentAvg} FPS em $gameName · $resolutionLabel",
                color = FpsColors.Tx3,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            )

            when {
                advice.bestPossibleAvg != null -> {
                    Text(
                        "Nenhuma combinação de configurações chega a $target FPS neste jogo — " +
                            "o máximo do seu hardware é ~${advice.bestPossibleAvg} FPS. " +
                            "O caminho é trocar peça.",
                        color = FpsColors.Warn,
                        fontSize = 11.sp,
                        lineHeight = 16.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(FpsRadius.Md))
                            .background(FpsColors.WarnBg)
                            .padding(12.dp),
                    )
                    GoalUpgradeButton(onClick = { onGoToUpgrade(); onDismiss() })
                }
                advice.alreadyAtBestQuality -> {
                    Text(
                        "Você já atinge $target FPS com a melhor qualidade possível — nada a melhorar aqui.",
                        color = FpsColors.Ok,
                        fontSize = 11.sp,
                        lineHeight = 16.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(FpsRadius.Md))
                            .background(FpsColors.OkBg)
                            .padding(12.dp),
                    )
                }
                else -> {
                    Column(Modifier.padding(top = 6.dp)) {
                        FpsCard(Modifier.padding(horizontal = 16.dp)) {
                            advice.options.forEachIndexed { index, option ->
                                GoalOptionRow(
                                    option = option,
                                    showDivider = index != advice.options.lastIndex,
                                    onClick = { onApply(option); onDismiss() },
                                )
                            }
                        }
                        Text(
                            "Sugestões priorizam qualidade visual: preset mais alto primeiro, " +
                                "depois upscaler mais leve, Frame Gen por último.",
                            color = FpsColors.Tx3,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalOptionRow(option: GoalOption, showDivider: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Preset ${option.presetName}",
                    color = FpsColors.Tx1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val sub = listOfNotNull(
                    option.upscalerName.takeIf { it.isNotEmpty() },
                    option.frameGenLabel.takeIf { it.isNotEmpty() },
                    "desliga RT".takeIf { option.turnsRtOff },
                ).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(
                        "$sub — toque para aplicar",
                        color = FpsColors.Tx3,
                        fontSize = 10.sp,
                        lineHeight = 14.5.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${option.result.avg}",
                    color = FpsColors.Ok,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMono,
                )
                Text("FPS", color = FpsColors.Tx3, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (showDivider) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line),
            )
        }
    }
}

@Composable
private fun GoalUpgradeButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.AccSoft)
            .border(1.dp, FpsColors.Acc.copy(alpha = 0.3f), RoundedCornerShape(FpsRadius.Md))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "Ver o que trocar → aba Upgrade",
            color = FpsColors.Acc,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
