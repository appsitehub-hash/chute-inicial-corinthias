package com.corinthians.app

class Competicao(var nome: String, var maxPerdas: Int) {
    val categorias = mutableListOf<String>()
    // now store participacoes with perdas state
    val participacoes = mutableListOf<Participacao>()

    fun addCategoria(cat: String) { val c = cat.trim(); if (c.isNotEmpty() && !categorias.contains(c)) categorias.add(c) }
    fun addParticipacao(timeNome: String, categoria: String) { participacoes.add(Participacao(timeNome, categoria, 0, false)) }

    data class Participacao(val timeNome: String, val categoria: String, var perdas: Int = 0, var eliminado: Boolean = false)

    // basic match record (kept in-memory)
    data class Match(val a: String, val b: String, var winner: String? = null)
    private val history = mutableListOf<Match>()

    override fun toString(): String = "Competicao{name='$nome', maxPerdas=$maxPerdas, categorias=$categorias, participantes=${participacoes.size}}"

    // get active (not eliminated) participant names
    fun activeParticipants(): List<String> = participacoes.filter { !it.eliminado }.map { it.timeNome }

    // group active by perdas
    fun groupedByPerdas(): Map<Int, MutableList<String>> {
        val map = mutableMapOf<Int, MutableList<String>>()
        for (p in participacoes) {
            if (p.eliminado) continue
            map.computeIfAbsent(p.perdas) { mutableListOf() }.add(p.timeNome)
        }
        return map
    }

    // generate next round pairings: pair teams with same number of perdas
    // returns list of Match objects (added to history with winner=null)
    fun generateNextRound(): List<Match> {
        val next = mutableListOf<Match>()
        val groups = groupedByPerdas()
        for ((_, list) in groups) {
            val copy = list.toMutableList()
            copy.shuffle()
            var i = 0
            while (i + 1 < copy.size) {
                val a = copy[i]; val b = copy[i+1]
                val m = Match(a,b,null)
                history.add(m)
                next.add(m)
                i += 2
            }
            // if odd, last one gets a bye (no match)
        }
        return next
    }

    // record result: winner name; loser gets perdas++ and may be eliminated
    fun recordResult(winner: String) {
        // find match in history where winner is participant and winner==null
        val m = history.find { it.winner == null && (it.a == winner || it.b == winner) }
        if (m != null) {
            m.winner = winner
            val loser = if (m.a == winner) m.b else m.a
            val part = participacoes.find { it.timeNome == loser }
            if (part != null) {
                part.perdas += 1
                if (part.perdas >= maxPerdas) {
                    part.eliminado = true
                }
            }
        } else {
            // if no matching recent match, try to mark based on names (fallback)
            val loserPart = participacoes.find { it.timeNome != winner && !it.eliminado }
            if (loserPart != null) {
                loserPart.perdas += 1
                if (loserPart.perdas >= maxPerdas) loserPart.eliminado = true
            }
        }
    }

    fun pendingMatches(): List<Match> = history.filter { it.winner == null }

    // convenience: reset history (not persisted)
    fun clearHistory() { history.clear() }

    // expose history for UI and persistence
    fun getHistory(): List<Match> = history.toList()
    fun addHistoryMatch(a: String, b: String, winner: String?) { history.add(Match(a,b,winner)) }
}
