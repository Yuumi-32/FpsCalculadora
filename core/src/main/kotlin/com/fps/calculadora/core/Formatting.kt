package com.fps.calculadora.core

/**
 * Abrevia o nome da CPU pra caber numa linha — porta 1:1 o `shortCPU()` do
 * `index.html` (:1783). Fica no `:core` porque mais de uma tela (Calcular,
 * Upgrade) monta texto de produto que depende dele.
 */
fun shortCpuName(name: String): String = name
    .replace("Ryzen ", "R")
    .replace("Core Ultra ", "Ultra ")
    .replace("Core ", "")
