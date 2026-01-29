package com.corinthians.app

import java.util.Scanner
import com.corinthians.gui.PainelAdminGUI

fun main(args: Array<String>) {
    // se for passado --gui ou -g, abre a GUI e encerra a main (GUI roda em EDT)
    if (args.isNotEmpty()) {
        val a = args[0].trim().lowercase()
        if (a == "--gui" || a == "-g") {
            try {
                PainelAdminGUI.launchGui()
            } catch (t: Throwable) {
                System.err.println("Falha ao iniciar GUI: ${t.message}")
                t.printStackTrace()
            }
            return
        }
    }

    val scanner = Scanner(System.`in`)
    var running = true
    while (running) {
        println("\n=== APP CORINTHIANS ===")
        println("1. Painel Admin")
        println("0. Sair")
        print("Escolha: ")
        if (!scanner.hasNextLine()) break
        val line = scanner.nextLine().trim()
        when (line) {
            "1" -> AdicionarTime.menu(scanner)
            "0" -> {
                running = false
                println("Encerrando aplicacao...")
            }
            else -> println("Opcao invalida")
        }
    }
    scanner.close()
}
