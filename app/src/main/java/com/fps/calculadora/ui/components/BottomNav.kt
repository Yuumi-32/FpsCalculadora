package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.NavBarHeight

/** As 5 abas do app — `data-tab` do `<nav>` do `index.html` (:1126). */
enum class AppTab(val label: String) {
    CALC("Calcular"),
    GAMES("Jogos"),
    UPGRADE("Upgrade"),
    COMPARE("Comparar"),
    HISTORY("Histórico"),
}

/** `.nav` — a barra inferior fixa com as 5 abas (`index.html:436`). */
@Composable
fun BottomNav(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(FpsColors.Bg1)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line))
        Row(Modifier.fillMaxWidth().height(NavBarHeight)) {
            for (tab in AppTab.entries) {
                NavButton(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun RowScope.NavButton(tab: AppTab, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) FpsColors.Acc else FpsColors.Tx3
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = !selected, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (selected) FpsColors.Acc else Color.Transparent),
        )
        Spacer(Modifier.height(5.dp))
        if (tab == AppTab.GAMES) {
            GridIcon(Modifier.width(21.dp).height(21.dp), tint = tint)
        } else {
            SvgIcon(iconFor(tab), Modifier.width(21.dp).height(21.dp), tint = tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(tab.label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun iconFor(tab: AppTab): String = when (tab) {
    AppTab.CALC -> Icons.NAV_CALC
    AppTab.GAMES -> "" // GridIcon desenha sozinho, não usa SvgIcon
    AppTab.UPGRADE -> Icons.NAV_UPGRADE
    AppTab.COMPARE -> Icons.NAV_COMPARE
    AppTab.HISTORY -> Icons.NAV_HISTORY
}
