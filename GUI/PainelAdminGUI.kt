package com.corinthians.gui

import javax.swing.*
import java.awt.*
import com.corinthians.app.AdicionarTime
import com.corinthians.app.Time
import com.corinthians.app.Jogador
import com.corinthians.app.Competicao
import kotlin.random.Random
import javax.imageio.ImageIO
import java.io.File
import java.awt.image.BufferedImage
import java.awt.font.GlyphVector
import java.awt.BasicStroke

object PainelAdminGUI {
    private var appIcon: Icon? = null
    private var headerImage: Image? = null

    // color constants (primary red, secondary black, tertiary white)
    private val PRIMARY = Color(0xED, 0x1C, 0x2E)
    private val SECONDARY = Color(0x01, 0x01, 0x01)
    private val TERTIARY = Color(0xFF, 0xFF, 0xFF)

    fun launchGui() {
        // carrega imagens e aplica cores antes de inicializar LAF
        appIcon = loadAppIcon()
        headerImage = loadHeaderImage()
        applyColorPalette()

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (ignored: Exception) {}
        EventQueue.invokeLater { showMainWindow() }
    }

    // tenta encontrar uma imagem raster em IMG para usar como ícone (pega PNG/JPG/GIF)
    private fun loadAppIcon(): Icon? {
        try {
            // checar arquivo absoluto preferencialmente (PNG fornecido pelo usuário)
            val absPreferred = File("/home/master/Área de trabalho/App Corinthians/IMG/Corinthians-Simbolo-Png.png")
            if (absPreferred.exists() && absPreferred.isFile) {
                val img = ImageIO.read(absPreferred) ?: return null
                val processed = tryRemoveBackground(img)
                val scaled = processed.getScaledInstance(48, 48, Image.SCALE_SMOOTH)
                return ImageIcon(scaled)
            }

            val imgDir = File("IMG")
            if (!imgDir.exists() || !imgDir.isDirectory) return null
            val candidates = imgDir.listFiles { f -> f.isFile && (f.name.endsWith(".png", true) || f.name.endsWith(".jpg", true) || f.name.endsWith(".jpeg", true) || f.name.endsWith(".gif", true)) }
            val chosen = if (candidates != null && candidates.isNotEmpty()) candidates[0] else null
            if (chosen == null) return null
            val img = ImageIO.read(chosen) ?: return null
            val processed = tryRemoveBackground(img)
            val scaled = processed.getScaledInstance(48, 48, Image.SCALE_SMOOTH)
            return ImageIcon(scaled)
        } catch (t: Throwable) {
            return null
        }
    }

    // tenta carregar qualquer imagem raster para o header (mais larga)
    private fun loadHeaderImage(): Image? {
        try {
            // checar arquivo absoluto preferencialmente (PNG fornecido pelo usuário)
            val absPreferred = File("/home/master/Área de trabalho/App Corinthians/IMG/Corinthians-Simbolo-Png.png")
            if (absPreferred.exists() && absPreferred.isFile) {
                val img = ImageIO.read(absPreferred) ?: return null
                val processed = tryRemoveBackground(img)
                return processed
            }

            val imgDir = File("IMG")
            if (!imgDir.exists() || !imgDir.isDirectory) return null
            val candidates = imgDir.listFiles { f -> f.isFile && (f.name.endsWith(".png", true) || f.name.endsWith(".jpg", true) || f.name.endsWith(".jpeg", true) || f.name.endsWith(".gif", true)) }
            val chosen = if (candidates != null && candidates.isNotEmpty()) candidates[0] else null
            if (chosen == null) return null
            val img = ImageIO.read(chosen) ?: return null
            val processed = tryRemoveBackground(img)
            return processed
        } catch (t: Throwable) {
            return null
        }
    }

    // tenta remover fundo automaticamente: amostra cantos e torna pixels semelhantes transparentes
    // agora preserva imagens que já têm canal alpha e não sobrescreve alpha existente
    private fun tryRemoveBackground(src: BufferedImage, tolerance: Int = 18): BufferedImage {
        try {
            // se já tem alpha, preservamos (assume fundo já removido)
            if (src.colorModel.hasAlpha()) return src

            val w = src.width
            val h = src.height
            // pegar cores dos 4 cantos e tirar média
            fun rgbComponents(rgb: Int): Triple<Int, Int, Int> {
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                return Triple(r, g, b)
            }
            val corners = listOf(
                src.getRGB(0.coerceAtLeast(0), 0.coerceAtLeast(0)),
                src.getRGB((w - 1).coerceAtLeast(0), 0.coerceAtLeast(0)),
                src.getRGB(0.coerceAtLeast(0), (h - 1).coerceAtLeast(0)),
                src.getRGB((w - 1).coerceAtLeast(0), (h - 1).coerceAtLeast(0))
            )
            var sr = 0; var sg = 0; var sb = 0
            for (c in corners) {
                val (r, g, b) = rgbComponents(c)
                sr += r; sg += g; sb += b
            }
            val avgR = sr / corners.size
            val avgG = sg / corners.size
            val avgB = sb / corners.size

            fun dist(r: Int, g: Int, b: Int): Int {
                val dr = r - avgR
                val dg = g - avgG
                val db = b - avgB
                return kotlin.math.sqrt((dr*dr + dg*dg + db*db).toDouble()).toInt()
            }

            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val rgb = src.getRGB(x, y)
                    val (r, g, b) = rgbComponents(rgb)
                    val d = dist(r, g, b)
                    if (d <= tolerance) {
                        // tornar transparente
                        out.setRGB(x, y, 0)
                    } else {
                        // manter cor original e tornar opaco (alpha 255)
                        val newA = 0xFF
                        val newRgb = (newA shl 24) or (r shl 16) or (g shl 8) or b
                        out.setRGB(x, y, newRgb)
                    }
                }
            }
            return out
        } catch (e: Throwable) {
            return src
        }
    }

    // aplica paleta solicitada: primary #ED1C2E, secondary = black, tertiary = white
    private fun applyColorPalette() {
        try {
            UIManager.put("Panel.background", TERTIARY)
            UIManager.put("OptionPane.background", TERTIARY)
            UIManager.put("OptionPane.messageForeground", SECONDARY)
            UIManager.put("Label.foreground", SECONDARY)
            UIManager.put("Button.background", PRIMARY)
            UIManager.put("Button.foreground", SECONDARY)
            UIManager.put("ComboBox.background", SECONDARY)
            UIManager.put("ComboBox.foreground", TERTIARY)
        } catch (t: Throwable) {
            // ignore
        }
    }

    private fun styledLabel(text: String, size: Int = 14): JLabel {
        val l = JLabel(text)
        l.foreground = SECONDARY
        l.font = Font("SansSerif", Font.BOLD, size)
        return l
    }

    private fun styledField(): JTextField {
        val f = JTextField(20)
        f.background = TERTIARY
        f.foreground = SECONDARY
        f.font = Font("SansSerif", Font.PLAIN, 14)
        return f
    }

    // helper: interpola entre duas cores (t 0.0..1.0)
    private fun lerpColor(a: Color, b: Color, t: Float): Color {
        val r = (a.red + ((b.red - a.red) * t)).toInt().coerceIn(0,255)
        val g = (a.green + ((b.green - a.green) * t)).toInt().coerceIn(0,255)
        val bl = (a.blue + ((b.blue - a.blue) * t)).toInt().coerceIn(0,255)
        return Color(r, g, bl)
    }

    // JLabel com contorno (outline) — desenha primeiro o contorno em `outline` e depois preenche com `foreground`
    private class OutlinedLabel(text: String, font: Font, private val outline: Color, private val strokeSize: Float) : JLabel(text) {
        init {
            this.font = font
            this.horizontalAlignment = SwingConstants.CENTER
            this.verticalAlignment = SwingConstants.CENTER
            this.isOpaque = false
        }

        override fun paintComponent(g: Graphics?) {
            if (g == null) return
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                val text = text ?: return
                val gv: GlyphVector = font.createGlyphVector(g2.fontRenderContext, text)
                val shape = gv.outline
                val bounds = shape.bounds2D
                // center text
                val tx = ((width - bounds.width) / 2.0 - bounds.x).toFloat()
                val ty = ((height - bounds.height) / 2.0 - bounds.y).toFloat()
                g2.translate(tx.toDouble(), ty.toDouble())
                // draw outline
                g2.color = outline
                g2.stroke = BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g2.draw(shape)
                // fill
                g2.color = foreground
                g2.fill(shape)
            } finally {
                g2.dispose()
            }
        }
    }

    // helper para criar botões padronizados (vermelhos por padrão)
    private fun styledButton(text: String, bg: Color = PRIMARY, fgIn: Color? = null, fontSize: Int = 16): JButton {
        val b = JButton(text)
        b.background = bg
        // nova regra: se fundo for SECONDARY (preto) -> texto branco; caso contrário (inclui PRIMARY/vermelho e TERTIARY/branco) -> texto preto
        val fg = fgIn ?: if (bg == SECONDARY) TERTIARY else SECONDARY
        b.foreground = fg
        b.font = Font("SansSerif", Font.BOLD, fontSize)
        b.isOpaque = true
        b.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY.darker(), 2), BorderFactory.createEmptyBorder(8, 14, 8, 14))
        b.isFocusPainted = false
        try { b.putClientProperty("JButton.buttonType", "roundRect") } catch (_: Throwable) {}

        // se botão tiver fundo branco, animar texto entre preto e vermelho no hover
        if (bg == TERTIARY) {
            b.addMouseListener(object : java.awt.event.MouseAdapter() {
                var anim: javax.swing.Timer? = null
                override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                    anim?.stop()
                    val from = b.foreground
                    val to = PRIMARY
                    val steps = 8
                    var step = 0
                    anim = javax.swing.Timer(16) { _ ->
                        step++
                        val t = (step.toFloat() / steps.toFloat()).coerceAtMost(1.0f)
                        b.foreground = lerpColor(from, to, t)
                        if (step >= steps) anim?.stop()
                    }
                    anim?.start()
                }
                override fun mouseExited(e: java.awt.event.MouseEvent?) {
                    anim?.stop()
                    val from = b.foreground
                    val to = SECONDARY
                    val steps = 8
                    var step = 0
                    anim = javax.swing.Timer(16) { _ ->
                        step++
                        val t = (step.toFloat() / steps.toFloat()).coerceAtMost(1.0f)
                        b.foreground = lerpColor(from, to, t)
                        if (step >= steps) anim?.stop()
                    }
                    anim?.start()
                }
            })
        }

        // Ensure LAF or model state doesn't flicker text color on click: while pressed, keep intended foreground
        val intendedFg = b.foreground
        b.model.addChangeListener { _ ->
            try {
                if (b.model.isPressed || b.model.isArmed) {
                    b.foreground = intendedFg
                }
                // when released, don't override (hover animation or original color may apply)
            } catch (_: Throwable) {}
        }

        return b
    }

    // Novo: janela principal com header fixo
    private fun showMainWindow() {
        val frame = JFrame("App Corinthians")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(600, 420)
        frame.setLocationRelativeTo(null)

        val content = JPanel(BorderLayout())
        content.background = SECONDARY

        // header panel (fixo) com imagem centralizada
        val header = JPanel(BorderLayout())
        header.background = PRIMARY
        // header component: prefer headerImage, then appIcon, otherwise an outlined text label
        val headerComponent: JComponent = when {
            headerImage != null -> {
                val scaled = headerImage!!.getScaledInstance(180, 120, Image.SCALE_SMOOTH)
                try { frame.iconImage = headerImage!!.getScaledInstance(48,48, Image.SCALE_SMOOTH) } catch (_: Throwable) {}
                val l = JLabel(ImageIcon(scaled))
                l.horizontalAlignment = SwingConstants.CENTER
                l
            }
            appIcon != null -> {
                val l = JLabel(appIcon)
                l.horizontalAlignment = SwingConstants.CENTER
                l
            }
            else -> {
                // use outlined label: black fill with red outline
                val ol = OutlinedLabel("App Corinthians", Font("SansSerif", Font.BOLD, 28), PRIMARY, 3.0f)
                ol.foreground = SECONDARY
                ol
            }
        }
        headerComponent.isOpaque = false
        header.add(headerComponent, BorderLayout.CENTER)
        header.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)

        // center panel with two big buttons
        val center = JPanel()
        center.layout = GridBagLayout()
        center.background = SECONDARY
        val gbc = GridBagConstraints()
        gbc.gridx = 0; gbc.gridy = 0
        gbc.insets = Insets(8, 16, 8, 16)
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0

        val adminBtn = styledButton("Painel Admin")
        adminBtn.preferredSize = Dimension(240, 60)
        adminBtn.addActionListener { showAdminPanel() }

        val sorteiosBtn = styledButton("Sorteios")
        sorteiosBtn.preferredSize = Dimension(240, 60)
        sorteiosBtn.addActionListener { sorteiosGui() }

        center.add(adminBtn, gbc)
        gbc.gridy = 1
        center.add(sorteiosBtn, gbc)

        val footer = JPanel(BorderLayout())
        footer.background = SECONDARY
        footer.border = BorderFactory.createEmptyBorder(8, 8, 12, 8)

        content.add(header, BorderLayout.NORTH)
        content.add(center, BorderLayout.CENTER)
        content.add(footer, BorderLayout.SOUTH)

        frame.contentPane = content
        frame.isVisible = true
    }

    // ==========================
    // Styled dialogs (Admin flows)
    // ==========================

    private fun showAdminPanel() {
        // modal dialog with simple options
        val d = JDialog()
        d.title = "Painel Admin"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(520, 300))
        d.isResizable = false

        val title = styledLabel("Painel Admin", 18)
        title.border = BorderFactory.createEmptyBorder(12,12,12,12)

        val btnPanel = JPanel()
        btnPanel.background = SECONDARY
        btnPanel.layout = GridLayout(3, 1, 8, 8)
        btnPanel.border = BorderFactory.createEmptyBorder(16, 40, 16, 40)

        val viewBtn = styledButton("Visualizar times")
        val addBtn = styledButton("Adicionar time")
        val catsBtn = styledButton("Categorias")
        val compBtn = styledButton("Criar competição")

        viewBtn.addActionListener { d.dispose(); visualizarTimesDialog() }
        addBtn.addActionListener { d.dispose(); adicionarTimeDialog(null) }
        catsBtn.addActionListener { d.dispose(); manageCategoriesDialog() }
        compBtn.addActionListener { d.dispose(); criarCompeticaoDialog() }

        btnPanel.add(viewBtn); btnPanel.add(addBtn); btnPanel.add(compBtn)
        btnPanel.add(catsBtn)

        d.add(btnPanel, BorderLayout.CENTER)

        val close = styledButton("Fechar")
        close.addActionListener { d.dispose() }
        val foot = JPanel(); foot.background = SECONDARY; foot.add(close)
        d.add(foot, BorderLayout.SOUTH)

        d.setLocationRelativeTo(null)
        d.isVisible = true
    }

    private fun visualizarTimesDialog() {
        val originalTimes = AdicionarTime.getTimes().toMutableList()
        // remove unnamed teams
        originalTimes.removeIf { it.nome.isBlank() }
        // Diagnostic: total players
        var totalPlayers = 0
        for (t in originalTimes) totalPlayers += t.getJogadores().size
        System.err.println("DEBUG: carregar times => ${originalTimes.size} times, ${totalPlayers} jogadores")
        if (originalTimes.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhum time carregado. Verifique o arquivo DB/data.xml ou reinicie o app (DEBUG: 0 times)")
        }
        val d = JDialog()
        d.title = "Times"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(760, 480))
        d.isResizable = true

        val headerPanel = JPanel(BorderLayout())
        headerPanel.background = TERTIARY
        val title = styledLabel("Times Cadastrados", 18)
        title.border = BorderFactory.createEmptyBorder(12, 12, 8, 12)

        // category filter - collect distinct categories across all players
        val todasCategorias = mutableSetOf<String>()
        originalTimes.forEach { t -> t.getJogadores().map { it.categoria }.filter { it.isNotBlank() }.forEach { todasCategorias.add(it) } }
        val catOptions = mutableListOf<String>()
        catOptions.add("Todas")
        catOptions.addAll(todasCategorias.sorted())
        val categoriaCombo = JComboBox(catOptions.toTypedArray())
        categoriaCombo.maximumSize = Dimension(220, 28)
        categoriaCombo.background = TERTIARY

        val search = JTextField()
        search.toolTipText = "Buscar por nome ou técnico"
        search.background = TERTIARY
        search.foreground = SECONDARY
        search.maximumSize = Dimension(Integer.MAX_VALUE, 32)
        val searchWrap = JPanel(); searchWrap.layout = BoxLayout(searchWrap, BoxLayout.X_AXIS); searchWrap.background = TERTIARY
        searchWrap.border = BorderFactory.createEmptyBorder(8,12,8,12)
        searchWrap.add(JLabel("🔎 "))
        searchWrap.add(search)
        searchWrap.add(Box.createRigidArea(Dimension(12,0)))
        searchWrap.add(JLabel("Categoria:")); searchWrap.add(Box.createRigidArea(Dimension(6,0))); searchWrap.add(categoriaCombo)

        headerPanel.add(title, BorderLayout.NORTH)
        headerPanel.add(searchWrap, BorderLayout.SOUTH)

        // Build list entries: each unique pair (team name + category) becomes an entry
        data class TeamView(val teamName: String, val tecnico: String, val avaliacao: Double, val categoria: String, val jogadores: List<Jogador>) {
            override fun toString(): String = if (categoria.isBlank()) teamName else "${teamName} (${categoria})"
        }

        val listModel = DefaultListModel<TeamView>()
        fun rebuildModel(filterText: String, selectedCat: String) {
            listModel.clear()
            val q = filterText.trim().lowercase()
            // for each team, group jogadores by categoria; if categoria filter is 'Todas', include all categories
            for (t in originalTimes) {
                val grouped = t.getJogadores().groupBy { if (it.categoria.isBlank()) "" else it.categoria }
                if (grouped.isEmpty()) {
                    // show team with empty category
                    val tv = TeamView(t.nome, t.tecnico, t.avaliacao, "", listOf())
                    if ((selectedCat == "Todas" || selectedCat == "") && (t.nome.lowercase().contains(q) || t.tecnico.lowercase().contains(q))) listModel.addElement(tv)
                } else {
                    for ((cat, players) in grouped) {
                        if (selectedCat != "Todas" && selectedCat != cat) continue
                        val tv = TeamView(t.nome, t.tecnico, t.avaliacao, cat, players)
                        if (t.nome.lowercase().contains(q) || t.tecnico.lowercase().contains(q) || cat.lowercase().contains(q)) listModel.addElement(tv)
                    }
                }
            }
        }

        rebuildModel("", "Todas")

        val list = JList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.background = TERTIARY

        list.cellRenderer = ListCellRenderer { lst, value, index, isSelected, cellHasFocus ->
            val panel = JPanel(BorderLayout())
            panel.border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
            val name = JLabel(value.toString())
            name.font = Font("SansSerif", Font.BOLD, 14)
            name.foreground = if (isSelected) TERTIARY else SECONDARY

            val playersCount = JLabel("Jogadores: ${value.jogadores.size}  •  Téc: ${value.tecnico}  •  Av: ${"%.1f".format(value.avaliacao)}")
            playersCount.font = Font("SansSerif", Font.PLAIN, 12)
            playersCount.foreground = if (isSelected) TERTIARY else Color(120,120,120)

            val left = JPanel(); left.layout = BoxLayout(left, BoxLayout.Y_AXIS); left.isOpaque = false
            left.add(name); left.add(Box.createRigidArea(Dimension(0,6))); left.add(playersCount)

            panel.add(left, BorderLayout.CENTER)
            panel.background = if (isSelected) PRIMARY else if (index % 2 == 0) Color(245,245,245) else TERTIARY
            panel
        }

        val scroll = JScrollPane(list)
        scroll.border = BorderFactory.createLineBorder(Color(200,200,200))

        // right action buttons
        val btnView = styledButton("Ver jogadores")
        val btnAdd = styledButton("Adicionar time")
        val btnClose = styledButton("Voltar")

        fun refresh() {
            rebuildModel(search.text, categoriaCombo.selectedItem as String)
        }

        // wire filter controls
        search.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = refresh()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = refresh()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = refresh()
        })
        categoriaCombo.addActionListener { refresh() }

        // double click opens details (shows only players of that category instance)
        list.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val sel = list.selectedValue
                    if (sel != null) showTimeDetailDialogForCategory(sel.teamName, sel.categoria, d)
                }
            }
        })

        btnView.addActionListener {
            val sel = list.selectedValue
            if (sel == null) JOptionPane.showMessageDialog(d, "Selecione um time.") else showTimeDetailDialogForCategory(sel.teamName, sel.categoria, d)
        }
        btnAdd.addActionListener {
            adicionarTimeDialog(d)
            // reload data
            originalTimes.clear(); originalTimes.addAll(AdicionarTime.getTimes()); rebuildModel(search.text, categoriaCombo.selectedItem as String)
        }
        btnClose.addActionListener { d.dispose() }

        val right = JPanel(); right.layout = BoxLayout(right, BoxLayout.Y_AXIS); right.background = SECONDARY
        right.border = BorderFactory.createEmptyBorder(12,12,12,12)
        right.add(btnView); right.add(Box.createRigidArea(Dimension(0,10))); right.add(btnAdd); right.add(Box.createRigidArea(Dimension(0,10))); right.add(btnClose)

        d.add(headerPanel, BorderLayout.NORTH)
        d.add(scroll, BorderLayout.CENTER)
        d.add(right, BorderLayout.EAST)

        d.setLocationRelativeTo(null)
        d.isVisible = true
    }

    // new helper: show details for a particular team + category (if categoria blank => show all players)
    private fun showTimeDetailDialogForCategory(teamName: String, categoria: String, parent: Window?) {
        val times = AdicionarTime.getTimes().filter { it.nome == teamName }
        if (times.isEmpty()) { JOptionPane.showMessageDialog(null, "Time não encontrado: $teamName"); return }
        val time = times.first()
        val d = JDialog()
        d.title = "Detalhes: ${time.nome} ${if (categoria.isNotBlank()) "($categoria)" else ""}"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(560, 460))
        d.isResizable = false

        val header = JPanel(BorderLayout())
        header.background = SECONDARY
        val title = styledLabel(time.nome, 18)
        title.border = BorderFactory.createEmptyBorder(12,12,8,12)
        val meta = JLabel("Técnico: ${time.tecnico}   •   Avaliação: ${"%.1f".format(time.avaliacao)}   •   Categoria: ${if (categoria.isBlank()) "Todas" else categoria}")
        meta.foreground = Color(100,100,100)
        meta.font = Font("SansSerif", Font.PLAIN, 13)
        meta.border = BorderFactory.createEmptyBorder(0,12,12,12)
        header.add(title, BorderLayout.NORTH)
        header.add(meta, BorderLayout.SOUTH)

        // players list filtered by category if provided
        val players = if (categoria.isBlank()) time.getJogadores().toMutableList() else time.getJogadores().filter { it.categoria == categoria }.toMutableList()
        val model = DefaultListModel<Jogador>()
        players.forEach { model.addElement(it) }
        val list = JList(model)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.background = TERTIARY

        list.cellRenderer = ListCellRenderer { _, value, index, isSelected, _ ->
            val p = JPanel(BorderLayout())
            p.border = BorderFactory.createEmptyBorder(8,10,8,10)
            p.isOpaque = true
            val name = JLabel(value.nome)
            name.font = Font("SansSerif", Font.BOLD, 14)
            name.foreground = SECONDARY
            val detail = JLabel("${value.idade} anos  •  ${value.posicao}  •  ${value.categoria}")
            detail.font = Font("SansSerif", Font.PLAIN, 12)
            detail.foreground = Color(110,110,110)
            val left = JPanel(); left.layout = BoxLayout(left, BoxLayout.Y_AXIS); left.isOpaque = false
            left.add(name); left.add(Box.createRigidArea(Dimension(0,6))); left.add(detail)
            p.add(left, BorderLayout.CENTER)
            p.background = if (isSelected) PRIMARY else TERTIARY
            p
        }

        val scroll = JScrollPane(list)
        scroll.border = BorderFactory.createLineBorder(Color(200,200,200))

        // actions
        val addBtn = styledButton("Adicionar")
        val editBtn = styledButton("Editar")
        val removeBtn = styledButton("Remover")
        val closeBtn = styledButton("Fechar")

        fun reload() {
            model.clear()
            val newPlayers = if (categoria.isBlank()) time.getJogadores() else time.getJogadores().filter { it.categoria == categoria }
            newPlayers.forEach { model.addElement(it) }
        }

        addBtn.addActionListener {
            adicionarJogadorDialog(time, d)
            reload()
        }
        editBtn.addActionListener {
            val sel = list.selectedValue
            if (sel == null) { JOptionPane.showMessageDialog(d, "Selecione um jogador para editar."); return@addActionListener }
            // reuse adicionar dialog fields but prefill
            val edt = JDialog(d)
            edt.title = "Editar Jogador"
            edt.isModal = true
            edt.layout = BorderLayout()
            edt.setSize(Dimension(380,220))
            val panel = JPanel(GridBagLayout())
            panel.background = TERTIARY
            val gbc = GridBagConstraints(); gbc.insets = Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx=0; gbc.gridy=0
            panel.add(styledLabel("Nome:"), gbc)
            gbc.gridx=1; val nField = styledField(); nField.text = sel.nome; panel.add(nField, gbc)
            gbc.gridx=0; gbc.gridy=1; panel.add(styledLabel("Idade:"), gbc)
            gbc.gridx=1; val iField = styledField(); iField.text = sel.idade.toString(); panel.add(iField, gbc)
            gbc.gridx=0; gbc.gridy=2; panel.add(styledLabel("Posição:"), gbc)
            gbc.gridx=1; val pField = styledField(); pField.text = sel.posicao; panel.add(pField, gbc)
            gbc.gridx=0; gbc.gridy=3; panel.add(styledLabel("Categoria:"), gbc)
            gbc.gridx=1
            val categoriaCombo = JComboBox(Jogador.allCategorias().toTypedArray())
            categoriaCombo.background = TERTIARY
            categoriaCombo.maximumSize = Dimension(Integer.MAX_VALUE, 28)
            categoriaCombo.selectedItem = sel.categoria
            panel.add(categoriaCombo, gbc)
            val save = styledButton("Salvar"); val cancel = styledButton("Cancelar")
            save.addActionListener {
                val newName = nField.text.trim(); val newId = iField.text.trim().toIntOrNull(); val newPos = pField.text.trim(); val newCat = (categoriaCombo.selectedItem as? String) ?: ""
                if (newName.isEmpty() || newId == null) { JOptionPane.showMessageDialog(edt, "Preencha nome e idade válidos."); return@addActionListener }
                sel.nome = newName
                sel.idade = newId
                sel.posicao = newPos
                sel.categoria = newCat
                AdicionarTime.saveAllChanges()
                reload()
                edt.dispose()
            }
            cancel.addActionListener { edt.dispose() }
            val foot = JPanel(); foot.background = TERTIARY; foot.add(save); foot.add(cancel)
            edt.add(panel, BorderLayout.CENTER); edt.add(foot, BorderLayout.SOUTH)
            edt.setLocationRelativeTo(d); edt.isVisible = true
        }
        removeBtn.addActionListener {
            val sel = list.selectedValue
            if (sel == null) { JOptionPane.showMessageDialog(d, "Selecione um jogador para remover."); return@addActionListener }
            val ok = JOptionPane.showConfirmDialog(d, "Remover ${sel.nome}?", "Confirmar", JOptionPane.YES_NO_OPTION)
            if (ok == JOptionPane.YES_OPTION) {
                AdicionarTime.removePlayerFromTime(time.nome, sel.nome)
                reload()
            }
        }
        closeBtn.addActionListener { d.dispose() }

        list.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val sel = list.selectedValue
                    if (sel != null) editBtn.doClick()
                }
            }
        })

        val right = JPanel(); right.layout = BoxLayout(right, BoxLayout.Y_AXIS); right.background = TERTIARY; right.border = BorderFactory.createEmptyBorder(12,12,12,12)
        right.add(addBtn); right.add(Box.createRigidArea(Dimension(0,8))); right.add(editBtn); right.add(Box.createRigidArea(Dimension(0,8))); right.add(removeBtn); right.add(Box.createVerticalGlue()); right.add(closeBtn)

        d.add(header, BorderLayout.NORTH)
        d.add(scroll, BorderLayout.CENTER)
        d.add(right, BorderLayout.EAST)

        d.setLocationRelativeTo(null)
        d.isVisible = true
    }

    private fun adicionarTimeDialog(parent: Window?) {
        val d = JDialog()
        d.title = "Adicionar Time"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(480, 300))
        d.isResizable = false

        val panel = JPanel(); panel.layout = GridBagLayout(); panel.background = TERTIARY
        val gbc = GridBagConstraints(); gbc.insets = Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx=0; gbc.gridy=0

        panel.add(styledLabel("Nome:"), gbc)
        gbc.gridx=1; val nomeField = styledField(); panel.add(nomeField, gbc)
        gbc.gridx=0; gbc.gridy=1; panel.add(styledLabel("Técnico:"), gbc)
        gbc.gridx=1; val tecnicoField = styledField(); panel.add(tecnicoField, gbc)
        gbc.gridx=0; gbc.gridy=2; panel.add(styledLabel("Avaliação (0-10):"), gbc)
        gbc.gridx=1; val avField = styledField(); panel.add(avField, gbc)

        val save = styledButton("Salvar")
        val cancel = styledButton("Cancelar")
        save.addActionListener {
            val nome = nomeField.text.trim(); val tecnico = tecnicoField.text.trim(); val av = avField.text.trim().replace(',', '.').toDoubleOrNull()
            if (nome.isEmpty() || tecnico.isEmpty() || av == null || av < 0.0 || av > 10.0) {
                JOptionPane.showMessageDialog(d, "Preencha os dados corretamente (av entre 0 e 10).")
                return@addActionListener
            }
            val t = Time(nome, tecnico, av)
            AdicionarTime.addTimeObj(t)
            JOptionPane.showMessageDialog(d, "Time adicionado: ${t.nome}")
            d.dispose()
        }
        cancel.addActionListener { d.dispose() }

        val foot = JPanel(); foot.background = TERTIARY; foot.add(save); foot.add(cancel)

        d.add(panel, BorderLayout.CENTER)
        d.add(foot, BorderLayout.SOUTH)
        // ensure dialog background matches inputs so black text is visible
        try { d.contentPane.background = TERTIARY } catch (_: Throwable) {}
        d.setLocationRelativeTo(null)
        d.isVisible = true
    }

    private fun adicionarJogadorDialog(time: Time, parent: Window?) {
        val d = JDialog()
        d.title = "Adicionar Jogador - ${time.nome}"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(420, 260))
        d.isResizable = false

        val panel = JPanel(); panel.layout = GridBagLayout(); panel.background = SECONDARY
        val gbc = GridBagConstraints(); gbc.insets = Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx=0; gbc.gridy=0

        panel.add(styledLabel("Nome:"), gbc)
        gbc.gridx=1; val nomeField = styledField(); panel.add(nomeField, gbc)
        gbc.gridx=0; gbc.gridy=1; panel.add(styledLabel("Idade:"), gbc)
        gbc.gridx=1; val idadeField = styledField(); panel.add(idadeField, gbc)
        gbc.gridx=0; gbc.gridy=2; panel.add(styledLabel("Posição:"), gbc)
        gbc.gridx=1; val posField = styledField(); panel.add(posField, gbc)
        gbc.gridx=0; gbc.gridy=3; panel.add(styledLabel("Categoria:"), gbc)
        gbc.gridx=1
        // category combo prefilled with default based on age
        val categoriaCombo = JComboBox(Jogador.allCategorias().toTypedArray())
        categoriaCombo.background = TERTIARY
        categoriaCombo.maximumSize = Dimension(Integer.MAX_VALUE, 28)
        panel.add(categoriaCombo, gbc)

        val save = styledButton("Salvar")
        val cancel = styledButton("Cancelar")
        save.addActionListener {
            val nome = nomeField.text.trim(); val idade = idadeField.text.trim().toIntOrNull(); val pos = posField.text.trim(); val cat = (categoriaCombo.selectedItem as? String) ?: ""
            if (nome.isEmpty() || idade == null || idade < 0) { JOptionPane.showMessageDialog(d, "Preencha nome e idade válidos."); return@addActionListener }
            val j = Jogador(nome, idade, pos, cat)
            AdicionarTime.addPlayerToTime(time.nome, j)
            JOptionPane.showMessageDialog(d, "Jogador adicionado: ${j.nome}")
            d.dispose()
        }
        cancel.addActionListener { d.dispose() }

        val foot = JPanel(); foot.background = SECONDARY; foot.add(save); foot.add(cancel)

        d.add(panel, BorderLayout.CENTER)
        d.add(foot, BorderLayout.SOUTH)
        d.setLocationRelativeTo(null)
        d.isVisible = true
    }

    private fun criarCompeticaoDialog() {
        val d = JDialog()
        d.title = "Criar Competição"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(1000, 600))
        d.isResizable = true

        // Header (compact)
        val header = JPanel(BorderLayout())
        header.background = SECONDARY
        val title = styledLabel("Criar Competição", 20)
        title.border = BorderFactory.createEmptyBorder(14,14,6,12)
        val hint = JLabel("Preencha nome, regras e selecione participantes")
        hint.foreground = Color(150,150,150)
        hint.font = Font("SansSerif", Font.PLAIN, 13)
        hint.border = BorderFactory.createEmptyBorder(0,14,12,12)
        header.add(title, BorderLayout.NORTH)
        header.add(hint, BorderLayout.SOUTH)

        // Use JSplitPane for resizable, modern feel
        val leftPanel = JPanel(GridBagLayout())
        leftPanel.background = TERTIARY
        leftPanel.minimumSize = Dimension(380, 300)

        val gc = GridBagConstraints()
        gc.insets = Insets(10,14,10,14)
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.gridx = 0; gc.gridy = 0

        // allow label wrapping with HTML if necessary
        val nameLabel = JLabel("<html><b>Nome da competição:</b></html>")
        nameLabel.foreground = SECONDARY
        nameLabel.font = Font("SansSerif", Font.BOLD, 14)
        leftPanel.add(nameLabel, gc)
        gc.gridx = 1
        val nomeField = styledField(); nomeField.toolTipText = "Ex: Copa da Escolinha"; leftPanel.add(nomeField, gc)

        gc.gridx = 0; gc.gridy = 1
        val maxLabel = JLabel("<html><b>Max de derrotas (elim):</b></html>")
        maxLabel.foreground = SECONDARY
        maxLabel.font = Font("SansSerif", Font.BOLD, 14)
        leftPanel.add(maxLabel, gc)
        gc.gridx = 1
        val maxField = styledField(); maxField.toolTipText = "Quantidade de derrotas para eliminação (0 para nenhuma)"; leftPanel.add(maxField, gc)

        // Categories box
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2
        // category panel now shows only existing global categories and a selected-list
        val catBox = JPanel()
        catBox.background = TERTIARY
        catBox.layout = BoxLayout(catBox, BoxLayout.Y_AXIS)
        catBox.border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color(220,220,220)), "Categorias")

        // load global categories (fallback to default list)
        val catModel = DefaultListModel<String>()
        try {
            val cats = DB.XmlDatabase.loadGlobalCategorias()
            if (cats.isNotEmpty()) cats.forEach { catModel.addElement(it) } else Jogador.allCategorias().forEach { catModel.addElement(it) }
        } catch (_: Throwable) { Jogador.allCategorias().forEach { catModel.addElement(it) } }

        // checklist: show categories with checkboxes so user can toggle multiple explicitly
        val checkedCats = mutableSetOf<String>()

        val catList = JList(catModel)
        catList.visibleRowCount = 8
        catList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        catList.background = TERTIARY
        catList.fixedCellHeight = 28

        // renderer that draws a checkbox for each item, checked when in checkedCats
        class CheckBoxListRenderer : JCheckBox(), ListCellRenderer<String> {
            init {
                this.isOpaque = true
            }
            override fun getListCellRendererComponent(list: JList<out String>?, value: String?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                // set displayed text
                text = value ?: ""
                // configure colors and padding
                foreground = SECONDARY
                background = if (index % 2 == 0) TERTIARY else Color(245,245,245)
                border = BorderFactory.createEmptyBorder(4,8,4,8)
                // checkbox state reflects whether this category is checked
                this.isSelected = value != null && checkedCats.contains(value)
                return this
            }
        }

        catList.cellRenderer = CheckBoxListRenderer()
        val catScroll = JScrollPane(catList)
        catScroll.preferredSize = Dimension(360, 160)

        // list that shows currently selected categories (keeps selection visible and scrollable)
        val selectedModel = DefaultListModel<String>()
        val selectedList = JList(selectedModel)
        selectedList.background = TERTIARY
        selectedList.fixedCellHeight = 26
        val selectedScroll = JScrollPane(selectedList)
        selectedScroll.preferredSize = Dimension(360, 80)

        // placeholder refresh function — assigned after participant list is created
        var refreshParticipants: () -> Unit = { }

         // toggle checkbox on click
         catList.addMouseListener(object : java.awt.event.MouseAdapter() {
             override fun mouseClicked(e: java.awt.event.MouseEvent) {
                 val idx = catList.locationToIndex(e.point)
                 if (idx < 0) return
                 val value = catModel.get(idx)
                 if (checkedCats.contains(value)) checkedCats.remove(value) else checkedCats.add(value)
                 // update selected list model
                 selectedModel.clear()
                 checkedCats.forEach { selectedModel.addElement(it) }
                 // repaint list to update checkboxes
                 catList.repaint()
                 try { refreshParticipants() } catch (_: Throwable) {}
             }
         })

        // layout assembly for catBox
        catBox.add(Box.createRigidArea(Dimension(0,6)))
        val topWrap = JPanel(BorderLayout()); topWrap.background = TERTIARY; topWrap.add(JLabel("Categorias disponíveis:"), BorderLayout.NORTH); topWrap.add(catScroll, BorderLayout.CENTER)
        topWrap.alignmentX = Component.LEFT_ALIGNMENT
        catBox.add(topWrap)
        catBox.add(Box.createRigidArea(Dimension(0,8)))
        val selWrap = JPanel(BorderLayout()); selWrap.background = TERTIARY; selWrap.add(JLabel("Categorias selecionadas:"), BorderLayout.NORTH); selWrap.add(selectedScroll, BorderLayout.CENTER)
        selWrap.alignmentX = Component.LEFT_ALIGNMENT
        catBox.add(selWrap)
        catBox.maximumSize = Dimension(Integer.MAX_VALUE, 380)
        leftPanel.add(catBox, gc)

        // Right side - participants with search and renderer
        val rightPanel = JPanel(BorderLayout())
        rightPanel.background = TERTIARY
        rightPanel.minimumSize = Dimension(380,300)
        rightPanel.border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color(220,220,220)), "Participantes (selecione)")

        val participantTop = JPanel(); participantTop.background = TERTIARY
        val pSearch = styledField(); pSearch.toolTipText = "Buscar participantes"
        pSearch.preferredSize = Dimension(220,28)
        participantTop.add(pSearch)

        // times list model used by participants list
        val timesListModel = DefaultListModel<String>()
        val partList = JList(timesListModel); partList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        partList.cellRenderer = ListCellRenderer { _, value, index, isSelected, _ ->
            val p = JPanel(BorderLayout())
            p.border = BorderFactory.createEmptyBorder(6,8,6,8)
            p.isOpaque = true
            val name = JLabel(value)
            name.font = Font("SansSerif", Font.BOLD, 13)
            name.foreground = if (isSelected) TERTIARY else SECONDARY
            // try to show category if time exists
            val t = AdicionarTime.getTimes().find { it.nome == value }
            val catLabel = JLabel(if (t != null && t.getJogadores().isNotEmpty()) "" else "")
            if (t != null) {
                val cats = t.getJogadores().map { it.categoria }.distinct().filter { it.isNotEmpty() }
                if (cats.isNotEmpty()) catLabel.text = " (${cats.joinToString(",")})" else catLabel.text = ""
            }
            catLabel.font = Font("SansSerif", Font.PLAIN, 12)
            catLabel.foreground = Color(110,110,110)
            val center = JPanel(); center.layout = BoxLayout(center, BoxLayout.X_AXIS); center.isOpaque = false
            center.add(name); center.add(catLabel)
            p.add(center, BorderLayout.CENTER)
            p.background = if (isSelected) PRIMARY else TERTIARY
            p
        }
        val partScroll = JScrollPane(partList)

        // implement refreshParticipants now that timesListModel exists
        refreshParticipants = {
            val selectedCats = checkedCats.toSet()
            val q = pSearch.text.trim().lowercase()
            timesListModel.clear()
            val teams = AdicionarTime.getTimes()
            teams.filter { team ->
                // match search
                val matchesSearch = q.isEmpty() || team.nome.lowercase().contains(q) || team.tecnico.lowercase().contains(q)
                if (!matchesSearch) return@filter false
                // if no categories selected -> include
                if (selectedCats.isEmpty()) return@filter true
                // else include if any player in team matches any selected category
                team.getJogadores().any { p -> p.categoria in selectedCats }
            }.forEach { timesListModel.addElement(it.nome) }
        }

        // filter participants on search
        pSearch.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = refreshParticipants()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = refreshParticipants()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = refreshParticipants()
        })

        val pControls = JPanel(); pControls.background = TERTIARY
        val selectAll = styledButton("Selecionar tudo"); val clearSel = styledButton("Limpar seleção")
        selectAll.addActionListener { if (timesListModel.size() > 0) partList.setSelectionInterval(0, timesListModel.size()-1) }
        clearSel.addActionListener { partList.clearSelection() }
        pControls.add(selectAll); pControls.add(clearSel)

        rightPanel.add(participantTop, BorderLayout.NORTH)
        rightPanel.add(partScroll, BorderLayout.CENTER)
        rightPanel.add(pControls, BorderLayout.SOUTH)

        // ensure initial participants populated
        refreshParticipants()

        // Use split pane so user can resize columns
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
        split.setResizeWeight(0.5)
        split.setOneTouchExpandable(true)

        // Footer
        val save = styledButton("Criar Competição"); save.preferredSize = Dimension(200, 44)
        val cancel = styledButton("Cancelar"); cancel.preferredSize = Dimension(140, 44)
        save.addActionListener {
            val name = nomeField.text.trim(); val maxP = maxField.text.trim().toIntOrNull()
            if (name.isEmpty() || maxP == null || maxP < 0) { JOptionPane.showMessageDialog(d, "Preencha o nome e max perdas corretamente."); return@addActionListener }
            val c = Competicao(name, maxP)
            for (i in 0 until selectedModel.size()) c.addCategoria(selectedModel.get(i))
            val sel = partList.selectedValuesList
            for (p in sel) c.addParticipacao(p, "")
            AdicionarTime.addCompeticaoObj(c)
            JOptionPane.showMessageDialog(d, "Competição criada: ${c.nome}")
            d.dispose()
        }
        cancel.addActionListener { d.dispose() }
        val foot = JPanel(); foot.background = TERTIARY; foot.border = BorderFactory.createEmptyBorder(14,14,14,14); foot.add(save); foot.add(Box.createRigidArea(Dimension(12,0))); foot.add(cancel)

        d.add(header, BorderLayout.NORTH)
        d.add(split, BorderLayout.CENTER)
        d.add(foot, BorderLayout.SOUTH)
        d.setLocationRelativeTo(null)
        d.isVisible = true
    }

    private fun sorteiosGui() {
        val comps = AdicionarTime.getCompeticoes().toMutableList()
        if (comps.isEmpty()) { JOptionPane.showMessageDialog(null, "Nenhuma competição criada."); return }
        val d = JDialog()
        d.title = "Sorteios"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(560, 420))
        d.isResizable = false

        val listModel = DefaultListModel<String>(); comps.forEach { listModel.addElement("${it.nome} (${it.participacoes.size})") }
        val list = JList(listModel); list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val scroll = JScrollPane(list)

        val openBtn = styledButton("Abrir")
        val sortBtn = styledButton("Sortear próximos")
        val closeBtn = styledButton("Fechar")

        openBtn.addActionListener {
            val idx = list.selectedIndex; if (idx < 0) JOptionPane.showMessageDialog(d, "Selecione uma competição.") else showCompetitionDialog(comps[idx], d)
        }
        sortBtn.addActionListener {
            val idx = list.selectedIndex; if (idx < 0) JOptionPane.showMessageDialog(d, "Selecione uma competição.") else {
                val chosen = comps[idx]
                val participants = chosen.participacoes.map { it.timeNome }.toMutableList()
                if (participants.size < 2) { JOptionPane.showMessageDialog(d, "É necessário pelo menos 2 times para sortear."); return@addActionListener }
                participants.shuffle(Random(System.currentTimeMillis()))
                val sb = StringBuilder(); var i = 0
                while (i + 1 < participants.size) { sb.append("${participants[i]}  x  ${participants[i+1]}\n"); i += 2 }
                if (i < participants.size) sb.append("${participants[i]} (folga)\n")
                JOptionPane.showMessageDialog(d, sb.toString(), "Resultado do Sorteio", JOptionPane.INFORMATION_MESSAGE)
            }
        }
        closeBtn.addActionListener { d.dispose() }

        val right = JPanel(); right.layout = BoxLayout(right, BoxLayout.Y_AXIS); right.background = SECONDARY; right.border = BorderFactory.createEmptyBorder(8,8,8,8)
        right.add(openBtn); right.add(Box.createRigidArea(Dimension(0,8))); right.add(sortBtn); right.add(Box.createRigidArea(Dimension(0,8))); right.add(closeBtn)

        d.add(styledLabel("Competições", 16), BorderLayout.NORTH)
        d.add(scroll, BorderLayout.CENTER)
        d.add(right, BorderLayout.EAST)

        d.setLocationRelativeTo(null); d.isVisible = true
    }

    private fun showCompetitionDialog(c: Competicao, parent: Window?) {
        val d = JDialog()
        d.title = "Competição: ${c.nome}"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(520, 420))
        d.isResizable = false
        val info = JTextArea(); info.isEditable = false; info.background = SECONDARY; info.foreground = TERTIARY; info.font = Font("SansSerif", Font.PLAIN, 14)
        val sb = StringBuilder(); sb.append("Nome: ${c.nome}\nMax perdas: ${c.maxPerdas}\nCategorias: ${c.categorias}\n\nParticipantes:\n")
        if (c.participacoes.isEmpty()) sb.append("(nenhum)\n") else c.participacoes.forEachIndexed { i, p -> sb.append("${i+1}) ${p.timeNome} ${if (p.categoria.isNotEmpty()) "(cat: ${p.categoria})" else ""}\n") }
        info.text = sb.toString()
        d.add(styledLabel("Competição"), BorderLayout.NORTH)
        d.add(JScrollPane(info), BorderLayout.CENTER)
        val close = styledButton("Fechar"); close.addActionListener { d.dispose() }
        val foot = JPanel(); foot.background = SECONDARY; foot.add(close)
        d.add(foot, BorderLayout.SOUTH)
        d.setLocationRelativeTo(null); d.isVisible = true
    }

    private fun doSorteio(c: Competicao) {
        val participants = c.participacoes.map { it.timeNome }.toMutableList()
        if (participants.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Sem participantes para sortear.")
            return
        }
        participants.shuffle(Random(System.currentTimeMillis()))
        val sb = StringBuilder()
        sb.append("Resultados do sorteio:\n")
        var i = 0
        while (i < participants.size) {
            val a = participants[i]
            val b = if (i + 1 < participants.size) participants[i + 1] else null
            if (b != null) sb.append("${a}  x  ${b}\n") else sb.append("${a}  (folga)\n")
            i += 2
        }
        JOptionPane.showMessageDialog(null, sb.toString())
    }

    private fun manageCategoriesDialog() {
        val d = JDialog()
        d.title = "Gerenciar Categorias"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(480, 420))
        d.isResizable = false

        val panel = JPanel(BorderLayout())
        panel.background = TERTIARY
        val model = DefaultListModel<String>()
        // load categories from DB (or fallback)
        try {
            val cats = DB.XmlDatabase.loadGlobalCategorias()
            if (cats.isNotEmpty()) cats.forEach { model.addElement(it) }
            else Jogador.allCategorias().forEach { model.addElement(it) }
        } catch (_: Throwable) { Jogador.allCategorias().forEach { model.addElement(it) } }

        val list = JList(model)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.background = TERTIARY
        val scroll = JScrollPane(list)
        scroll.border = BorderFactory.createEmptyBorder(8,8,8,8)

        val input = styledField()
        input.maximumSize = Dimension(Integer.MAX_VALUE, 30)
        val addBtn = styledButton("Adicionar"); val removeBtn = styledButton("Remover")
        addBtn.addActionListener {
            val t = input.text.trim()
            if (t.isNotEmpty() && !model.contains(t)) { model.addElement(t); input.text = "" }
        }
        removeBtn.addActionListener {
            val idx = list.selectedIndex
            if (idx >= 0) model.remove(idx)
        }

        val controls = JPanel(); controls.background = TERTIARY; controls.add(input); controls.add(addBtn); controls.add(removeBtn)

        val save = styledButton("Salvar")
        val cancel = styledButton("Cancelar")
        save.addActionListener {
            // persist list
            val cats = (0 until model.size()).map { model.get(it) }
            DB.XmlDatabase.saveGlobalCategorias(cats)
            JOptionPane.showMessageDialog(d, "Categorias salvas.")
            d.dispose()
        }
        cancel.addActionListener { d.dispose() }
        val foot = JPanel(); foot.background = TERTIARY; foot.add(save); foot.add(cancel)

        panel.add(scroll, BorderLayout.CENTER)
        panel.add(controls, BorderLayout.NORTH)
        d.add(panel, BorderLayout.CENTER)
        d.add(foot, BorderLayout.SOUTH)
        d.setLocationRelativeTo(null)
        d.isVisible = true
    }
}
