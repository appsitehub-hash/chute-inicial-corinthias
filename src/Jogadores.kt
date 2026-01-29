package com.corinthians.app

import java.util.Scanner

object Jogadores {
    fun menu(time: Time, scanner: Scanner) {
        println("\n--- JOGADORES DO TIME: ${'$'}{time.nome} ---")
        val jgs = time.getJogadores()
        if (jgs.isEmpty()) println("Nenhum jogador cadastrado neste time.") else jgs.forEachIndexed { i, j -> println("${'$'}{i+1}) ${'$'}{j.nome}, ${'$'}{j.idade} anos, ${'$'}{j.posicao}") }
        println("1. Adicionar jogador")
        println("0. Voltar")
        print("Opcao: ")
        if (!scanner.hasNextLine()) return
        when (scanner.nextLine().trim()) {
            "1" -> adicionarJogador(scanner, time)
            "0" -> return
            else -> println("Opcao invalida")
        }
    }

    private fun adicionarJogador(scanner: Scanner, time: Time) {
        print("Nome do jogador: ")
        if (!scanner.hasNextLine()) return
        val nome = scanner.nextLine().trim()
        var idade = -1
        while (true) {
            print("Idade: ")
            if (!scanner.hasNextLine()) return
            val linha = scanner.nextLine().trim()
            try {
                idade = linha.toInt()
                if (idade < 0) throw NumberFormatException()
                break
            } catch (e: NumberFormatException) {
                println("Idade invalida. Digite um numero inteiro nao negativo.")
            }
        }
        print("Posicao: ")
        if (!scanner.hasNextLine()) return
        val posicao = scanner.nextLine().trim()
        val j = Jogador(nome, idade, posicao)
        time.adicionarJogador(j)
        // reload all times, update this time and save all
        val all = DB.XmlDatabase.loadTimes().toMutableList()
        val idx = all.indexOfFirst { it.nome == time.nome }
        if (idx >= 0) {
            all[idx] = time
        } else {
            all.add(time)
        }
        DB.XmlDatabase.saveTimes(all)
        println("Jogador adicionado: ${'$'}{j.nome}")
    }
}
