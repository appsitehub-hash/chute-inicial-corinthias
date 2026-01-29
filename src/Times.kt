package com.corinthians.app

import java.util.Scanner

object Times {
    fun menu(times: List<Time>, scanner: Scanner) {
        println("\n--- LISTA DE TIMES ---")
        if (times.isEmpty()) {
            println("Nenhum time cadastrado.")
            return
        }
        times.forEachIndexed { i, t -> println("${i+1}) ${t.nome} (Tecnico: ${t.tecnico}, Avaliacao: ${"%.1f".format(t.avaliacao)})") }
        println("Escolha um numero para ver jogadores do time, ou 0 para voltar.")
        print("Opcao: ")
        if (!scanner.hasNextLine()) return
        val line = scanner.nextLine().trim()
        val opt = line.toIntOrNull()
        if (opt == null) { println("Entrada invalida"); return }
        if (opt == 0) return
        if (opt < 1 || opt > times.size) { println("Opcao invalida"); return }
        Jogadores.menu(times[opt-1], scanner)
    }
}
