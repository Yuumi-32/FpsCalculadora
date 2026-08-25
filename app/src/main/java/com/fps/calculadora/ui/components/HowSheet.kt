package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.CalcResult
import com.fps.calculadora.core.Step
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono
import kotlin.math.roundToInt

/**
 * "Como calculamos" — porta o `openHowSheet()` do `index.html` (:2801): o
 * waterfall completo de multiplicadores, na ordem em que `calc()` os aplica,
 * até chegar no FPS médio mostrado no card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowSheet(result: CalcResult, dbVersion: String, dbUpdated: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FpsColors.Bg1,
        contentColor = FpsColors.Tx1,
    ) {
        Text(
            "Como calculamos",
            color = FpsColors.Tx1,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            items(result.steps) { step -> StepRow(step) }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(FpsRadius.Md))
                        .background(FpsColors.AccSoft)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "FPS MÉDIO ESTIMADO",
                        color = FpsColors.Acc,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.08.em,
                    )
                    Text(
                        "${result.avg}",
                        color = FpsColors.Acc,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMono,
                    )
                }
            }
            item {
                Text(
                    "Faixa provável: ${result.avgLow}–${result.avgHigh} FPS (±5%). " +
                        "Base de dados v$dbVersion · $dbUpdated.\n" +
                        "Estimativa comparativa — o valor real varia por cena, drivers e temperatura.",
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 24.dp),
                    color = FpsColors.Tx3,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StepRow(step: Step) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(step.title, color = FpsColors.Tx1, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                if (step.detail.isNotEmpty()) {
                    Text(
                        step.detail,
                        color = FpsColors.Tx3,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            val (label, color) = multiplierLabel(step)
            Text(
                label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RobotoMono,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                "${step.fps.roundToInt()}",
                color = FpsColors.Tx1,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RobotoMono,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line))
    }
}

private fun multiplierLabel(step: Step): Pair<String, Color> {
    val mult = step.mult ?: return (if (step.isCap) "teto" else "base") to FpsColors.Tx3
    val label = "×${"%.2f".format(mult)}"
    return when {
        mult > 1.001 -> label to FpsColors.Ok
        mult < 0.999 -> label to FpsColors.Bad
        else -> label to FpsColors.Tx3
    }
}
