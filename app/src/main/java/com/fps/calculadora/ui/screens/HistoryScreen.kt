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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.HistoryEntry
import com.fps.calculadora.core.PerformanceTier
import com.fps.calculadora.core.toBuildState
import com.fps.calculadora.ui.components.FpsCard
import com.fps.calculadora.ui.components.Icons
import com.fps.calculadora.ui.components.MonoLabel
import com.fps.calculadora.ui.components.ScreenTitle
import com.fps.calculadora.ui.components.SvgIcon
import com.fps.calculadora.ui.components.buildHwLine
import com.fps.calculadora.ui.components.color
import com.fps.calculadora.ui.components.formatHistoryDate
import com.fps.calculadora.ui.state.CalcViewModel
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono

/**
 * "Histórico local" — o `#panel-hist` do `index.html` (:1113). Builds salvos
 * no aparelho (`DataStore`, ver [com.fps.calculadora.data.HistoryStore]) mais
 * os botões de exportar/importar um build por código de texto — que sempre
 * operam sobre a build atual da calculadora, não sobre um item da lista.
 */
@Composable
fun HistoryScreen(
    vm: CalcViewModel,
    onOpenBuild: () -> Unit,
    onCompareBuild: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries by vm.historyEntries.collectAsState(initial = emptyList())
    val clipboard = LocalClipboardManager.current
    var importOpen by remember { mutableStateOf(false) }
    var copyFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ScreenTitle("Histórico")
        Text(
            "Builds salvos neste aparelho. Toque no ícone de salvar na aba Calcular pra adicionar um novo.",
            color = FpsColors.Tx3,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            ActionButton(
                icon = Icons.COPY_CODE,
                label = "Copiar código",
                modifier = Modifier.weight(1f),
                onClick = {
                    clipboard.setText(AnnotatedString(vm.exportCurrentBuildCode()))
                    copyFeedback = "Código copiado — envie para um amigo"
                },
            )
            ActionButton(
                icon = Icons.IMPORT_CODE,
                label = "Importar código",
                modifier = Modifier.weight(1f),
                onClick = { importOpen = true },
            )
        }
        copyFeedback?.let {
            Text(it, color = FpsColors.Ok, fontSize = 10.5.sp, modifier = Modifier.padding(bottom = 10.dp))
        }

        if (entries.isEmpty()) {
            EmptyHistoryState()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                for (entry in entries) {
                    HistoryCard(
                        vm = vm,
                        entry = entry,
                        onLoad = { vm.loadHistoryEntry(entry); onOpenBuild() },
                        onCompare = { vm.compareBuildId = entry.id; onCompareBuild() },
                        onDelete = { vm.deleteHistoryEntry(entry.id) },
                    )
                }
            }
        }
    }

    if (importOpen) {
        ImportCodeSheet(
            onImport = { code -> vm.importBuildCode(code) },
            onImported = { importOpen = false; onOpenBuild() },
            onDismiss = { importOpen = false },
        )
    }
}

@Composable
private fun ActionButton(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgIcon(icon, Modifier.size(16.dp), tint = FpsColors.Tx1, strokeWidth = 1.8f)
        Text(label, color = FpsColors.Tx1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyHistoryState() {
    FpsCard {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(FpsColors.Bg2),
                contentAlignment = Alignment.Center,
            ) {
                SvgIcon(Icons.NAV_HISTORY, Modifier.size(22.dp), tint = FpsColors.Tx3)
            }
            Spacer(Modifier.height(12.dp))
            Text("Nenhum build salvo", color = FpsColors.Tx1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "Na aba Calcular, toque no ícone de salvar para guardar o build atual aqui.",
                color = FpsColors.Tx3,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun HistoryCard(
    vm: CalcViewModel,
    entry: HistoryEntry,
    onLoad: () -> Unit,
    onCompare: () -> Unit,
    onDelete: () -> Unit,
) {
    val db = vm.db
    val build = entry.state.toBuildState()
    val game = db.game(build.gameId)
    val fps = vm.fpsFor(build)
    val tier = PerformanceTier.forFps(fps)
    val presetName = db.preset(build.preset)?.name.orEmpty()

    FpsCard {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        game.name,
                        color = FpsColors.Tx1,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(buildHwLine(db, build), color = FpsColors.Tx3, fontSize = 10.5.sp)
                }
                Text("$fps", color = tier.color(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = RobotoMono)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                HistoryTag(build.resolution.label)
                HistoryTag(presetName)
                HistoryTag("${build.monitorHz} Hz")
                Spacer(Modifier.weight(1f))
                Text(formatHistoryDate(entry.ts), color = FpsColors.Tx3, fontSize = 9.5.sp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 11.dp),
            ) {
                HistoryActionButton("Carregar", Icons.LOAD, primary = true, modifier = Modifier.weight(1f), onClick = onLoad)
                HistoryActionButton("Comparar", Icons.NAV_COMPARE, primary = false, modifier = Modifier.weight(1f), onClick = onCompare)
                HistoryActionButton("Excluir", Icons.TRASH, primary = false, danger = true, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun HistoryTag(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(FpsColors.Bg2)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        MonoLabel(text, size = 9.sp, weight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryActionButton(
    label: String,
    icon: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val (bg, fg) = when {
        primary -> FpsColors.AccSoft to FpsColors.Acc
        danger -> FpsColors.BadBg to FpsColors.Bad
        else -> FpsColors.Bg2 to FpsColors.Tx2
    }
    Row(
        modifier
            .clip(RoundedCornerShape(FpsRadius.Sm))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgIcon(icon, Modifier.size(13.dp), tint = fg, strokeWidth = 2f)
        if (label.isNotEmpty()) {
            Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportCodeSheet(onImport: (String) -> Boolean, onImported: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FpsColors.Bg1,
        contentColor = FpsColors.Tx1,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("Importar build", color = FpsColors.Tx1, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Peça para seu amigo tocar em \"Copiar código\" no Histórico do app dele e te enviar.",
                color = FpsColors.Tx3,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FpsRadius.Md))
                    .background(FpsColors.Bg0)
                    .border(1.dp, FpsColors.Line2, RoundedCornerShape(FpsRadius.Md))
                    .padding(12.dp),
            ) {
                if (text.isEmpty()) {
                    Text("Cole aqui o código (começa com FPS1.)", color = FpsColors.Tx3, fontSize = 12.5.sp)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    textStyle = TextStyle(color = FpsColors.Tx1, fontSize = 12.5.sp),
                    cursorBrush = SolidColor(FpsColors.Acc),
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                )
            }
            error?.let {
                Text(it, color = FpsColors.Bad, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 20.dp)
                    .clip(RoundedCornerShape(FpsRadius.Md))
                    .background(FpsColors.Acc)
                    .clickable {
                        if (onImport(text)) onImported() else error = "Código inválido — confira se copiou tudo"
                    }
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Importar", color = FpsColors.Bg0, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
