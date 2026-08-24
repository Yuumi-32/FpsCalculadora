package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.EnergyEstimate
import com.fps.calculadora.core.PsuEstimate
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono
import kotlin.math.roundToInt

/** `R$ 123,45` — o `fmtBRL` do JS, com vírgula decimal. */
fun formatBrl(value: Double): String = "R$ " + "%.2f".format(value).replace('.', ',')

/** `.psu` — fonte mínima e recomendada, com o detalhamento do consumo. */
@Composable
fun PsuCard(
    psu: PsuEstimate,
    gpuWatts: Int,
    cpuWatts: Int,
    systemWatts: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PsuTile(
                label = "Mínima",
                value = "${psu.min} W",
                valueColor = FpsColors.Tx1,
                certification = "80 Plus Bronze+",
                modifier = Modifier.weight(1f),
            )
            PsuTile(
                label = "Recomendada",
                value = "${psu.recommended} W",
                valueColor = FpsColors.Ok,
                certification = "80 Plus Gold+",
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 11.dp)
                .height(1.dp)
                .background(FpsColors.Line2)
        )
        val breakdown = buildAnnotatedString {
            val strong = SpanStyle(color = FpsColors.Tx2, fontWeight = FontWeight.SemiBold)
            append("Consumo: ")
            withStyle(strong) { append("GPU ${gpuWatts}W") }
            append(" + ")
            withStyle(strong) { append("CPU ${cpuWatts}W") }
            append(" + ")
            withStyle(strong) { append("Sistema ${systemWatts}W") }
            append(" = ")
            withStyle(strong) { append("${psu.total}W") }
        }
        Text(
            breakdown,
            Modifier.fillMaxWidth().padding(top = 10.dp),
            color = FpsColors.Tx3,
            fontSize = 10.sp,
            fontFamily = RobotoMono,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PsuTile(
    label: String,
    value: String,
    valueColor: Color,
    certification: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg2)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label.uppercase(),
            color = FpsColors.Tx3,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
        )
        Box(Modifier.height(5.dp))
        Text(
            value,
            color = valueColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RobotoMono,
        )
        Box(Modifier.height(5.dp))
        Text(certification, color = FpsColors.Tx3, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
    }
}

/** `.energy` — custo mensal, com horas por dia e tarifa editáveis. */
@Composable
fun EnergyCard(
    energy: EnergyEstimate,
    hoursPerDay: Int,
    tariff: Double,
    onHoursChange: (Int) -> Unit,
    onTariffChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "CUSTO DE ENERGIA",
                color = FpsColors.Tx3,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.em,
            )
            Text(
                formatBrl(energy.monthlyCost) + "/mês",
                color = FpsColors.Tx1,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RobotoMono,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Horas de jogo/dia", color = FpsColors.Tx3, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (hours in listOf(1, 2, 3, 4, 6, 8)) {
                    HourChip(hours, selected = hours == hoursPerDay) { onHoursChange(hours) }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Tarifa (R\$/kWh)", color = FpsColors.Tx3, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            TariffField(tariff, onTariffChange)
        }

        Text(
            "≈ ${energy.kwhPerMonth.roundToInt()} kWh/mês com carga típica de " +
                "${energy.gamingWatts} W jogando ${hoursPerDay}h/dia. " +
                "Tarifa média BR ≈ R$ 0,95/kWh — confira na sua conta de luz.",
            Modifier.padding(top = 8.dp),
            color = FpsColors.Tx3,
            fontSize = 9.5.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun HourChip(hours: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) FpsColors.AccSoft else FpsColors.Bg2)
            .border(
                1.dp,
                if (selected) FpsColors.Acc else FpsColors.Line,
                RoundedCornerShape(9.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${hours}h",
            color = if (selected) FpsColors.Acc else FpsColors.Tx2,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = RobotoMono,
        )
    }
}

/**
 * Campo da tarifa. Mantém o texto cru enquanto o usuário digita — só propaga
 * valores dentro da faixa aceita, igual ao `0 < v < 20` do original.
 */
@Composable
private fun TariffField(tariff: Double, onTariffChange: (Double) -> Unit) {
    // Sem chave: o valor inicial vem de `tariff`, mas depois disso o texto só
    // muda pela digitação do usuário — reformatar a cada tecla (via `tariff`
    // como chave) apagava o que ele estava escrevendo a cada dígito válido.
    var text by remember { mutableStateOf("%.2f".format(tariff)) }
    BasicTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.replace(',', '.').toDoubleOrNull()
                ?.takeIf { it > 0 && it < 20 }
                ?.let(onTariffChange)
        },
        textStyle = TextStyle(
            color = FpsColors.Tx1,
            fontSize = 12.sp,
            fontFamily = RobotoMono,
            textAlign = TextAlign.End,
        ),
        cursorBrush = SolidColor(FpsColors.Acc),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(FpsColors.Bg0)
            .border(1.dp, FpsColors.Line2, RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
    )
}
