package com.corinthians.app

import DB.XmlDatabase

data class Jogador(var nome: String, var idade: Int, var posicao: String = "", var categoria: String = "", var telefone: String = "") {
    init {
        if (categoria.isEmpty()) {
            categoria = defaultCategoriaForAge(idade)
        }
        // normalize telefone formatting: trim
        telefone = telefone.trim()
    }

    companion object {
        // Determina a categoria padrão com base na idade
        fun defaultCategoriaForAge(age: Int): String {
            return when {
                age <= 6 -> "Sub-7"
                age == 7 -> "Sub-8"
                age == 8 -> "Sub-9"
                age == 9 -> "Sub-10"
                age == 10 -> "Sub-11"
                age == 11 -> "Sub-12"
                age == 12 -> "Sub-13"
                age == 13 -> "Sub-14"
                age == 14 -> "Sub-15"
                age == 15 -> "Sub-16"
                age == 16 -> "Sub-17"
                age == 17 -> "Sub-18"
                else -> "Sub-18+"
            }
        }

        // Lista completa de categorias permitidas: tenta carregar do DB, se vazio usa lista padrão
        fun allCategorias(): List<String> {
            try {
                val loaded = XmlDatabase.loadGlobalCategorias()
                if (loaded.isNotEmpty()) return loaded
            } catch (_: Throwable) {}
            return listOf(
                "Sub-7","Sub-8","Sub-9","Sub-10","Sub-11","Sub-12",
                "Sub-13","Sub-14","Sub-15","Sub-16","Sub-17","Sub-18","Sub-18+"
            )
        }
    }
}
