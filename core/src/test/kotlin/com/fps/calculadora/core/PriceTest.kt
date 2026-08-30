package com.fps.calculadora.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A promessa que este arquivo protege é de produto, não de código: **o app
 * mostra média de mercado, nunca cotação**.
 *
 * É uma promessa fácil de quebrar sem perceber — basta alguém formatar um
 * preço direto do modelo em vez de passar por [formatAveragePrice], ou afrouxar
 * o arredondamento "só para ficar mais informativo". Os testes abaixo existem
 * para essa mudança falhar aqui antes de chegar na tela.
 */
class PriceTest {

    @Test
    fun `arredonda em escala proporcional ao valor`() {
        // Até R$ 500 a granularidade é de R$ 10.
        assertEquals(190L, roundToAverage(187.43))
        assertEquals(500L, roundToAverage(496.0))
        // Até R$ 2.000, de R$ 50.
        assertEquals(1_300L, roundToAverage(1_287.90))
        assertEquals(1_950L, roundToAverage(1_961.0))
        // Acima disso, de R$ 100.
        assertEquals(4_200L, roundToAverage(4_187.43))
        assertEquals(8_500L, roundToAverage(8_463.20))
    }

    @Test
    fun `nenhum preco formatado sobrevive com precisao de centavos`() {
        // A varredura é o teste de verdade: qualquer valor, em qualquer faixa,
        // tem de sair sem centavos e arredondado. Se alguém trocar o
        // arredondamento por um `toFixed(2)`, isto quebra.
        var v = 49.0
        while (v < 12_000.0) {
            val texto = formatAveragePrice(v)
            assertTrue(texto.startsWith("≈ R$ "), "faltou o ≈ em $v: $texto")
            assertTrue(!texto.contains(","), "centavos vazaram em $v: $texto")
            val cru = texto.removePrefix("≈ R$ ").replace(".", "").toLong()
            assertEquals(roundToAverage(v), cru, "valor não arredondado em $v")
            v += 7.31
        }
    }

    @Test
    fun `agrupa milhar no padrao brasileiro`() {
        assertEquals("≈ R$ 190", formatAveragePrice(187.43))
        assertEquals("≈ R$ 4.200", formatAveragePrice(4_187.43))
        assertEquals("≈ R$ 12.000", formatAveragePrice(11_970.0))
    }

    @Test
    fun `sem preco nao vira zero`() {
        assertEquals("—", formatAveragePrice(null))
        assertEquals("—", formatAveragePrice(0.0))
        // Preço negativo é dado corrompido, não desconto.
        assertEquals("—", formatAveragePrice(-100.0))
        assertEquals("sem preço", formatAveragePrice(null, semPreco = "sem preço"))
    }

    @Test
    fun `fps por mil reais compara peca com peca`() {
        // 120 FPS numa dupla de R$ 4.000 = 30 FPS por R$ 1.000.
        assertEquals(30.0, fpsPerThousandBrl(120, 1_000.0, 3_000.0)!!, 0.001)
    }

    @Test
    fun `meia conta de preco nao vira conta inteira`() {
        // Faltando o preço de uma das duas peças, o número sairia otimista e
        // parecendo completo — pior que não mostrar nada.
        assertNull(fpsPerThousandBrl(120, null, 3_000.0))
        assertNull(fpsPerThousandBrl(120, 1_000.0, null))
        assertNull(fpsPerThousandBrl(120, 0.0, 0.0))
    }

    @Test
    fun `custo por fps ganho recusa ganho inexistente`() {
        assertEquals(180.0, brlPerFpsGained(20, 3_600.0)!!, 0.001)
        // Sem ganho não há custo-benefício a calcular: dividir por zero daria
        // infinito, e um upgrade que piora não tem "preço por FPS".
        assertNull(brlPerFpsGained(0, 3_600.0))
        assertNull(brlPerFpsGained(-5, 3_600.0))
        assertNull(brlPerFpsGained(20, null))
    }
}
