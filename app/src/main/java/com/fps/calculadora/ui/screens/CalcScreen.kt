package com.fps.calculadora.ui.screens

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.BuildState
import com.fps.calculadora.core.CalcResult
import com.fps.calculadora.core.PcProfile
import com.fps.calculadora.core.PerformanceTier
import com.fps.calculadora.core.Resolution
import com.fps.calculadora.core.RtSetting
import com.fps.calculadora.core.frameGenOptionsFor
import com.fps.calculadora.core.ramOptionsFor
import com.fps.calculadora.core.rtOptionsFor
import com.fps.calculadora.core.shortCpuName
import com.fps.calculadora.share.renderShareCard
import com.fps.calculadora.share.saveCardToGallery
import com.fps.calculadora.share.shareCard
import com.fps.calculadora.share.shareCardFileName
import com.fps.calculadora.ui.components.NewPartsNotice
import com.fps.calculadora.ui.components.BalanceCard
import com.fps.calculadora.ui.components.ChipRow
import com.fps.calculadora.ui.components.EnergyCard
import com.fps.calculadora.ui.components.FpsCard
import com.fps.calculadora.ui.components.FpsGauge
import com.fps.calculadora.ui.components.GoalSheet
import com.fps.calculadora.ui.components.HowSheet
import com.fps.calculadora.ui.components.Icons
import com.fps.calculadora.ui.components.LabeledBlock
import com.fps.calculadora.ui.components.LatencyLine
import com.fps.calculadora.ui.components.MonitorBars
import com.fps.calculadora.ui.components.PickerOption
import com.fps.calculadora.ui.components.PickerSheet
import com.fps.calculadora.ui.components.PresetBuildsCarousel
import com.fps.calculadora.ui.components.ProfilesSheet
import com.fps.calculadora.ui.components.PsuCard
import com.fps.calculadora.ui.components.SectionLabel
import com.fps.calculadora.ui.components.SelectionRow
import com.fps.calculadora.ui.components.ShareSheet
import com.fps.calculadora.ui.components.StatsRow
import com.fps.calculadora.ui.components.SvgIcon
import com.fps.calculadora.ui.components.WarningList
import com.fps.calculadora.ui.components.color
import com.fps.calculadora.ui.state.CalcViewModel
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius
import com.fps.calculadora.ui.theme.RobotoMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Qual folha de seleção está aberta. */
private enum class Picker { NONE, CPU, MOBO, RAM, GPU, GAME }

/** Qual folha informativa do hero está aberta. */
private enum class InfoSheet { NONE, HOW, GOAL }

/** Taxas de atualização de monitor oferecidas no seletor "Monitor · taxa de atualização". */
private val MONITOR_HZ_OPTIONS = listOf(60, 75, 100, 120, 144, 165, 180, 210, 240, 300, 360)

/**
 * A tela Calcular — o `#panel-calc` do `index.html` (:890-1069).
 *
 * Toda conta vem do [CalcViewModel], que por sua vez só repassa o `:core`.
 * Aqui não se decide nenhum número.
 */
@Composable
fun CalcScreen(
    vm: CalcViewModel,
    animated: Boolean,
    onGoToUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = vm.snapshot
    val state = snapshot.state
    val result = snapshot.result
    val db = vm.db

    val game = db.game(state.gameId)
    val cpu = db.cpu(state.cpuId)
    val gpu = db.gpu(state.gpuId)
    val mobo = db.mobo(state.moboId)

    var picker by remember { mutableStateOf(Picker.NONE) }
    var infoSheet by remember { mutableStateOf(InfoSheet.NONE) }
    var justSaved by remember { mutableStateOf(false) }
    var profilesOpen by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }
    var shareFeedback by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // O card é 1080×1350: desenhar no fio da UI trava a abertura da folha, e
    // recalcular a cada recomposição seria desperdício. Sai de cena junto com
    // a folha, que é quando o `produceState` é descartado.
    val card by produceState<Bitmap?>(null, shareOpen, state) {
        value = null
        if (!shareOpen) return@produceState
        value = withContext(Dispatchers.Default) {
            renderShareCard(context, db, state, result, snapshot.psu)
        }
    }

    // A seta de variação some depois de 2,4 s, como o `deltaFade` do CSS.
    LaunchedEffect(vm.previousFps) {
        if (vm.previousFps != null) {
            delay(2400)
            vm.clearPreviousFps()
        }
    }

    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(1400)
            justSaved = false
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        TopBar()
        ProfileBar(name = vm.activeProfileName, onClick = { profilesOpen = true })

        // Só aparece quando o catálogo remoto trouxe hardware que não existia
        // neste APK. Fica antes do card principal porque é contexto sobre as
        // opções disponíveis, não sobre o resultado do cálculo.
        NewPartsNotice(vm.catalog.newPartNames)

        HeroCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    game.name,
                    color = FpsColors.Tx1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SaveBuildButton(
                        saved = justSaved,
                        onClick = { vm.saveCurrentBuild(); justSaved = true },
                    )
                    HeroIconButton(Icons.SHARE, "Compartilhar") {
                        shareFeedback = null
                        shareOpen = true
                    }
                }
            }
            Text(
                "${shortCpuName(cpu.name)} · ${gpu.name} · " +
                    "${state.resolution.label} ${db.preset(state.preset)?.name.orEmpty()}",
                color = FpsColors.Tx3,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )

            FpsGauge(
                fps = result.avg,
                rangeLow = result.avgLow,
                rangeHigh = result.avgHigh,
                monitorHz = state.monitorHz,
                hzMarkers = hzMarkersFor(vm, state.monitorHz),
                tier = snapshot.tier,
                previousFps = vm.previousFps,
                animated = animated,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            StatsRow(result, state.monitorHz, Modifier.padding(top = 2.dp))
            BalanceCard(
                balance = snapshot.balance,
                result = result,
                cpuName = cpu.name,
                gpuName = gpu.name,
                modifier = Modifier.padding(top = 12.dp),
            )
            MonitorBars(
                result = result,
                monitorHz = state.monitorHz,
                averageColor = snapshot.tier.color(),
                markers = hzMarkersFor(vm, state.monitorHz),
                modifier = Modifier.padding(top = 12.dp),
            )
            LatencyLine(result, Modifier.padding(top = 10.dp))
            HowGoalButtons(
                onHow = { infoSheet = InfoSheet.HOW },
                onGoal = { infoSheet = InfoSheet.GOAL },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp),
            )
        }

        ResolutionCompare(
            byResolution = snapshot.byResolution,
            current = state.resolution,
            onSelect = { resolution -> vm.update { it.copy(resolution = resolution) } },
            modifier = Modifier.padding(top = 12.dp),
        )

        WarningList(snapshot.warnings, Modifier.padding(top = 12.dp))

        SectionLabel("Builds prontos")
        PresetBuildsCarousel(
            presets = db.constants.buildPresets,
            cpuName = { id -> shortCpuName(db.cpu(id).name) },
            gpuName = { id -> db.gpu(id).name },
            onSelect = { preset -> vm.applyBuildPreset(preset) },
        )

        SectionLabel("Monte seu PC")
        FpsCard {
            SelectionRow(Icons.CPU, "Processador", cpu.name, { picker = Picker.CPU })
            SelectionRow(Icons.MOBO, "Placa-mãe", mobo.name, { picker = Picker.MOBO })
            SelectionRow(
                Icons.RAM,
                "Memória RAM",
                db.constants.ramLabels[state.ram] ?: state.ram,
                { picker = Picker.RAM },
            )
            SelectionRow(Icons.GPU, "Placa de vídeo", "${gpu.name} · ${gpu.vram.toInt()} GB", { picker = Picker.GPU })
            LabeledBlock("Monitor · taxa de atualização", showDivider = false) {
                ChipRow(
                    options = MONITOR_HZ_OPTIONS.map { it to "$it Hz" },
                    selected = state.monitorHz,
                    onSelect = { hz -> vm.update { it.copy(monitorHz = hz) } },
                )
            }
        }

        SectionLabel("Jogo e gráficos")
        FpsCard {
            SelectionRow(Icons.GAME, "Jogo", game.name, { picker = Picker.GAME })
            LabeledBlock("Resolução") {
                ChipRow(
                    options = Resolution.entries.map { it to it.label },
                    selected = state.resolution,
                    onSelect = { resolution -> vm.update { it.copy(resolution = resolution) } },
                )
            }
            LabeledBlock("Qualidade gráfica") {
                ChipRow(
                    options = db.constants.presets.map { it.key to it.name },
                    selected = state.preset,
                    onSelect = { preset -> vm.update { it.copy(preset = preset) } },
                )
            }

            val rtOptions = db.rtOptionsFor(game, gpu)
            if (rtOptions.show) {
                LabeledBlock(rtOptions.label) {
                    ChipRow(
                        options = rtOptions.options.map { it.setting to it.name },
                        selected = state.rt,
                        onSelect = { rt: RtSetting -> vm.update { it.copy(rt = rt) } },
                    )
                }
            }

            val fgOptions = db.frameGenOptionsFor(gpu)
            if (fgOptions.size > 1) {
                LabeledBlock("Frame Generation") {
                    ChipRow(
                        options = fgOptions.map { it.mult to it.name },
                        selected = state.frameGen,
                        onSelect = { mult -> vm.update { it.copy(frameGen = mult) } },
                    )
                }
            }

            LabeledBlock(
                when {
                    gpu.gen.isRadeon -> "Upscaling / FSR"
                    gpu.gen.isArc -> "Upscaling / XeSS"
                    else -> "Upscaling / DLSS"
                },
                showDivider = false,
            ) {
                ChipRow(
                    options = db.upscalersFor(gpu).map { it.mult to it.name },
                    selected = state.upscaler,
                    onSelect = { mult -> vm.update { it.copy(upscaler = mult) } },
                )
            }
        }

        SectionLabel("Fonte de alimentação")
        FpsCard {
            PsuCard(
                psu = snapshot.psu,
                gpuWatts = gpu.watts,
                cpuWatts = cpu.watts,
                systemWatts = db.constants.systemWatts,
            )
            EnergyCard(
                energy = snapshot.energy,
                hoursPerDay = vm.hoursPerDay,
                tariff = vm.tariff,
                onHoursChange = { vm.hoursPerDay = it },
                onTariffChange = { vm.tariff = it },
            )
        }

        Footer(db.meta.version, Modifier.padding(top = 20.dp, bottom = 28.dp))
    }

    when (picker) {
        Picker.NONE -> Unit
        Picker.CPU -> PickerSheet(
            title = "Processador",
            options = db.cpus.map { PickerOption(it.id, it.name, it.group, it.socket.name) },
            selectedId = state.cpuId,
            onSelect = { id -> vm.update { it.copy(cpuId = id) }; picker = Picker.NONE },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.MOBO -> PickerSheet(
            title = "Placa-mãe",
            // Só as compatíveis com o socket da CPU escolhida.
            options = db.mobosFor(cpu.socket).map { PickerOption(it.id, it.name, it.group) },
            selectedId = state.moboId,
            onSelect = { id -> vm.update { it.copy(moboId = id) }; picker = Picker.NONE },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.RAM -> PickerSheet(
            title = "Memória RAM",
            options = db.ramOptionsFor(cpu.socket).map { PickerOption(it.key, it.name, it.group) },
            selectedId = state.ram,
            onSelect = { id -> vm.update { it.copy(ram = id) }; picker = Picker.NONE },
            onDismiss = { picker = Picker.NONE },
            searchable = false,
        )
        Picker.GPU -> PickerSheet(
            title = "Placa de vídeo",
            options = db.gpus.map {
                PickerOption(it.id, it.name, it.group, "${it.vram.toInt()} GB")
            },
            selectedId = state.gpuId,
            onSelect = { id -> vm.update { it.copy(gpuId = id) }; picker = Picker.NONE },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.GAME -> PickerSheet(
            title = "Jogo",
            options = db.games.map { PickerOption(it.id, it.name, it.group) },
            selectedId = state.gameId,
            onSelect = { id -> vm.update { it.copy(gameId = id) }; picker = Picker.NONE },
            onDismiss = { picker = Picker.NONE },
        )
    }

    when (infoSheet) {
        InfoSheet.NONE -> Unit
        InfoSheet.HOW -> HowSheet(
            result = result,
            dbVersion = db.meta.version,
            dbUpdated = db.meta.updated,
            onDismiss = { infoSheet = InfoSheet.NONE },
        )
        InfoSheet.GOAL -> GoalSheet(
            initialTarget = state.monitorHz,
            monitorHz = state.monitorHz,
            gameName = game.name,
            resolutionLabel = state.resolution.label,
            goalAdvice = { target -> vm.goalAdvice(target) },
            onApply = { option -> vm.applyGoalOption(option) },
            onGoToUpgrade = onGoToUpgrade,
            onDismiss = { infoSheet = InfoSheet.NONE },
        )
    }

    if (profilesOpen) {
        ProfilesSheet(
            book = vm.profiles,
            subtitle = { profile -> profileSubtitle(vm, profile) },
            onSwitch = { id -> vm.switchProfile(id) },
            onRename = { id, name -> vm.renameProfile(id, name) },
            onDelete = { id -> vm.deleteProfile(id) },
            onNew = { vm.newProfile() },
            onDismiss = { profilesOpen = false },
        )
    }

    if (shareOpen) {
        val bitmap = card
        ShareSheet(
            card = bitmap,
            // Abaixo do Android 10 salvar exigiria permissão de armazenamento;
            // o botão some em vez de pedir uma permissão que não se justifica.
            canSaveToGallery = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            feedback = shareFeedback,
            onShare = {
                if (bitmap != null) {
                    val ok = shareCard(
                        context = context,
                        bitmap = bitmap,
                        fileName = shareCardFileName(db, state),
                        text = vm.currentShareText(),
                    )
                    if (!ok) shareFeedback = "Não foi possível abrir o compartilhamento"
                }
            },
            onSaveToGallery = {
                if (bitmap != null) {
                    val ok = saveCardToGallery(context, bitmap, shareCardFileName(db, state))
                    shareFeedback = if (ok) {
                        "PNG salvo em Imagens → FPS Calculadora"
                    } else {
                        "Não foi possível salvar na galeria"
                    }
                }
            },
            onCopyText = {
                clipboard.setText(AnnotatedString(vm.currentShareText()))
                shareFeedback = "Resumo copiado — cole onde quiser"
            },
            onCopyCode = {
                clipboard.setText(AnnotatedString(vm.exportCurrentBuildCode()))
                shareFeedback = "Código copiado — envie para um amigo"
            },
            onDismiss = { shareOpen = false },
        )
    }
}

/**
 * A linha embaixo do nome do perfil na folha — o `profSub()` do `index.html`
 * (:3394). Perfil que aponta para peça que a base não conhece mais vira um
 * travessão em vez de derrubar a folha.
 */
private fun profileSubtitle(vm: CalcViewModel, profile: PcProfile): String = try {
    val db = vm.db
    "${shortCpuName(db.cpu(profile.state.cpuId).name)} + ${db.gpu(profile.state.gpuId).name} · " +
        Resolution.fromKey(profile.state.resolution).label
} catch (e: IllegalStateException) {
    "—"
}

/** `#btnHow` / `#btnGoal` — as duas pílulas embaixo do hero (`index.html:958`). */
@Composable
private fun HowGoalButtons(onHow: () -> Unit, onGoal: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HowGoalPill("Como calculamos", Icons.INFO, onHow)
        HowGoalPill("Meta de FPS", Icons.TARGET, onGoal)
    }
}

@Composable
private fun HowGoalPill(label: String, iconPath: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SvgIcon(iconPath, Modifier.size(13.dp), tint = FpsColors.Tx3, strokeWidth = 1.8f)
        Text(label, color = FpsColors.Tx3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TopBar() {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp)) {
        Text("Estimador de FPS", color = FpsColors.Tx1, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Ryzen · Core · GeForce · Radeon · Arc",
            color = FpsColors.Tx3,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/**
 * `.profbar` — o nome do perfil ativo, que abre a troca de perfil
 * (`index.html:900`). Fica acima do resultado porque responde "de qual PC é
 * este número", que é a primeira coisa a saber ao olhar para ele.
 */
@Composable
private fun ProfileBar(name: String, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(top = 2.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line2, RoundedCornerShape(100.dp))
            .clickable(onClickLabel = "Trocar perfil de PC", onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SvgIcon(Icons.PC, Modifier.size(13.dp), tint = FpsColors.Acc, strokeWidth = 1.8f)
        Text(
            name,
            color = FpsColors.Tx1,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        SvgIcon(Icons.CARET, Modifier.size(11.dp), tint = FpsColors.Tx3, strokeWidth = 2f)
    }
}

/** `#btnSave` — salva a build atual no histórico local (`index.html:916`). */
@Composable
private fun SaveBuildButton(saved: Boolean, onClick: () -> Unit) {
    HeroIconButton(
        path = Icons.SAVE,
        description = "Salvar no histórico",
        tint = if (saved) FpsColors.Ok else FpsColors.Tx2,
        background = if (saved) FpsColors.OkBg else FpsColors.Bg2,
        onClick = onClick,
    )
}

/** `.icbtn` — os botões quadrados do canto do hero. */
@Composable
private fun HeroIconButton(
    path: String,
    description: String,
    tint: Color = FpsColors.Tx2,
    background: Color = FpsColors.Bg2,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(path, Modifier.size(15.dp), tint = tint, strokeWidth = 1.8f)
    }
}

/** `.hero` — o cartão do resultado. */
@Composable
private fun HeroCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FpsRadius.Lg))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Lg))
            .padding(14.dp),
        content = content,
    )
}

/** `.rescmp` — a mesma build em todas as resoluções, tocável para trocar. */
@Composable
private fun ResolutionCompare(
    byResolution: Map<Resolution, CalcResult>,
    current: Resolution,
    onSelect: (Resolution) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FpsRadius.Lg))
            .background(FpsColors.Bg1)
            .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Lg))
            .padding(14.dp),
    ) {
        Text(
            "EM TODAS AS RESOLUÇÕES — TOQUE PARA TROCAR",
            color = FpsColors.Tx3,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
            modifier = Modifier.padding(bottom = 9.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (resolution in Resolution.entries) {
                val cell = byResolution[resolution] ?: continue
                val on = resolution == current
                Column(
                    Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) FpsColors.AccSoft else FpsColors.Bg2)
                        .border(
                            1.dp,
                            if (on) FpsColors.Acc else FpsColors.Line,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelect(resolution) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        resolution.label,
                        color = if (on) FpsColors.Acc else FpsColors.Tx3,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${cell.avg}",
                        color = PerformanceTier.forFps(cell.avg).color(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMono,
                    )
                    Text(
                        "FPS",
                        color = FpsColors.Tx3,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.em,
                    )
                }
            }
        }
    }
}

@Composable
private fun Footer(dbVersion: String, modifier: Modifier = Modifier) {
    Text(
        "Referência: RTX 5070 + Ryzen 7 5700X + B550 + DDR4 32GB @ DLSS Balanceado = 1.00×\n" +
            "Base de dados $dbVersion · Ryzen & Intel Core · GeForce, Radeon & Arc\n" +
            "Valores estimados — desempenho real varia por jogo e drivers.",
        modifier.fillMaxWidth(),
        color = FpsColors.Tx3,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center,
    )
}

private fun hzMarkersFor(vm: CalcViewModel, monitorHz: Int) =
    vm.db.constants.hzMarkers[monitorHz.toString()]
        ?: vm.db.constants.hzMarkers.getValue("144")
