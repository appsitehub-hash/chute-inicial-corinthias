package com.corinthians.app

import java.util.Scanner

object AdicionarTime {
    private val times = mutableListOf<Time>()
    private val competicoes = mutableListOf<Competicao>()

    init {
        try {
            val loaded = DB.XmlDatabase.loadTimes()
            if (loaded != null) times.addAll(loaded)
            val loadedC = DB.XmlDatabase.loadCompeticoes()
            if (loadedC != null) competicoes.addAll(loadedC)
        } catch (t: Throwable) {
            System.err.println("Aviso: nao foi possivel carregar DB: ${t.message}")
        }

        // remove any teams with blank/empty names (prevent unnamed teams)
        times.removeIf { it.nome.isBlank() }

        // NOTE: removed automatic seeding to avoid duplicate/sample data being injected on startup.
        // If you need to re-populate sample data for testing, call `repopulateWithSample()` manually.
    }

    private fun seedSampleData() {
        // ...existing code (kept for manual use via repopulateWithSample)
        // Time 1
        val t1 = Time("Timão Kids", "Carlos Silva", 8.2)
        t1.adicionarJogador(Jogador("João Silva", 7, "Atacante"))
        t1.adicionarJogador(Jogador("Miguel Souza", 9, "Meio-campo"))
        t1.adicionarJogador(Jogador("Lucas Rocha", 11, "Zagueiro"))

        // Time 2
        val t2 = Time("Escola Verde", "Ana Pereira", 7.6)
        t2.adicionarJogador(Jogador("Pedro Costa", 12, "Goleiro"))
        t2.adicionarJogador(Jogador("Rafael Lima", 14, "Atacante"))
        t2.adicionarJogador(Jogador("Gabriel Alves", 16, "Meio-campo"))

        // Time 3
        val t3 = Time("Paulista Academy", "Marcos Nunes", 6.9)
        t3.adicionarJogador(Jogador("Matheus Dias", 8, "Atacante"))
        t3.adicionarJogador(Jogador("Enzo Martins", 10, "Meio-campo"))
        t3.adicionarJogador(Jogador("Heitor Ramos", 18, "Zagueiro"))

        // Time 4
        val t4 = Time("Futuro FC", "Sofia Andrade", 5.5)
        t4.adicionarJogador(Jogador("Lucca Fernandes", 5, "Atacante"))
        t4.adicionarJogador(Jogador("Alice Borges", 17, "Meio-campo"))
        t4.adicionarJogador(Jogador("Pedro Henrique", 19, "Zagueiro"))

        times.add(t1)
        times.add(t2)
        times.add(t3)
        times.add(t4)
    }

    private fun seedSampleCompetitions() {
        val c1 = Competicao("Copa Escolinhas", 2)
        // add participants from existing times with categories
        times.take(4).forEach { t ->
            // assign one sample category participation per time, choose the first player's category if available
            val cat = t.getJogadores().firstOrNull()?.categoria ?: ""
            c1.addParticipacao(t.nome, cat)
        }
        competicoes.add(c1)
    }

    fun menu(scanner: Scanner) {
        var running = true
        while (running) {
            println("\n--- PAINEL ADMIN ---")
            println("1. Visualizar times e jogadores")
            println("2. Adicionar time")
            println("3. Criar competicao")
            println("0. Voltar")
            print("Opcao: ")
            if (!scanner.hasNextLine()) break
            val opt = scanner.nextLine().trim()
            when (opt) {
                "1" -> Times.menu(times, scanner)
                "2" -> adicionarTime(scanner)
                "3" -> criarCompeticao(scanner)
                "0" -> running = false
                else -> println("Opcao invalida")
            }
        }
    }

    private fun criarCompeticao(scanner: Scanner) {
        print("Nome da competicao: ")
        if (!scanner.hasNextLine()) return
        val nome = scanner.nextLine().trim()
        var maxPerdas = -1
        while (true) {
            print("Maximo de derrotas permitidas (para eliminacao): ")
            if (!scanner.hasNextLine()) return
            val l = scanner.nextLine().trim()
            try {
                maxPerdas = l.toInt()
                if (maxPerdas < 0) throw NumberFormatException()
                break
            } catch (e: NumberFormatException) {
                println("Numero invalido")
            }
        }
        val c = Competicao(nome, maxPerdas)
        println("Adicionar categorias (digite vazios quando terminar)")
        while (true) {
            print("Categoria: ")
            if (!scanner.hasNextLine()) break
            val cat = scanner.nextLine().trim()
            if (cat.isEmpty()) break
            c.addCategoria(cat)
        }
        if (times.isEmpty()) {
            println("Nenhum time disponivel para participar ainda.")
        } else {
            println("Times disponiveis:")
            for (i in times.indices) println("${i+1}) ${times[i].nome}")
            println("Digite os numeros dos times participantes separados por virgula (ex: 1,3,4) ou vazio para nenhum:")
            print("Selecao: ")
            if (scanner.hasNextLine()) {
                val sel = scanner.nextLine().trim()
                if (sel.isNotEmpty()) {
                    val parts = sel.split(",")
                    for (p in parts) {
                        val idx = p.trim().toIntOrNull()?.minus(1) ?: continue
                        if (idx in times.indices) {
                            if (c.categorias.isEmpty()) {
                                c.addParticipacao(times[idx].nome, "")
                            } else {
                                print("Qual categoria para time ${times[idx].nome}? (escolha entre: ${c.categorias})\nCategoria: ")
                                if (!scanner.hasNextLine()) continue
                                var chosenCat = scanner.nextLine().trim()
                                if (chosenCat.isEmpty()) chosenCat = ""
                                c.addParticipacao(times[idx].nome, chosenCat)
                            }
                        }
                    }
                }
            }
        }
        competicoes.add(c)
        DB.XmlDatabase.saveCompeticoes(competicoes)
        println("Competicao criada: ${c.nome}")
    }

    private fun adicionarTime(scanner: Scanner) {
        print("Nome do time: ")
        if (!scanner.hasNextLine()) return
        val nome = scanner.nextLine().trim()
        print("Nome do tecnico: ")
        if (!scanner.hasNextLine()) return
        val tecnico = scanner.nextLine().trim()
        var avaliacao = -1.0
        while (true) {
            print("Avaliacao (0.0 - 10.0): ")
            if (!scanner.hasNextLine()) return
            val line = scanner.nextLine().trim()
            try {
                avaliacao = line.toDouble()
                if (avaliacao < 0 || avaliacao > 10) throw NumberFormatException()
                break
            } catch (e: NumberFormatException) {
                println("Entrada invalida. Digite um numero entre 0 e 10 (ex: 7.5)")
            }
        }
        val t = Time(nome, tecnico, avaliacao)
        val ok = addTimeObj(t)
        if (ok) println("Time adicionado com sucesso: ${t.nome}") else println("Erro: não foi possível adicionar o time (nome vazio ou já existe).")
    }

    // Returns true if added, false if rejected (blank name or duplicate)
    fun addTimeObj(t: Time): Boolean {
        val name = t.nome.trim()
        if (name.isEmpty()) return false
        // prevent duplicate team names (case-insensitive)
        val exists = times.any { it.nome.equals(name, ignoreCase = true) }
        if (exists) return false
        times.add(t)
        DB.XmlDatabase.saveTimes(times)
        return true
    }

    // Adds player to time; returns true if added, false if rejected (phone already exists)
    fun addPlayerToTime(timeNome: String, jogador: Jogador): Boolean {
        // do not allow creating teams with blank name
        if (timeNome.isBlank()) return false
        // phone uniqueness check: if telefone non-empty and already registered, reject
        val phone = jogador.telefone.trim()
        if (phone.isNotEmpty()) {
            val existingTeam = getTeamByPhone(phone)
            if (existingTeam != null) {
                // already registered under another team
                return false
            }
        }
        val idx = times.indexOfFirst { it.nome == timeNome }
        if (idx >= 0) {
            times[idx].adicionarJogador(jogador)
        } else {
            // create a new time only if name is not duplicate (case-insensitive)
            val exists = times.any { it.nome.equals(timeNome, ignoreCase = true) }
            if (exists) {
                // shouldn't happen, but just in case, add to first matching
                val f = times.indexOfFirst { it.nome.equals(timeNome, ignoreCase = true) }
                if (f >= 0) times[f].adicionarJogador(jogador)
            } else {
                val nt = Time(timeNome, "", 0.0)
                nt.adicionarJogador(jogador)
                times.add(nt)
            }
        }
        DB.XmlDatabase.saveTimes(times)
        return true
    }

    // find team name by phone, or null
    fun getTeamByPhone(telefone: String): String? {
        val phone = telefone.trim()
        if (phone.isEmpty()) return null
        for (t in times) {
            for (j in t.getJogadores()) {
                if (j.telefone.trim() == phone) return t.nome
            }
        }
        return null
    }

    // Returns true if added, false if duplicate or invalid name
    fun addCompeticaoObj(c: Competicao): Boolean {
        val name = c.nome.trim()
        if (name.isEmpty()) return false
        val exists = competicoes.any { it.nome.equals(name, ignoreCase = true) }
        if (exists) return false
        competicoes.add(c)
        DB.XmlDatabase.saveCompeticoes(competicoes)
        return true
    }

    // Update an existing competition (replace by name) or add if missing
    fun updateCompeticao(c: Competicao) {
        val idx = competicoes.indexOfFirst { it.nome == c.nome }
        if (idx >= 0) competicoes[idx] = c else competicoes.add(c)
        DB.XmlDatabase.saveCompeticoes(competicoes)
    }

    // Remove competition by name (returns true if removed)
    fun removeCompeticaoByName(nome: String): Boolean {
        val idx = competicoes.indexOfFirst { it.nome == nome }
        if (idx >= 0) {
            competicoes.removeAt(idx)
            DB.XmlDatabase.saveCompeticoes(competicoes)
            return true
        }
        return false
    }

    // Remove competition by index
    fun removeCompeticaoAt(index: Int): Boolean {
        if (index in competicoes.indices) {
            competicoes.removeAt(index)
            DB.XmlDatabase.saveCompeticoes(competicoes)
            return true
        }
        return false
    }

    // GUI helpers: remove a player by name from a time and persist
    fun removePlayerFromTime(timeNome: String, jogadorNome: String) {
        val idx = times.indexOfFirst { it.nome == timeNome }
        if (idx >= 0) {
            val removed = times[idx].getJogadores().toMutableList().removeIf { it.nome == jogadorNome }
            // rebuild jogadores list in Time: no direct setter, so manipulate underlying list via reflection? Simpler: recreate Time object
            val t = times[idx]
            val keep = t.getJogadores().filter { it.nome != jogadorNome }.toMutableList()
            // Clear existing jogadores by creating new Time and replacing
            val newT = Time(t.nome, t.tecnico, t.avaliacao)
            keep.forEach { newT.adicionarJogador(it) }
            times[idx] = newT
            DB.XmlDatabase.saveTimes(times)
        }
    }

    // persist current times list
    fun saveAllChanges() {
        // ensure we don't persist unnamed teams
        times.removeIf { it.nome.isBlank() }
        DB.XmlDatabase.saveTimes(times)
    }

    fun getTimes(): List<Time> = times
    fun getCompeticoes(): List<Competicao> = competicoes

    // Force repopulate DB with sample data (used for testing via GUI)
    fun repopulateWithSample() {
        times.clear()
        competicoes.clear()
        seedSampleData()
        seedSampleCompetitions()
        DB.XmlDatabase.saveTimes(times)
        DB.XmlDatabase.saveCompeticoes(competicoes)
    }
}
