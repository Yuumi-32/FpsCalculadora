package com.fps.calculadora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.CpuUpgrade
import com.fps.calculadora.core.GpuUpgrade
import com.fps.calculadora.core.RamUpgrade
import com.fps.calculadora.core.UpgradeAdvice
import com.fps.calculadora.ui.components.FpsCard
import com.fps.calculadora.ui.components.Icons
import com.fps.calculadora.ui.components.ScreenTitle
import com.fps.calculadora.ui.components.SectionLabel
import com.fps.calculadora.ui.components.SvgIcon
import com.fps.calculadora.ui.state.CalcViewModel
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono

/**
 * "O que trocar primeiro?" — o `#panel-upg` do `index.html` (:1081).
 *
 * Toda a decisão (quem é o gargalo, quais peças valem a troca) vem de
 * [com.fps.calculadora.core.upgradeAdvice] — aqui só se desenha o resultado.
 */
@Composable
fun UpgradeScreen(vm: CalcViewModel, modifier: Modifier = Modifier) {
    val state = vm.state
    val game = vm.db.game(state.gameId)
    val advice = remember(state) { vm.upgradeAdvice() }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ScreenTitle("O que trocar primeiro?")
        Text(
            "Ganhos estimados em ${game.name} · ${state.resolution.key.uppercase()} " +
                "— trocando só a peça indicada.",
            color = FpsColors.Tx3,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        VerdictCard(title = advice.verdictTitle, body = advice.verdictBody)

        val gpuSection: @Composable () -> Unit = {
            SectionLabel("Trocar GPU")
            UpgradeSection(advice.gpuEmptyMessage) {
                for ((index, up) in advice.gpuUpgrades.withIndex()) {
                    GpuUpgradeRow(up, advice.currentPsuRecommended, index != advice.gpuUpgrades.lastIndex)
                }
            }
        }
        val cpuSection: @Composable () -> Unit = {
            SectionLabel("Trocar CPU")
            UpgradeSection(advice.cpuEmptyMessage) {
                for ((index, up) in advice.cpuUpgrades.withIndex()) {
                    CpuUpgradeRow(up, index != advice.cpuUpgrades.lastIndex)
                }
            }
        }

        if (advice.cpuSectionFirst) {
            cpuSection(); gpuSection()
        } else {
            gpuSection(); cpuSection()
        }

        advice.ramUpgrade?.let { ram ->
            SectionLabel("Upgrade de RAM")
            FpsCard(Modifier.padding(bottom = 4.dp)) { RamUpgradeRow(ram) }
        }

        Text(
            advice.footer,
            color = FpsColors.Tx3,
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun VerdictCard(title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FpsRadius.Lg))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Lg))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(FpsColors.AccSoft),
            contentAlignment = Alignment.Center,
        ) {
            SvgIcon(Icons.NAV_UPGRADE, Modifier.size(17.dp), tint = FpsColors.Acc)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = FpsColors.Tx1, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(body, color = FpsColors.Tx2, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun UpgradeSection(emptyMessage: String?, content: @Composable () -> Unit) {
    FpsCard(Modifier.padding(bottom = 4.dp)) {
        if (emptyMessage != null) {
            Text(
                emptyMessage,
                color = FpsColors.Tx3,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            content()
        }
    }
}

@Composable
private fun GpuUpgradeRow(up: GpuUpgrade, currentPsuRecommended: Int, showDivider: Boolean) {
    val tag = if (up.psuRecommended > currentPsuRecommended) "fonte ${up.psuRecommended} W" else null
    HardwareUpgradeRow(
        name = up.gpu.name,
        subtitle = "${up.gpu.group} · ${up.gpu.vram.toInt()} GB VRAM",
        tag = tag,
        gainFps = up.gainFps,
        newAvg = up.result.avg,
        gainPercent = up.gainPercent,
        showDivider = showDivider,
    )
}

@Composable
private fun CpuUpgradeRow(up: CpuUpgrade, showDivider: Boolean) {
    val subtitle = if (up.dropIn) up.cpu.group else "${up.cpu.group} · requer placa-mãe e RAM novas"
    HardwareUpgradeRow(
        name = up.cpu.name,
        subtitle = subtitle,
        tag = if (up.dropIn) "drop-in" else "nova plataforma",
        gainFps = up.gainFps,
        newAvg = up.result.avg,
        gainPercent = up.gainPercent,
        showDivider = showDivider,
    )
}

@Composable
private fun HardwareUpgradeRow(
    name: String,
    subtitle: String,
    tag: String?,
    gainFps: Int,
    newAvg: Int,
    gainPercent: Int,
    showDivider: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    name,
                    color = FpsColors.Tx1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, color = FpsColors.Tx3, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tag != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(FpsColors.AccSoft)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(tag, color = FpsColors.Acc, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "+$gainFps FPS",
                    color = FpsColors.Ok,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMono,
                )
                Text(
                    "→ $newAvg FPS (+$gainPercent%)",
                    color = FpsColors.Tx3,
                    fontSize = 10.sp,
                    fontFamily = RobotoMono,
                )
            }
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(FpsColors.Line))
        }
    }
}

@Composable
private fun RamUpgradeRow(ram: RamUpgrade) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ram.ramLabel, color = FpsColors.Tx1, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(ram.reason, color = FpsColors.Tx3, fontSize = 10.5.sp)
        }
        Text(
            "+${ram.gainFps} FPS",
            color = FpsColors.Ok,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RobotoMono,
        )
    }
}
