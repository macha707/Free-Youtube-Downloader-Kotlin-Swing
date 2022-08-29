package ui

import models.UserPreferences
import utils.GBHelper
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileFilter


class PreferencesDialog(owner: JFrame) : JDialog(owner, "User Preferences", true) {

  private val spinnerParallelDownloads = JSpinner(SpinnerNumberModel(UserPreferences.MAX_PARALLEL_DOWNLOADS, 1, 10, 1))
  private val fldFFmpegLocaiton = JTextField(UserPreferences.FFMPEG_PATH, 20)
  private val btnChooseFFmpegLocation = JButton("Choose")

  init {
    layout = BorderLayout()
    defaultCloseOperation = DISPOSE_ON_CLOSE

    val mainPanel = JPanel()
    mainPanel.layout = BorderLayout()
    mainPanel.border = BorderFactory.createCompoundBorder(
      BorderFactory.createEmptyBorder(5, 5, 5, 5),
      BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
    )

    mainPanel.add(makePreferencePane())
    mainPanel.add(makeButtonsPanel(), BorderLayout.SOUTH)

    add(mainPanel)
    pack()
    setLocationRelativeTo(owner)
  }


  private fun makePreferencePane(): JPanel {
    val preferencesPanel = JPanel()
    preferencesPanel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    preferencesPanel.layout = GridBagLayout()


    if (UserPreferences.isFFmpegInstalledInSystem()) {
      btnChooseFFmpegLocation.isEnabled = false
      fldFFmpegLocaiton.isEnabled = false
      fldFFmpegLocaiton.text = "FFMPEG Detected On The System"
      fldFFmpegLocaiton.disabledTextColor = Color.decode("#20CC82")
    }

    btnChooseFFmpegLocation.addActionListener {
      val chooser = JFileChooser()
      chooser.fileFilter = object : FileFilter() {
        override fun accept(file: File?): Boolean {
          file ?: return false
          return file.name.startsWith("ffmpeg") || file.isDirectory
        }
        override fun getDescription(): String {
          return "ffmpeg"
        }
      }
      val returnVal = chooser.showOpenDialog(parent)
      if (returnVal == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
        fldFFmpegLocaiton.text = chooser.selectedFile.absolutePath
      }
    }

    val pos = GBHelper()
    preferencesPanel.add(JLabel("Parallel Limit"), pos.padding(right = 5).align(GridBagConstraints.WEST))
    preferencesPanel.add(spinnerParallelDownloads, pos.nextCol().width(2).expandW())

    preferencesPanel.add(JLabel("FFMPEG Path"), pos.nextRow().padding(right = 5).align(GridBagConstraints.WEST))
    preferencesPanel.add(fldFFmpegLocaiton, pos.nextCol().expandW())
    preferencesPanel.add(btnChooseFFmpegLocation, pos.nextCol())

    preferencesPanel.add(JPanel(), pos.nextRow().expandH())

    return preferencesPanel
  }


  private fun makeButtonsPanel(): JPanel {
    val buttonsPanel = JPanel()
    buttonsPanel.border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
      BorderFactory.createEmptyBorder(5, 5, 5, 5)
    )

    val btnReset = JButton("Restore Default").apply {
      addActionListener {
        spinnerParallelDownloads.value = 3
        fldFFmpegLocaiton.text = ""
      }
    }

    val btnSave = JButton("Save").apply {
      addActionListener {
        UserPreferences.MAX_PARALLEL_DOWNLOADS = spinnerParallelDownloads.value as Int
        UserPreferences.FFMPEG_PATH = fldFFmpegLocaiton.text
        dispose()
      }
    }

    val btnCancel = JButton("Cancel").apply {
      addActionListener {
        dispose()
      }
    }

    buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.X_AXIS)
    buttonsPanel.add(btnReset)
    buttonsPanel.add(Box.createHorizontalGlue())
    buttonsPanel.add(btnCancel)
    buttonsPanel.add(btnSave)

    getRootPane().defaultButton = btnSave

    return buttonsPanel
  }
}