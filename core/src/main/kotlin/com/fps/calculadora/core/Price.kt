package com.fps.calculadora.core

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Preço no FPS Calculadora é **sempre uma média de mercado, nunca uma
 * cotação**. Nada aqui pretende dizer quanto uma peça custa hoje numa loja
 * específica: o número serve para comparar peças entre si e responder "vale a
 * pena?", não para conferir com o carrinho de compras.
 *
 * Essa promessa é sustentada por duas decisões deste arquivo:
 *
 * 1. [roundToAverage] joga fora a precisão antes de a UI ver o número. Um
 *    preço arredondado para a centena não tem como ser confundido com uma
 *    cotação — "R$ 4.187,43" parece um preço de loja, "≈ R$ 4.200" não.
 * 2. [formatAveragePrice] nunca devolve o valor cru: sai sempre com "≈" na
 *    frente e sem centavos.
 *
 * Quem quiser o número sem enfeite para conta tem [Double] direto no modelo —
 * mas o que chega na tela passa por aqui.
 */

/**
 * Arredonda para uma granularidade proporcional ao valor, de forma que o
 * resultado nunca carregue precisão que a média não tem.
 *
 * A escala acompanha o preço porque R$ 10 de diferença importa numa peça de
 * R$ 200 e é ruído numa de R$ 8.000:
 *
 * | Faixa | Arredonda para |
 * |---|---|
 * | até R$ 500 | R$ 10 |
 * | até R$ 2.000 | R$ 50 |
 * | acima | R$ 100 |
 */
fun roundToAverage(brl: Double): Long {
    val step = when {
        abs(brl) <= 500 -> 10
        abs(brl) <= 2_000 -> 50
        else -> 100
    }
    return (brl / step).roundToLong() * step
}

/** `4200` → `"4.200"` — separador de milhar brasileiro, sem centavos. */
private fun groupThousands(value: Long): String {
    val digits = abs(value).toString()
    val out = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) out.append('.')
        out.append(c)
    }
    return if (value < 0) "-$out" else out.toString()
}

/**
 * O jeito — único — de um preço chegar à tela: `≈ R$ 4.200`.
 *
 * O "≈" não é enfeite. É o que separa "esta peça custa por volta disso" de
 * "esta peça custa isto", e a diferença entre as duas frases é a única coisa
 * que o app pode honestamente prometer sobre preço.
 *
 * Sem preço no catálogo, devolve [semPreco] em vez de inventar zero.
 */
fun formatAveragePrice(brl: Double?, semPreco: String = "—"): String {
    if (brl == null || brl <= 0) return semPreco
    return "≈ R$ " + groupThousands(roundToAverage(brl))
}

/**
 * Quanto de FPS a build entrega para cada R$ 1.000 investidos nas duas peças
 * que mandam no resultado (CPU e GPU).
 *
 * Por R$ 1.000 e não por real porque "0,031 FPS por real" não diz nada a
 * ninguém, enquanto "31 FPS por R$ 1.000" dá para comparar de cabeça.
 *
 * `null` quando falta o preço de qualquer uma das duas peças — meia conta seria
 * pior que conta nenhuma, porque o número sairia otimista e parecendo completo.
 */
fun fpsPerThousandBrl(fps: Int, cpuPrice: Double?, gpuPrice: Double?): Double? {
    if (cpuPrice == null || gpuPrice == null) return null
    val total = cpuPrice + gpuPrice
    if (total <= 0) return null
    return fps * 1_000.0 / total
}

/**
 * Quanto custa, em média, cada FPS que um upgrade acrescenta — a pergunta que
 * a tela "O que trocar primeiro?" existe para responder.
 *
 * `null` quando não há preço, ou quando o ganho é zero ou negativo: dividir por
 * um ganho que não existe produz infinito, e infinito na tela não ajuda
 * ninguém a decidir.
 */
fun brlPerFpsGained(gainFps: Int, price: Double?): Double? {
    if (price == null || price <= 0 || gainFps <= 0) return null
    return price / gainFps
}
