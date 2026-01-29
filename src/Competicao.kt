package com.corinthians.app

class Competicao(var nome: String, var maxPerdas: Int) {
    val categorias = mutableListOf<String>()
    val participacoes = mutableListOf<Participacao>()

    fun addCategoria(cat: String) { val c = cat.trim(); if (c.isNotEmpty() && !categorias.contains(c)) categorias.add(c) }
    fun addParticipacao(timeNome: String, categoria: String) { participacoes.add(Participacao(timeNome, categoria)) }

    data class Participacao(val timeNome: String, val categoria: String)

    override fun toString(): String = "Competicao{name='$nome', maxPerdas=$maxPerdas, categorias=$categorias, participantes=${participacoes.size}}"
}
