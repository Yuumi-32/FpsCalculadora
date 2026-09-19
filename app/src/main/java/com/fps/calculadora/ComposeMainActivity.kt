package com.fps.calculadora

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fps.calculadora.core.shortCpuName
import com.fps.calculadora.ui.components.AppTab
import com.fps.calculadora.ui.components.BottomNav
import com.fps.calculadora.ui.components.OnboardingOverlay
import com.fps.calculadora.ui.screens.CalcScreen
import com.fps.calculadora.ui.screens.CompareScreen
import com.fps.calculadora.ui.screens.GamesScreen
import com.fps.calculadora.ui.screens.HistoryScreen
import com.fps.calculadora.ui.screens.UpgradeScreen
import com.fps.calculadora.ui.state.CalcViewModel
import com.fps.calculadora.ui.theme.FpsBackground
import com.fps.calculadora.ui.theme.FpsTheme

/**
 * A UI nativa da Fase 2. As 5 abas do `index.html` (`nav.nav`, :1126) ganham
 * cada uma sua tela Compose, todas lendo e escrevendo o mesmo [CalcViewModel].
 *
 * Convive com a [MainActivity] — no debug as duas aparecem como ícones
 * separados, para comparar a tela nova com a antiga no mesmo aparelho.
 */
class ComposeMainActivity : ComponentActivity() {

    private val vm: CalcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Segura a splash até o disco responder com o perfil salvo. Sem isso o
        // app abriria na build padrão e trocaria para a do usuário meio segundo
        // depois — um pisca que faz o app parecer ter esquecido o que ele sabe.
        SplashScreenBridge.install(this).setKeepOnScreenCondition { !vm.restored }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FpsTheme {
                FpsBackground {
                    val animated = animationsEnabled()

                    // Uma tentativa por abertura. O repositório decide se vale
                    // ir à rede (12h desde o último download, conexão de pé) e
                    // devolve o estado atual sem custo quando não vale.
                    LaunchedEffect(Unit) { vm.refreshCatalog() }
                    // `rememberSaveable`: a aba sobrevive a uma recriação da
                    // Activity (rotação, mudança de tema, processo morto em
                    // segundo plano), que é quando um `remember` perderia a
                    // navegação sem que ninguém tivesse pedido nada.
                    var tab by rememberSaveable { mutableStateOf(AppTab.CALC) }

                    // Voltar leva à aba Calcular antes de sair do app. Sem isso
                    // o botão fechava o app de dentro de qualquer aba — o gesto
                    // mais comum do Android terminando a sessão por engano.
                    BackHandler(enabled = tab != AppTab.CALC) { tab = AppTab.CALC }

                    // As telas são desenhadas numa coluna de largura limitada e
                    // centralizada. Em pé num celular isso não muda nada (a tela
                    // é mais estreita que o teto); em paisagem, em tablet e na
                    // janela do modo desktop é o que impede uma linha de card de
                    // atravessar 20 cm de tela. O Android 16 ignora o
                    // screenOrientation="portrait" em tela grande, então essa
                    // situação deixou de ser hipotética.
                    //
                    // O recorte da câmera entra aqui junto das barras do sistema:
                    // deitado, o furo fica na lateral, onde só o
                    // displayCutoutPadding protege.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .displayCutoutPadding(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(Modifier.fillMaxHeight().widthIn(max = CONTENT_MAX_WIDTH)) {
                            Box(Modifier.weight(1f)) {
                                when (tab) {
                                    AppTab.CALC -> CalcScreen(
                                        vm = vm,
                                        animated = animated,
                                        onGoToUpgrade = { tab = AppTab.UPGRADE },
                                    )
                                    AppTab.GAMES -> GamesScreen(vm = vm, onOpenGame = { tab = AppTab.CALC })
                                    AppTab.UPGRADE -> UpgradeScreen(vm = vm)
                                    AppTab.COMPARE -> CompareScreen(vm = vm)
                                    AppTab.HISTORY -> HistoryScreen(
                                        vm = vm,
                                        onOpenBuild = { tab = AppTab.CALC },
                                        onCompareBuild = { tab = AppTab.COMPARE },
                                    )
                                }
                            }
                            BottomNav(selected = tab, onSelect = { tab = it })
                        }
                    }

                    // Por cima de tudo, e só na primeira abertura.
                    if (vm.onboardingPending) {
                        BackHandler { vm.dismissOnboarding() }
                        OnboardingOverlay(
                            modifier = Modifier.systemBarsPadding().displayCutoutPadding(),
                            presets = vm.db.constants.buildPresets,
                            cpuName = { id -> shortCpuName(vm.db.cpu(id).name) },
                            gpuName = { id -> vm.db.gpu(id).name },
                            onPick = { preset ->
                                vm.applyBuildPreset(preset)
                                vm.dismissOnboarding()
                                tab = AppTab.CALC
                            },
                            onSkip = { vm.dismissOnboarding() },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Teto de largura do conteúdo. 560dp é pouco mais que a largura de um celular
 * grande em pé (o Pixel 9 Pro XL tem 448dp), então em retrato nada muda: o
 * limite só começa a valer deitado, em tablet e na janela do modo desktop.
 */
private val CONTENT_MAX_WIDTH = 560.dp

/**
 * Equivalente Android do `prefers-reduced-motion`: quando o usuário zera a
 * escala de animação nas opções do sistema, os números saltam direto para o
 * valor final.
 */
@Composable
private fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
    }
}
