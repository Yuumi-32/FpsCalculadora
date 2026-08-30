package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius

/**
 * "Apareceram peças novas" — o resultado visível de o catálogo remoto ter
 * trazido hardware que não existia quando este APK foi publicado.
 *
 * É um aviso e não uma notificação: fica no fluxo da tela, não interrompe
 * nada, não pede toque e não some sozinho. A informação é útil ("dá para
 * escolher a 5080 Super agora") mas não é urgente, e tratá-la como urgente
 * seria mentir sobre a importância dela.
 *
 * Nomeia as peças em vez de dizer só "3 peças novas". O número sozinho obriga
 * a pessoa a caçar o que mudou nos seletores; o nome responde na hora se
 * interessa ou não.
 */
@Composable
fun NewPartsNotice(names: List<String>, modifier: Modifier = Modifier) {
    if (names.isEmpty()) return

    // Acima de quatro a lista vira parede de texto e para de ser lida. O
    // resto entra como contagem.
    val mostrados = names.take(4)
    val resto = names.size - mostrados.size
    val lista = mostrados.joinToString(", ") + if (resto > 0) " e mais $resto" else ""

    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.OkBg)
            .border(1.dp, FpsColors.Ok.copy(alpha = 0.25f), RoundedCornerShape(FpsRadius.Md))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (names.size == 1) "Peça nova no catálogo" else "${names.size} peças novas no catálogo",
                color = FpsColors.Ok,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(lista, color = FpsColors.Tx2, fontSize = 10.5.sp, lineHeight = 15.sp)
        }
    }
}
