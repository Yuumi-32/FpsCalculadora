package com.fps.calculadora

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fps.calculadora.ui.screens.CalcScreen
import com.fps.calculadora.ui.state.CalcViewModel
import com.fps.calculadora.ui.theme.FpsBackground
import com.fps.calculadora.ui.theme.FpsTheme

/**
 * A UI nativa da Fase 2. Hoje entrega a tela Calcular; as outras quatro abas
 * ainda vivem no WebView.
 *
 * Convive com a [MainActivity] — no debug as duas aparecem como ícones
 * separados, para comparar a tela nova com a antiga no mesmo aparelho.
 */
class ComposeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FpsTheme {
                FpsBackground {
                    val vm: CalcViewModel = viewModel()
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        CalcScreen(vm = vm, animated = animationsEnabled())
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
