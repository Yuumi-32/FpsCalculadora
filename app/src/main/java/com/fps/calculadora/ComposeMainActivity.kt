package com.fps.calculadora

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fps.calculadora.ui.components.AppTab
import com.fps.calculadora.ui.components.BottomNav
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
                    var tab by remember { mutableStateOf(AppTab.CALC) }

                    Column(Modifier.fillMaxSize().systemBarsPadding()) {
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
            }
        }
    }
}

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
