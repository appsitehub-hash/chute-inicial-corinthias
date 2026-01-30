package DB

import com.corinthians.app.Competicao
import com.corinthians.app.Jogador
import com.corinthians.app.Time
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object XmlDatabase {
    private val DB_DIR = File("DB")
    private val DATA_FILE = File(DB_DIR, "data.xml")

    fun loadTimes(): List<Time> {
        val list = mutableListOf<Time>()
        try {
            if (!DATA_FILE.exists()) return list
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(DATA_FILE)
            val timeNodes = doc.getElementsByTagName("time")
            for (i in 0 until timeNodes.length) {
                val tEl = timeNodes.item(i) as Element
                val nome = getText(tEl, "nome")
                if (nome.isBlank()) continue // skip unnamed teams
                val tecnico = getText(tEl, "tecnico")
                val avaliacao = getText(tEl, "avaliacao").toDoubleOrNull() ?: 0.0
                val t = Time(nome, tecnico, avaliacao)
                val jogadoresEl = tEl.getElementsByTagName("jogadores").item(0) as? Element
                if (jogadoresEl != null) {
                    val jogNodes = jogadoresEl.getElementsByTagName("jogador")
                    for (j in 0 until jogNodes.length) {
                        val jEl = jogNodes.item(j) as Element
                        val jnome = getText(jEl, "nome")
                        val jidade = getText(jEl, "idade").toIntOrNull() ?: 0
                        val jpos = getText(jEl, "posicao")
                        val jcat = getText(jEl, "categoria")
                        val jtel = getText(jEl, "telefone")
                        t.adicionarJogador(Jogador(jnome, jidade, jpos, jcat, jtel))
                    }
                }
                list.add(t)
            }
        } catch (e: Exception) {
            System.err.println("Erro ao carregar DB: ${e.message}")
            e.printStackTrace()
        }
        return list
    }

    private fun getText(parent: Element, tag: String): String {
        val nl = parent.getElementsByTagName(tag)
        if (nl == null || nl.length == 0) return ""
        return nl.item(0).textContent
    }

    fun loadCompeticoes(): List<Competicao> {
        val result = mutableListOf<Competicao>()
        try {
            if (!DATA_FILE.exists()) return result
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(DATA_FILE)
            val compNodes = doc.getElementsByTagName("competicao")
            for (i in 0 until compNodes.length) {
                val cEl = compNodes.item(i) as Element
                val nome = getText(cEl, "nome")
                val maxPerdas = getText(cEl, "maxPerdas").toIntOrNull() ?: 0
                val c = Competicao(nome, maxPerdas)
                val cats = cEl.getElementsByTagName("categorias").item(0) as? Element
                if (cats != null) {
                    val catNodes = cats.getElementsByTagName("categoria")
                    for (j in 0 until catNodes.length) {
                        val catEl = catNodes.item(j) as Element
                        c.addCategoria(getText(catEl, "nome"))
                    }
                }
                val parts = cEl.getElementsByTagName("participacoes").item(0) as? Element
                if (parts != null) {
                    val pNodes = parts.getElementsByTagName("participacao")
                    for (j in 0 until pNodes.length) {
                        val pEl = pNodes.item(j) as Element
                        val timeNome = getText(pEl, "time")
                        val categoria = getText(pEl, "categoria")
                        // load perdas/eliminado if present
                        val perdas = getText(pEl, "perdas").toIntOrNull() ?: 0
                        val elim = (getText(pEl, "eliminado").lowercase() == "true")
                        c.addParticipacao(timeNome, categoria)
                        // set the perdas/eliminado on the last added participacao
                        val last = c.participacoes.lastOrNull()
                        if (last != null) { last.perdas = perdas; last.eliminado = elim }
                    }
                }

                // load history if present
                val histEl = cEl.getElementsByTagName("historico").item(0) as? Element
                if (histEl != null) {
                    val pNodes = histEl.getElementsByTagName("partida")
                    for (j in 0 until pNodes.length) {
                        val pEl = pNodes.item(j) as Element
                        val a = getText(pEl, "a")
                        val b = getText(pEl, "b")
                        val vencedor = getText(pEl, "vencedor").ifBlank { null }
                        if (a.isNotBlank() && b.isNotBlank()) c.addHistoryMatch(a, b, vencedor)
                    }
                }
                result.add(c)
            }
        } catch (e: Exception) {
            System.err.println("Erro ao carregar competicoes: ${e.message}")
            e.printStackTrace()
        }
        return result
    }

    // New: load global categories (stored under <categoriasGlobais><categoria>...</categoria></categoriasGlobais>)
    fun loadGlobalCategorias(): List<String> {
        val out = mutableListOf<String>()
        try {
            if (!DATA_FILE.exists()) return out
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(DATA_FILE)
            val catsRoot = doc.getElementsByTagName("categoriasGlobais").item(0) as? Element ?: return out
            val catNodes = catsRoot.getElementsByTagName("categoria")
            for (i in 0 until catNodes.length) {
                val el = catNodes.item(i) as Element
                val name = el.textContent.trim()
                if (name.isNotEmpty()) out.add(name)
            }
        } catch (e: Exception) {
            System.err.println("Erro ao carregar categorias globais: ${e.message}")
            e.printStackTrace()
        }
        return out
    }

    // New: save global categories while preserving times and competicoes
    fun saveGlobalCategorias(categorias: List<String>) {
        try {
            if (!DB_DIR.exists()) DB_DIR.mkdirs()
            val times = loadTimes()
            val comps = loadCompeticoes()

            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.newDocument()
            val root = doc.createElement("data")
            doc.appendChild(root)

            // times
            val timesRoot = doc.createElement("times")
            for (t in times) {
                if (t.nome.isBlank()) continue
                val tEl = doc.createElement("time")
                val nomeEl = doc.createElement("nome"); nomeEl.textContent = t.nome; tEl.appendChild(nomeEl)
                val tecnicoEl = doc.createElement("tecnico"); tecnicoEl.textContent = t.tecnico; tEl.appendChild(tecnicoEl)
                val avEl = doc.createElement("avaliacao"); avEl.textContent = t.avaliacao.toString(); tEl.appendChild(avEl)
                val jogadoresEl = doc.createElement("jogadores")
                for (j in t.getJogadores()) {
                    val jEl = doc.createElement("jogador")
                    val jnome = doc.createElement("nome"); jnome.textContent = j.nome; jEl.appendChild(jnome)
                    val jidade = doc.createElement("idade"); jidade.textContent = j.idade.toString(); jEl.appendChild(jidade)
                    val jpos = doc.createElement("posicao"); jpos.textContent = j.posicao; jEl.appendChild(jpos)
                    val jcat = doc.createElement("categoria"); jcat.textContent = j.categoria; jEl.appendChild(jcat)
                    val jtel = doc.createElement("telefone"); jtel.textContent = j.telefone; jEl.appendChild(jtel)
                    jogadoresEl.appendChild(jEl)
                }
                tEl.appendChild(jogadoresEl)
                timesRoot.appendChild(tEl)
            }
            root.appendChild(timesRoot)

            // competicoes
            val compsRoot = doc.createElement("competicoes")
            for (c in comps) {
                val cEl = doc.createElement("competicao")
                val nomeEl = doc.createElement("nome"); nomeEl.textContent = c.nome; cEl.appendChild(nomeEl)
                val maxEl = doc.createElement("maxPerdas"); maxEl.textContent = c.maxPerdas.toString(); cEl.appendChild(maxEl)
                val catsEl = doc.createElement("categorias")
                for (cat in c.categorias) {
                    val catEl = doc.createElement("categoria")
                    val cname = doc.createElement("nome"); cname.textContent = cat; catEl.appendChild(cname); catsEl.appendChild(catEl)
                }
                cEl.appendChild(catsEl)
                val partsEl = doc.createElement("participacoes")
                for (p in c.participacoes) {
                    val pEl = doc.createElement("participacao")
                    val tname = doc.createElement("time"); tname.textContent = p.timeNome; pEl.appendChild(tname)
                    val catEl = doc.createElement("categoria"); catEl.textContent = p.categoria; pEl.appendChild(catEl)
                    partsEl.appendChild(pEl)
                }
                cEl.appendChild(partsEl)

                // persist history
                val histEl = doc.createElement("historico")
                for (m in c.getHistory()) {
                    val pm = doc.createElement("partida")
                    val a = doc.createElement("a"); a.textContent = m.a; pm.appendChild(a)
                    val b = doc.createElement("b"); b.textContent = m.b; pm.appendChild(b)
                    val v = doc.createElement("vencedor"); v.textContent = m.winner ?: ""; pm.appendChild(v)
                    histEl.appendChild(pm)
                }
                cEl.appendChild(histEl)
                compsRoot.appendChild(cEl)
            }
            root.appendChild(compsRoot)

            // categorias globais
            val globCatsRoot = doc.createElement("categoriasGlobais")
            for (cat in categorias.distinct()) {
                if (cat.isBlank()) continue
                val catEl = doc.createElement("categoria")
                catEl.textContent = cat
                globCatsRoot.appendChild(catEl)
            }
            root.appendChild(globCatsRoot)

            val tf = TransformerFactory.newInstance()
            val tr = tf.newTransformer()
            tr.setOutputProperty(OutputKeys.INDENT, "yes")
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            val src = DOMSource(doc)
            val res = StreamResult(DATA_FILE)
            tr.transform(src, res)
        } catch (e: Exception) {
            System.err.println("Erro ao salvar categorias globais: ${e.message}")
            e.printStackTrace()
        }
    }

    fun saveTimes(times: List<Time>) {
        try {
            if (!DB_DIR.exists()) DB_DIR.mkdirs()
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.newDocument()
            val root = doc.createElement("data")
            doc.appendChild(root)
            val timesRoot = doc.createElement("times")
            root.appendChild(timesRoot)
            for (t in times) {
                if (t.nome.isBlank()) continue // skip unnamed teams
                val tEl = doc.createElement("time")
                val nomeEl = doc.createElement("nome"); nomeEl.textContent = t.nome; tEl.appendChild(nomeEl)
                val tecnicoEl = doc.createElement("tecnico"); tecnicoEl.textContent = t.tecnico; tEl.appendChild(tecnicoEl)
                val avEl = doc.createElement("avaliacao"); avEl.textContent = t.avaliacao.toString(); tEl.appendChild(avEl)
                val jogadoresEl = doc.createElement("jogadores")
                for (j in t.getJogadores()) {
                    val jEl = doc.createElement("jogador")
                    val jnome = doc.createElement("nome"); jnome.textContent = j.nome; jEl.appendChild(jnome)
                    val jidade = doc.createElement("idade"); jidade.textContent = j.idade.toString(); jEl.appendChild(jidade)
                    val jpos = doc.createElement("posicao"); jpos.textContent = j.posicao; jEl.appendChild(jpos)
                    val jcat = doc.createElement("categoria"); jcat.textContent = j.categoria; jEl.appendChild(jcat)
                    val jtel = doc.createElement("telefone"); jtel.textContent = j.telefone; jEl.appendChild(jtel)
                    jogadoresEl.appendChild(jEl)
                }
                tEl.appendChild(jogadoresEl)
                timesRoot.appendChild(tEl)
            }
            // preserve existing competicoes
            val comps = loadCompeticoes()
            val compsRoot = doc.createElement("competicoes")
            for (c in comps) {
                val cEl = doc.createElement("competicao")
                val nomeEl = doc.createElement("nome"); nomeEl.textContent = c.nome; cEl.appendChild(nomeEl)
                val maxEl = doc.createElement("maxPerdas"); maxEl.textContent = c.maxPerdas.toString(); cEl.appendChild(maxEl)
                val catsEl = doc.createElement("categorias")
                for (cat in c.categorias) {
                    val catEl = doc.createElement("categoria")
                    val cname = doc.createElement("nome"); cname.textContent = cat; catEl.appendChild(cname); catsEl.appendChild(catEl)
                }
                cEl.appendChild(catsEl)
                val partsEl = doc.createElement("participacoes")
                for (p in c.participacoes) {
                    val pEl = doc.createElement("participacao")
                    val tname = doc.createElement("time"); tname.textContent = p.timeNome; pEl.appendChild(tname)
                    val catEl = doc.createElement("categoria"); catEl.textContent = p.categoria; pEl.appendChild(catEl)
                    partsEl.appendChild(pEl)
                }
                cEl.appendChild(partsEl)

                // persist history
                val histEl = doc.createElement("historico")
                for (m in c.getHistory()) {
                    val pm = doc.createElement("partida")
                    val a = doc.createElement("a"); a.textContent = m.a; pm.appendChild(a)
                    val b = doc.createElement("b"); b.textContent = m.b; pm.appendChild(b)
                    val v = doc.createElement("vencedor"); v.textContent = m.winner ?: ""; pm.appendChild(v)
                    histEl.appendChild(pm)
                }
                cEl.appendChild(histEl)
                compsRoot.appendChild(cEl)
            }
            root.appendChild(compsRoot)

            // preserve existing global categories if present
            val existingCats = loadGlobalCategorias()
            val globCatsRoot = doc.createElement("categoriasGlobais")
            for (cat in existingCats.distinct()) {
                if (cat.isBlank()) continue
                val catEl = doc.createElement("categoria")
                catEl.textContent = cat
                globCatsRoot.appendChild(catEl)
            }
            root.appendChild(globCatsRoot)

            val tf = TransformerFactory.newInstance()
            val tr = tf.newTransformer()
            tr.setOutputProperty(OutputKeys.INDENT, "yes")
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            val src = DOMSource(doc)
            val res = StreamResult(DATA_FILE)
            tr.transform(src, res)
        } catch (e: Exception) {
            System.err.println("Erro ao salvar DB: ${e.message}")
            e.printStackTrace()
        }
    }

    fun saveCompeticoes(competicoes: List<Competicao>) {
        val times = loadTimes()
        try {
            if (!DB_DIR.exists()) DB_DIR.mkdirs()
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.newDocument()
            val root = doc.createElement("data")
            doc.appendChild(root)
            val timesRoot = doc.createElement("times")
            root.appendChild(timesRoot)
            for (t in times) {
                if (t.nome.isBlank()) continue // skip unnamed teams
                val tEl = doc.createElement("time")
                val nomeEl = doc.createElement("nome"); nomeEl.textContent = t.nome; tEl.appendChild(nomeEl)
                val tecnicoEl = doc.createElement("tecnico"); tecnicoEl.textContent = t.tecnico; tEl.appendChild(tecnicoEl)
                val avEl = doc.createElement("avaliacao"); avEl.textContent = t.avaliacao.toString(); tEl.appendChild(avEl)
                val jogadoresEl = doc.createElement("jogadores")
                for (j in t.getJogadores()) {
                    val jEl = doc.createElement("jogador")
                    val jnome = doc.createElement("nome"); jnome.textContent = j.nome; jEl.appendChild(jnome)
                    val jidade = doc.createElement("idade"); jidade.textContent = j.idade.toString(); jEl.appendChild(jidade)
                    val jpos = doc.createElement("posicao"); jpos.textContent = j.posicao; jEl.appendChild(jpos)
                    val jcat = doc.createElement("categoria"); jcat.textContent = j.categoria; jEl.appendChild(jcat)
                    val jtel = doc.createElement("telefone"); jtel.textContent = j.telefone; jEl.appendChild(jtel)
                    jogadoresEl.appendChild(jEl)
                }
                tEl.appendChild(jogadoresEl)
                timesRoot.appendChild(tEl)
            }
            val compsRoot = doc.createElement("competicoes")
            for (c in competicoes) {
                val cEl = doc.createElement("competicao")
                val nomeEl = doc.createElement("nome"); nomeEl.textContent = c.nome; cEl.appendChild(nomeEl)
                val maxEl = doc.createElement("maxPerdas"); maxEl.textContent = c.maxPerdas.toString(); cEl.appendChild(maxEl)
                val catsEl = doc.createElement("categorias")
                for (cat in c.categorias) {
                    val catEl = doc.createElement("categoria")
                    val cname = doc.createElement("nome"); cname.textContent = cat; catEl.appendChild(cname); catsEl.appendChild(catEl)
                }
                cEl.appendChild(catsEl)
                val partsEl = doc.createElement("participacoes")
                for (p in c.participacoes) {
                    val pEl = doc.createElement("participacao")
                    val tname = doc.createElement("time"); tname.textContent = p.timeNome; pEl.appendChild(tname)
                    val catEl = doc.createElement("categoria"); catEl.textContent = p.categoria; pEl.appendChild(catEl)
                    // persist perdas and eliminado state
                    val perdasEl = doc.createElement("perdas"); perdasEl.textContent = p.perdas.toString(); pEl.appendChild(perdasEl)
                    val elimEl = doc.createElement("eliminado"); elimEl.textContent = p.eliminado.toString(); pEl.appendChild(elimEl)
                    partsEl.appendChild(pEl)
                }
                cEl.appendChild(partsEl)

                // persist history
                val histEl = doc.createElement("historico")
                for (m in c.getHistory()) {
                    val pm = doc.createElement("partida")
                    val a = doc.createElement("a"); a.textContent = m.a; pm.appendChild(a)
                    val b = doc.createElement("b"); b.textContent = m.b; pm.appendChild(b)
                    val v = doc.createElement("vencedor"); v.textContent = m.winner ?: ""; pm.appendChild(v)
                    histEl.appendChild(pm)
                }
                cEl.appendChild(histEl)
                compsRoot.appendChild(cEl)
            }
            root.appendChild(compsRoot)
            val tf = TransformerFactory.newInstance()
            val tr = tf.newTransformer()
            tr.setOutputProperty(OutputKeys.INDENT, "yes")
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            val src = DOMSource(doc)
            val res = StreamResult(DATA_FILE)
            tr.transform(src, res)
        } catch (e: Exception) {
            System.err.println("Erro ao salvar competicoes: ${e.message}")
            e.printStackTrace()
        }
    }
}
