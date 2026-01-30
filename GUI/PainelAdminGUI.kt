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

    // helper: try several locations for the image so the code works on different OSes
    private fun findImageFile(name: String): File? {
        try {
            // 1) project IMG folder (relative)
            val proj = File("IMG", name)
            if (proj.exists() && proj.isFile) return proj

            // 2) user home Desktop (English and Portuguese common names)
            val userHome = System.getProperty("user.home") ?: return null
            val candidates = listOf(
                File(userHome, "Desktop/$name"),
                File(userHome, "Área de trabalho/$name"),
                File(userHome, "Área de trabalho/App Corinthians/IMG/$name"),
                File(userHome, "App Corinthians/IMG/$name"),
                File(userHome, name)
            )
            for (c in candidates) if (c.exists() && c.isFile) return c

            // 3) absolute path fallback if environment variable present
            val envPath = System.getenv("APP_CORINTHIANS_IMG")
            if (!envPath.isNullOrBlank()) {
                val e = File(envPath)
                if (e.exists() && e.isFile) return e
            }
        } catch (_: Throwable) {}
        return null
    }

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
            // try relative and common locations via helper
            val preferred = findImageFile("Corinthians-Simbolo-Png.png")
            if (preferred != null) {
                val img = ImageIO.read(preferred) ?: return null
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
            val preferred = findImageFile("Corinthians-Simbolo-Png.png")
            if (preferred != null) {
                val img = ImageIO.read(preferred) ?: return null
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

    // Utility: walk component tree and ensure readable contrast
    private fun fixContrastRec(c: Component?) {
        if (c == null) return
        try {
            if (c is JComponent) {
                val bg = c.background
                val fg = c.foreground
                if (bg == TERTIARY && fg == TERTIARY) {
                    c.foreground = SECONDARY
                }
                // special-case text areas that may have dark background but white text already handled elsewhere
            }
            if (c is Container) {
                for (i in 0 until c.componentCount) {
                    fixContrastRec(c.getComponent(i))
                }
            }
        } catch (_: Throwable) {}
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
        val btnViewAll = styledButton("Ver todos os jogadores")
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
        // new: view all players of the team regardless of currently listed category
        btnViewAll.addActionListener {
            val sel = list.selectedValue
            if (sel == null) JOptionPane.showMessageDialog(d, "Selecione um time.") else showTimeDetailDialogForCategory(sel.teamName, "", d)
        }
        btnAdd.addActionListener {
            adicionarTimeDialog(d)
            // reload data
            originalTimes.clear(); originalTimes.addAll(AdicionarTime.getTimes()); rebuildModel(search.text, categoriaCombo.selectedItem as String)
        }
        btnClose.addActionListener { d.dispose() }

        val right = JPanel(); right.layout = BoxLayout(right, BoxLayout.Y_AXIS); right.background = SECONDARY
        right.border = BorderFactory.createEmptyBorder(12,12,12,12)
        right.add(btnView); right.add(Box.createRigidArea(Dimension(0,8))); right.add(btnViewAll); right.add(Box.createRigidArea(Dimension(0,10))); right.add(btnAdd); right.add(Box.createRigidArea(Dimension(0,10))); right.add(btnClose)

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
            // telefone edit field
            gbc.gridy = 4
            gbc.gridx = 0
            panel.add(styledLabel("Telefone:"), gbc)
            gbc.gridx = 1
            val telEdit = styledField()
            telEdit.text = sel.telefone
            panel.add(telEdit, gbc)
            val save = styledButton("Salvar"); val cancel = styledButton("Cancelar")
            save.addActionListener {
                val newName = nField.text.trim(); val newId = iField.text.trim().toIntOrNull(); val newPos = pField.text.trim(); val newCat = (categoriaCombo.selectedItem as? String) ?: ""
                val newTel = telEdit.text.trim()
                if (newName.isEmpty() || newId == null) { JOptionPane.showMessageDialog(edt, "Preencha nome e idade válidos."); return@addActionListener }
                // if telefone changed, check uniqueness
                if (newTel.isNotEmpty() && newTel != sel.telefone) {
                    val existing = AdicionarTime.getTeamByPhone(newTel)
                    if (existing != null) { JOptionPane.showMessageDialog(edt, "Este telefone já está registrado no time: $existing"); return@addActionListener }
                }
                sel.nome = newName
                sel.idade = newId
                sel.posicao = newPos
                sel.categoria = newCat
                sel.telefone = newTel
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

    // Dialog to create a competition (re-added to fix unresolved reference)
    private fun criarCompeticaoDialog() {
        val d = JDialog()
        d.title = "Criar Competição"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(760, 480))
        d.isResizable = true

        val main = JPanel(GridBagLayout())
        main.background = TERTIARY
        val gbcMain = GridBagConstraints(); gbcMain.insets = Insets(8,8,8,8); gbcMain.fill = GridBagConstraints.BOTH

        // form left
        val form = JPanel(GridBagLayout())
        form.background = TERTIARY
        val fgbc = GridBagConstraints(); fgbc.insets = Insets(6,6,6,6); fgbc.fill = GridBagConstraints.HORIZONTAL; fgbc.gridx=0; fgbc.gridy=0
        form.add(styledLabel("Nome:"), fgbc)
        fgbc.gridx=1; val nomeField = styledField(); form.add(nomeField, fgbc)
        fgbc.gridx=0; fgbc.gridy=1; form.add(styledLabel("Max perdas:"), fgbc)
        fgbc.gridx=1; val maxField = styledField(); maxField.text = "1"; form.add(maxField, fgbc)
        fgbc.gridx=0; fgbc.gridy=2; form.add(styledLabel("Categoria:"), fgbc)
        fgbc.gridx=1
        val categorias = try { DB.XmlDatabase.loadGlobalCategorias().ifEmpty { Jogador.allCategorias() } } catch (_: Throwable) { Jogador.allCategorias() }
        val catBox = JComboBox(categorias.toTypedArray())
        catBox.background = TERTIARY
        form.add(catBox, fgbc)

        // selected participants list (left)
        val selectedModel = DefaultListModel<String>()
        val selectedList = JList(selectedModel)
        selectedList.background = TERTIARY
        val selScroll = JScrollPane(selectedList)
        selScroll.preferredSize = Dimension(360, 240)
        selScroll.border = BorderFactory.createTitledBorder("Participantes selecionados")

        val leftWrap = JPanel(BorderLayout())
        leftWrap.background = TERTIARY
        leftWrap.add(form, BorderLayout.NORTH)
        leftWrap.add(selScroll, BorderLayout.CENTER)

        // right side: search + available
        val rightWrap = JPanel(GridBagLayout())
        rightWrap.background = TERTIARY
        val rgbc = GridBagConstraints(); rgbc.insets = Insets(6,6,6,6); rgbc.fill = GridBagConstraints.BOTH; rgbc.gridx=0; rgbc.gridy=0
        val searchField = styledField(); searchField.maximumSize = Dimension(Integer.MAX_VALUE, 30)
        val searchPanel = JPanel(); searchPanel.background = TERTIARY; searchPanel.layout = BoxLayout(searchPanel, BoxLayout.X_AXIS)
        val searchLabel = JLabel("Buscar time:")
        searchLabel.foreground = SECONDARY
        searchPanel.add(searchLabel); searchPanel.add(Box.createRigidArea(Dimension(6,0))); searchPanel.add(searchField)
        rightWrap.add(searchPanel, rgbc)

        rgbc.gridy = 1; rgbc.weightx = 1.0; rgbc.weighty = 1.0
        val availableModel = DefaultListModel<String>()
        val availableList = JList(availableModel)
        availableList.background = TERTIARY
        val availScroll = JScrollPane(availableList)
        availScroll.preferredSize = Dimension(360, 320)
        availScroll.border = BorderFactory.createTitledBorder("Times disponíveis")
        rightWrap.add(availScroll, rgbc)

        // middle buttons
        val btnPanel = JPanel(); btnPanel.background = TERTIARY; btnPanel.layout = BoxLayout(btnPanel, BoxLayout.Y_AXIS)
        val addBtn = styledButton("Adicionar →", PRIMARY); val removeBtn = styledButton("← Remover", PRIMARY)
        addBtn.maximumSize = Dimension(140,44); removeBtn.maximumSize = Dimension(140,44)
        btnPanel.add(Box.createVerticalGlue()); btnPanel.add(addBtn); btnPanel.add(Box.createRigidArea(Dimension(0,12))); btnPanel.add(removeBtn); btnPanel.add(Box.createVerticalGlue())

        gbcMain.gridx = 0; gbcMain.gridy = 0; gbcMain.weightx = 0.6; gbcMain.weighty = 1.0
        main.add(leftWrap, gbcMain)
        gbcMain.gridx = 1; gbcMain.weightx = 0.0; gbcMain.fill = GridBagConstraints.VERTICAL
        main.add(btnPanel, gbcMain)
        gbcMain.gridx = 2; gbcMain.weightx = 0.4; gbcMain.fill = GridBagConstraints.BOTH
        main.add(rightWrap, gbcMain)

        // populate available based on category and search
        fun atualizarDisponiveis() {
            availableModel.clear()
            val selCat = (catBox.selectedItem as? String) ?: ""
            val q = searchField.text.trim().lowercase()
            val times = AdicionarTime.getTimes()
            for (t in times) {
                if (selCat.isBlank()) continue
                if (!t.getJogadores().any { it.categoria == selCat }) continue
                if (q.isNotEmpty() && !t.nome.lowercase().contains(q)) continue
                if ((0 until selectedModel.size()).any { selectedModel.get(it) == t.nome }) continue
                availableModel.addElement(t.nome)
            }
        }

        catBox.addActionListener { atualizarDisponiveis() }
        searchField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = atualizarDisponiveis()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = atualizarDisponiveis()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = atualizarDisponiveis()
        })

        addBtn.addActionListener {
            val sel = availableList.selectedValuesList
            for (s in sel) {
                val name = s as String
                if (!selectedModel.contains(name)) selectedModel.addElement(name)
                availableModel.removeElement(name)
            }
        }
        removeBtn.addActionListener {
            val sel = selectedList.selectedValuesList
            for (s in sel) {
                val name = s as String
                selectedModel.removeElement(name)
                // re-add to available if matches
                val selCat = (catBox.selectedItem as? String) ?: ""
                val q = searchField.text.trim().lowercase()
                val t = AdicionarTime.getTimes().firstOrNull { it.nome == name }
                if (t != null && selCat.isNotBlank() && t.getJogadores().any { it.categoria == selCat } && (q.isEmpty() || name.lowercase().contains(q))) {
                    if (!availableModel.contains(name)) availableModel.addElement(name)
                }
            }
        }

        availableList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val sel = availableList.selectedValue
                    if (sel != null) { val n = sel as String; if (!selectedModel.contains(n)) selectedModel.addElement(n); availableModel.removeElement(n) }
                }
            }
        })
        selectedList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val sel = selectedList.selectedValue
                    if (sel != null) { val n = sel as String; selectedModel.removeElement(n); atualizarDisponiveis() }
                }
            }
        })

        atualizarDisponiveis()

        val save = styledButton("Criar"); val cancel = styledButton("Cancelar")
        save.addActionListener {
            val nome = nomeField.text.trim(); val maxP = maxField.text.trim().toIntOrNull()
            val cat = (catBox.selectedItem as? String) ?: ""
            if (nome.isEmpty() || maxP == null || cat.isEmpty()) { JOptionPane.showMessageDialog(d, "Preencha nome, max perdas e selecione categoria."); return@addActionListener }
            val c = Competicao(nome, maxP)
            c.addCategoria(cat)
            for (i in 0 until selectedModel.size()) {
                val tname = selectedModel.get(i)
                c.addParticipacao(tname, cat)
            }
            val ok = AdicionarTime.addCompeticaoObj(c)
            if (ok) {
                JOptionPane.showMessageDialog(d, "Competição criada: ${'$'}{c.nome}")
                d.dispose()
            } else {
                JOptionPane.showMessageDialog(d, "Erro: nome da competição inválido ou já existe outra competição com esse nome.")
            }
        }
        cancel.addActionListener { d.dispose() }

        val foot = JPanel(); foot.background = TERTIARY; foot.add(save); foot.add(cancel)
        d.add(main, BorderLayout.CENTER)
        d.add(foot, BorderLayout.SOUTH)
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

        // footer with actions (create UI elements here so listeners below can attach)
        val foot = JPanel(FlowLayout(FlowLayout.RIGHT))
        foot.background = TERTIARY
        val save = styledButton("Salvar")
        val cancel = styledButton("Cancelar", TERTIARY, SECONDARY)
        save.preferredSize = Dimension(120, 36)
        cancel.preferredSize = Dimension(120, 36)
        foot.add(cancel); foot.add(save)

        // wrap the form so it can be scrolled when the dialog is small or content grows
        val formWrap = JPanel(BorderLayout())
        formWrap.isOpaque = false
        formWrap.add(panel, BorderLayout.NORTH)
        // vertical scrollbar shown as needed
        val formScroll = JScrollPane(formWrap, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        formScroll.border = BorderFactory.createEmptyBorder()
        try { formScroll.verticalScrollBar.unitIncrement = 16 } catch (_: Throwable) {}

        // assemble into container and dialog
        panel.add(formScroll, BorderLayout.CENTER)
        panel.add(foot, BorderLayout.SOUTH)
        d.add(panel, BorderLayout.CENTER)
        // ensure dialog background matches inputs so black text is visible
        try { d.contentPane.background = TERTIARY } catch (_: Throwable) {}
        // keyboard: Enter triggers save
        d.rootPane.defaultButton = save
        d.setLocationRelativeTo(parent)
        d.isVisible = true

        // save/cancel listeners for time
        save.addActionListener {
            val nome = nomeField.text.trim(); val tecnico = tecnicoField.text.trim(); val av = avField.text.trim().replace(',', '.').toDoubleOrNull()
            if (nome.isEmpty() || tecnico.isEmpty() || av == null || av < 0.0 || av > 10.0) {
                JOptionPane.showMessageDialog(d, "Preencha os dados corretamente (av entre 0 e 10).")
                return@addActionListener
            }
            val t = Time(nome, tecnico, av)
            val ok = AdicionarTime.addTimeObj(t)
            if (ok) {
                JOptionPane.showMessageDialog(d, "Time adicionado: ${t.nome}")
                d.dispose()
            } else {
                JOptionPane.showMessageDialog(d, "Erro: nome inválido ou já existe outro time com esse nome.")
            }
        }
        cancel.addActionListener { d.dispose() }
    }

    private fun adicionarJogadorDialog(time: Time, parent: Window?) {
        val d = JDialog()
        d.title = "Adicionar Jogador - ${time.nome}"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(540, 360))
        d.isResizable = false

        // Outer container with padding and subtle border
        val container = JPanel(BorderLayout())
        container.border = BorderFactory.createEmptyBorder(12,12,12,12)
        container.background = TERTIARY

        // Header
        val hdr = JPanel(BorderLayout())
        hdr.background = TERTIARY
        val title = OutlinedLabel("Adicionar Jogador - ${time.nome}", Font("SansSerif", Font.BOLD, 20), PRIMARY, 2.5f)
        title.foreground = SECONDARY
        title.border = BorderFactory.createEmptyBorder(6,6,12,6)
        hdr.add(title, BorderLayout.CENTER)
        container.add(hdr, BorderLayout.NORTH)

        // Form panel
        val form = JPanel(GridBagLayout())
        form.background = TERTIARY
        val gbc = GridBagConstraints()
        gbc.insets = Insets(10,10,10,10)
        gbc.fill = GridBagConstraints.HORIZONTAL

        // helper to create labels with consistent style
        fun lbl(text: String): JLabel {
            val l = JLabel(text)
            l.font = Font("SansSerif", Font.BOLD, 14)
            l.foreground = SECONDARY
            return l
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25
        form.add(lbl("Nome:"), gbc)
        gbc.gridx = 1; gbc.weightx = 0.75
        val nomeField = JTextField(28)
        nomeField.font = Font("SansSerif", Font.PLAIN, 14)
        nomeField.background = TERTIARY
        nomeField.foreground = SECONDARY
        nomeField.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color(200,200,200)), BorderFactory.createEmptyBorder(6,8,6,8))
        form.add(nomeField, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.25
        form.add(lbl("Idade:"), gbc)
        gbc.gridx = 1; gbc.weightx = 0.75
        val idadeField = JTextField(6)
        idadeField.font = Font("SansSerif", Font.PLAIN, 14)
        idadeField.background = TERTIARY
        idadeField.foreground = SECONDARY
        idadeField.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color(200,200,200)), BorderFactory.createEmptyBorder(6,8,6,8))
        form.add(idadeField, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.25
        form.add(lbl("Posição:"), gbc)
        gbc.gridx = 1; gbc.weightx = 0.75
        val posField = JTextField(20)
        posField.font = Font("SansSerif", Font.PLAIN, 14)
        posField.background = TERTIARY
        posField.foreground = SECONDARY
        posField.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color(200,200,200)), BorderFactory.createEmptyBorder(6,8,6,8))
        form.add(posField, gbc)

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.25
        form.add(lbl("Categoria:"), gbc)
        gbc.gridx = 1; gbc.weightx = 0.75
        val categoriaCombo = JComboBox(Jogador.allCategorias().toTypedArray())
        categoriaCombo.background = TERTIARY
        categoriaCombo.foreground = SECONDARY
        categoriaCombo.border = BorderFactory.createLineBorder(Color(200,200,200))
        form.add(categoriaCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.25
        form.add(lbl("Telefone:"), gbc)
        gbc.gridx = 1; gbc.weightx = 0.75
        val telField = JTextField(18)
        telField.font = Font("SansSerif", Font.PLAIN, 14)
        telField.background = TERTIARY
        telField.foreground = SECONDARY
        telField.border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color(200,200,200)), BorderFactory.createEmptyBorder(6,8,6,8))
        telField.toolTipText = "Digite apenas números ou +55..."
        form.add(telField, gbc)

        // hint area
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.weightx = 1.0
        val hint = JLabel("Telefone deve ser único. Campos obrigatórios: Nome, Idade.")
        hint.font = Font("SansSerif", Font.ITALIC, 12)
        hint.foreground = Color(100,100,100)
        form.add(hint, gbc)

        // wrap the form so it can be scrolled when the dialog is small or content grows
        val formWrap = JPanel(BorderLayout())
        formWrap.isOpaque = false
        formWrap.add(form, BorderLayout.NORTH)
        // vertical scrollbar shown as needed
        val formScroll = JScrollPane(formWrap, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        formScroll.border = BorderFactory.createEmptyBorder()
        try { formScroll.verticalScrollBar.unitIncrement = 16 } catch (_: Throwable) {}

        // footer with actions for this dialog (Salvar/Cadastrar jogador)
        val foot = JPanel(FlowLayout(FlowLayout.RIGHT))
        foot.background = TERTIARY
        val save = styledButton("Salvar")
        val cancel = styledButton("Cancelar", TERTIARY, SECONDARY)
        save.preferredSize = Dimension(120, 36)
        cancel.preferredSize = Dimension(120, 36)
        foot.add(cancel); foot.add(save)

        // assemble into container and dialog
        container.add(formScroll, BorderLayout.CENTER)
        container.add(foot, BorderLayout.SOUTH)
        d.add(container, BorderLayout.CENTER)
        // ensure dialog background matches inputs so black text is visible
        try { d.contentPane.background = TERTIARY } catch (_: Throwable) {}
        // keyboard: Enter triggers save
        d.rootPane.defaultButton = save
        d.setLocationRelativeTo(parent)
        d.isVisible = true

        // save/cancel listeners for jogador
        save.addActionListener {
            val nome = nomeField.text.trim(); val idade = idadeField.text.trim().toIntOrNull(); val pos = posField.text.trim(); val cat = (categoriaCombo.selectedItem as? String) ?: ""
            val tel = telField.text.trim()
            if (nome.isEmpty() || idade == null || idade < 0) { JOptionPane.showMessageDialog(d, "Preencha nome e idade válidos."); return@addActionListener }
            if (tel.isNotEmpty()) {
                val existing = AdicionarTime.getTeamByPhone(tel)
                if (existing != null) { JOptionPane.showMessageDialog(d, "Este telefone já está registrado no time: $existing"); return@addActionListener }
            }
            val j = Jogador(nome, idade, pos, cat, tel)
            val ok = AdicionarTime.addPlayerToTime(time.nome, j)
            if (ok) {
                JOptionPane.showMessageDialog(d, "Jogador adicionado: ${j.nome}")
                d.dispose()
            } else {
                JOptionPane.showMessageDialog(d, "Não foi possível adicionar jogador: telefone duplicado ou erro.")
            }
        }
        cancel.addActionListener { d.dispose() }
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

    // Simple Sorteios dialog: list competitions and allow open/delete
    private fun sorteiosGui() {
        val comps = AdicionarTime.getCompeticoes().toMutableList()
        val d = JDialog()
        d.title = "Sorteios"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(640, 420))
        d.isResizable = true

        val model = DefaultListModel<Competicao>()
        comps.forEach { model.addElement(it) }
        val list = JList(model)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = ListCellRenderer { _, value, index, isSelected, _ ->
            val p = JPanel(BorderLayout())
            p.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            val title = JLabel("${value.nome}  •  Max perdas: ${value.maxPerdas}")
            title.font = Font("SansSerif", Font.BOLD, 14)
            title.foreground = if (isSelected) TERTIARY else SECONDARY
            val meta = JLabel("Categorias: ${value.categorias.joinToString(", ")}")
            meta.font = Font("SansSerif", Font.PLAIN, 12)
            meta.foreground = Color(110,110,110)
            val left = JPanel(); left.layout = BoxLayout(left, BoxLayout.Y_AXIS); left.isOpaque = false
            left.add(title); left.add(Box.createRigidArea(Dimension(0,6))); left.add(meta)
            p.add(left, BorderLayout.CENTER)
            p.background = if (isSelected) PRIMARY else if (index % 2 == 0) Color(245,245,245) else TERTIARY
            p
        }
        val scroll = JScrollPane(list)
        scroll.border = BorderFactory.createLineBorder(Color(200,200,200))

        val openBtn = styledButton("Abrir")
        val trackBtn = styledButton("Acompanhar")
        val delBtn = styledButton("Excluir")
        val closeBtn = styledButton("Fechar")

        openBtn.addActionListener {
            val sel = list.selectedValue
            if (sel == null) {
                JOptionPane.showMessageDialog(d, "Selecione uma competição.")
            } else {
                d.dispose()
                openCompetitionDialog(sel, null)
            }
        }
        trackBtn.addActionListener {
            val sel = list.selectedValue
            if (sel == null) { JOptionPane.showMessageDialog(d, "Selecione uma competição."); return@addActionListener }
            d.dispose()
            openCompetitionTracker(sel, null)
        }
        delBtn.addActionListener {
            val sel = list.selectedValue
            if (sel == null) { JOptionPane.showMessageDialog(d, "Selecione uma competição."); return@addActionListener }
            val ok = JOptionPane.showConfirmDialog(d, "Excluir ${sel.nome}?", "Confirmar", JOptionPane.YES_NO_OPTION)
            if (ok == JOptionPane.YES_OPTION) {
                AdicionarTime.removeCompeticaoByName(sel.nome)
                model.removeElement(sel)
                JOptionPane.showMessageDialog(d, "Competição excluída.")
            }
        }
        closeBtn.addActionListener { d.dispose() }

        val btns = JPanel(FlowLayout(FlowLayout.RIGHT))
        btns.background = SECONDARY
        btns.add(openBtn); btns.add(trackBtn); btns.add(delBtn); btns.add(closeBtn)

        d.add(scroll, BorderLayout.CENTER)
        d.add(btns, BorderLayout.SOUTH)
        d.setLocationRelativeTo(null)
        d.isVisible = true
    }

    // Opens a detailed dialog for a competition (simple viewer for now)
    private fun openCompetitionDialog(c: Competicao, parent: Window?) {
        val d = JDialog()
        d.title = "Competição: ${c.nome}"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(680, 460))
        d.isResizable = true

        val header = JPanel(BorderLayout())
        header.background = TERTIARY
        val title = styledLabel(c.nome, 18)
        title.border = BorderFactory.createEmptyBorder(12,12,8,12)
        header.add(title, BorderLayout.NORTH)
        val meta = JLabel("Max perdas: ${c.maxPerdas}   •   Categorias: ${if (c.categorias.isEmpty()) "(nenhuma)" else c.categorias.joinToString(", ")}")
        meta.font = Font("SansSerif", Font.PLAIN, 13)
        meta.foreground = Color(100,100,100)
        meta.border = BorderFactory.createEmptyBorder(0,12,12,12)
        header.add(meta, BorderLayout.SOUTH)

        val model = DefaultListModel<String>()
        for (p in c.participacoes) model.addElement("${p.timeNome} ${if (p.categoria.isNotBlank()) "(cat: ${p.categoria})" else ""}")
        val list = JList(model)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.background = TERTIARY
        list.cellRenderer = ListCellRenderer { _, value, index, isSelected, _ ->
            val p = JPanel(BorderLayout())
            p.border = BorderFactory.createEmptyBorder(6,8,6,8)
            val lbl = JLabel(value)
            lbl.font = Font("SansSerif", Font.PLAIN, 13)
            lbl.foreground = SECONDARY
            p.add(lbl, BorderLayout.CENTER)
            p.background = if (isSelected) PRIMARY else if (index % 2 == 0) Color(245,245,245) else TERTIARY
            p
        }
        val scroll = JScrollPane(list)
        scroll.border = BorderFactory.createLineBorder(Color(200,200,200))

        val close = styledButton("Fechar")
        close.addActionListener { d.dispose() }
        val acompanharBtn = styledButton("Acompanhar")
        acompanharBtn.addActionListener { d.dispose(); openCompetitionTracker(c, parent) }
        val foot = JPanel(); foot.background = SECONDARY; foot.add(acompanharBtn); foot.add(close)

        d.add(header, BorderLayout.NORTH)
        d.add(scroll, BorderLayout.CENTER)
        d.add(foot, BorderLayout.SOUTH)
        d.setLocationRelativeTo(parent)
        d.isVisible = true
    }

    // Tracker dialog: generate rounds, show pending matches, mark winners
    private fun openCompetitionTracker(c: Competicao, parent: Window?) {
        val d = JDialog()
        d.title = "Acompanhar: ${c.nome}"
        d.isModal = true
        d.layout = BorderLayout()
        d.setSize(Dimension(820, 520))
        d.isResizable = true

        val header = JPanel(BorderLayout())
        header.background = TERTIARY
        header.add(styledLabel("Acompanhar: ${c.nome}", 18), BorderLayout.NORTH)
        val meta = JLabel("Max perdas: ${c.maxPerdas}   •   Categorias: ${c.categorias.joinToString(", ")}")
        meta.font = Font("SansSerif", Font.PLAIN, 13)
        meta.foreground = Color(90,90,90)
        meta.border = BorderFactory.createEmptyBorder(0,12,12,12)
        header.add(meta, BorderLayout.SOUTH)

        // left: pending matches
        val pendingModel = DefaultListModel<String>()
        fun refreshPending() {
            pendingModel.clear()
            for (m in c.pendingMatches()) pendingModel.addElement("")
        }
        val pendingList = JList(pendingModel)
        pendingList.cellRenderer = ListCellRenderer { _, value, index, isSelected, _ ->
            val p = JPanel(BorderLayout())
            p.border = BorderFactory.createEmptyBorder(8,8,8,8)
            val lbl = JLabel(value)
            lbl.font = Font("SansSerif", Font.PLAIN, 13)
            lbl.foreground = SECONDARY
            p.add(lbl, BorderLayout.CENTER)
            p.background = if (isSelected) PRIMARY else if (index % 2 == 0) Color(250,250,250) else TERTIARY
            p
        }
        val pendingScroll = JScrollPane(pendingList)
        pendingScroll.border = BorderFactory.createTitledBorder("Partidas pendentes")

        // functions to map matches to display strings
        fun matchToString(m: com.corinthians.app.Competicao.Match): String {
            return "${m.a}  vs  ${m.b}  ${if (m.winner != null) "• vencedor: ${m.winner}" else "• pendente"}"
        }

        fun reloadPendingNoState() {
            pendingModel.clear()
            for (m in c.pendingMatches()) pendingModel.addElement(matchToString(m))
        }
        // remove earlier reloadPending() call here; will call after drawBtn declared

        // center: action buttons
        val center = JPanel(); center.layout = BoxLayout(center, BoxLayout.Y_AXIS); center.background = TERTIARY
        val drawBtn = styledButton("Sortear próximos")
        val markWinnerBtn = styledButton("Marcar vencedor")
        val resetBtn = styledButton("Resetar histórico")
        // initial state: only allow drawing when there are no pending matches
        drawBtn.isEnabled = c.pendingMatches().isEmpty()
        center.add(Box.createVerticalGlue()); center.add(drawBtn); center.add(Box.createRigidArea(Dimension(0,10))); center.add(markWinnerBtn); center.add(Box.createRigidArea(Dimension(0,10))); center.add(resetBtn); center.add(Box.createVerticalGlue())

        // now safe to call reloadPending() and set drawBtn state
        fun reloadPending() {
            reloadPendingNoState()
            drawBtn.isEnabled = c.pendingMatches().isEmpty()
        }
        reloadPending()

        // right: history / standings
        val historyModel = DefaultListModel<String>()
        val historyList = JList(historyModel)
        historyList.cellRenderer = ListCellRenderer { _, value, index, isSelected, _ ->
            val p = JPanel(BorderLayout())
            p.border = BorderFactory.createEmptyBorder(6,8,6,8)
            val lbl = JLabel(value)
            lbl.font = Font("SansSerif", Font.PLAIN, 13)
            lbl.foreground = SECONDARY
            p.add(lbl, BorderLayout.CENTER)
            p.background = if (isSelected) PRIMARY else if (index % 2 == 0) Color(245,245,245) else TERTIARY
            p
        }
        val historyScroll = JScrollPane(historyList)
        historyScroll.border = BorderFactory.createTitledBorder("Histórico de partidas")

        fun reloadHistoryAndStandings() {
            historyModel.clear()
            // show full history: completed first (winner != null), then pending
            val hist = c.getHistory()
            // completed matches
            for (m in hist.filter { it.winner != null }) historyModel.addElement(matchToString(m))
            // pending matches (to make them visible as historical pendings)
            for (m in hist.filter { it.winner == null }) historyModel.addElement(matchToString(m))
        }
        reloadHistoryAndStandings()

        // actions
        drawBtn.addActionListener {
            val next = c.generateNextRound()
            if (next.isEmpty()) JOptionPane.showMessageDialog(d, "Nenhuma partida disponível para sortear (talvez número ímpar por perdas, ou ninguém ativo).")
            reloadPending()
            reloadHistoryAndStandings()
            // after drawing there will be pending matches -> disable drawing until they are resolved
            drawBtn.isEnabled = c.pendingMatches().isEmpty()
            AdicionarTime.updateCompeticao(c)
        }

        markWinnerBtn.addActionListener {
            val selIdx = pendingList.selectedIndex
            if (selIdx < 0) { JOptionPane.showMessageDialog(d, "Selecione uma partida pendente para marcar vencedor."); return@addActionListener }
            val match = c.pendingMatches()[selIdx]
            // prefer a two-button dialog to avoid typing errors
            val options = arrayOf(match.a, match.b)
            val chosen = JOptionPane.showOptionDialog(d, "Marcar vencedor:", "Marcar vencedor", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0])
            if (chosen == JOptionPane.CLOSED_OPTION) return@addActionListener
            val winner = options.getOrNull(chosen) ?: return@addActionListener
            c.recordResult(winner)
            // persist and refresh
            AdicionarTime.updateCompeticao(c)
            reloadPending(); reloadHistoryAndStandings()
            // after marking, if no more pending matches, enable draw
            drawBtn.isEnabled = c.pendingMatches().isEmpty()
        }

        resetBtn.addActionListener {
            val ok = JOptionPane.showConfirmDialog(d, "Limpar histórico de partidas desta competição? (não afeta perdas já registradas)", "Confirmar", JOptionPane.YES_NO_OPTION)
            if (ok == JOptionPane.YES_OPTION) { c.clearHistory(); reloadPending(); reloadHistoryAndStandings(); AdicionarTime.updateCompeticao(c) }
        }


        val main = JPanel(GridBagLayout())
        val gbc = GridBagConstraints(); gbc.insets = Insets(8,8,8,8); gbc.fill = GridBagConstraints.BOTH
        gbc.gridx=0; gbc.gridy=0; gbc.weightx = 0.45; gbc.weighty = 1.0; main.add(pendingScroll, gbc)
        gbc.gridx=1; gbc.weightx = 0.1; main.add(center, gbc)
        gbc.gridx=2; gbc.weightx = 0.45; main.add(historyScroll, gbc)

        val foot = JPanel(); foot.background = TERTIARY; val closeBtn = styledButton("Fechar"); closeBtn.addActionListener { d.dispose() }; foot.add(closeBtn)

        d.add(header, BorderLayout.NORTH)
        d.add(main, BorderLayout.CENTER)
        d.add(foot, BorderLayout.SOUTH)
        d.setLocationRelativeTo(parent)
        d.isVisible = true
    }

}
