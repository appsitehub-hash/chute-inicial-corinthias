package com.corinthians.app

data class Time(var nome: String, var tecnico: String, var avaliacao: Double) {
    private val jogadores = mutableListOf<Jogador>()
    fun getJogadores(): List<Jogador> = jogadores.toList()
    fun adicionarJogador(jogador: Jogador) { jogadores.add(jogador) }
    override fun toString(): String = "Time{name='$nome', tecnico='$tecnico', avaliacao=${"%.1f".format(avaliacao)}, jogadores=${jogadores.size}}"
}
