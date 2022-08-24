package ui

import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

class AboutDialog(owner: JFrame) : JDialog(owner, "About", true) {

  init {

    layout = BorderLayout()
    defaultCloseOperation = DISPOSE_ON_CLOSE

    val mainPanel = JPanel()
    mainPanel.layout = BorderLayout()
    mainPanel.border = BorderFactory.createCompoundBorder(
      BorderFactory.createEmptyBorder(5, 5, 5, 5),
      BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
    )

    val center = JPanel().apply {
      border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
      add(JLabel("Free Youtube Downloader").apply {
        font = font.deriveFont(18.0f).deriveFont(Font.BOLD)
      })
    }

    val bottom = JPanel().apply {
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
      )
      add(JButton("Close").apply {
        addActionListener { dispose() }
        alignmentX = CENTER_ALIGNMENT
        this@AboutDialog.rootPane.defaultButton = this
      })
    }

    mainPanel.add(center, BorderLayout.CENTER)
    mainPanel.add(bottom, BorderLayout.SOUTH)

    add(mainPanel)
    pack()
    setLocationRelativeTo(owner)
  }
}