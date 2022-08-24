package ui.views

import models.Quality
import models.State
import models.VideoItem
import utils.GBHelper
import utils.chooseFolder
import utils.toReadableFileSize
import utils.loadUrl
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder

class VideoPanel(private val videoItem: VideoItem, val index: Int = 0) : JPanel() {

  val availableQualities get() = videoItem.availableQualities
  val downloadSize get() = videoItem.size

  private val tfVideoName = JTextField(videoItem.name, 15)
  private val cbVideoQuality = JComboBox(videoItem.availableQualities.map { it.readableName }.toTypedArray())
  private val tfVideoSize = JTextField(videoItem.size.toReadableFileSize()).apply {
    isEditable = false
    horizontalAlignment = JTextField.CENTER
  }

  private val lblLocation = JLabel("Location")
  private val tfDownloadLocation = JTextField(videoItem.downloadTo.absolutePath, 15).apply { isEditable = false }
  private val savePathBtn = JButton("Choose")

  init {
    layout = BorderLayout()
    border = EmptyBorder(0, 5, 0, 0)
    if (index >= 1)
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
        border
      )

    if (index % 2 != 0) background = background.darker()

    add(getThumbnailImage(), BorderLayout.WEST)
    add(getVideoOptionsPanel(), BorderLayout.CENTER)
  }

  fun updateVideoQuality(quality: Quality) {
    videoItem.downloadQuality = quality
    cbVideoQuality.selectedItem = quality.readableName
    tfVideoSize.text = videoItem.size.toReadableFileSize()
    firePropertyChange("VIDEO_QUALITY_CHANGED", null, videoItem.size)
  }

  fun updateDownloadLocation(downloadLocation: File) {
    JTextField().run {
      tfDownloadLocation.foreground = disabledTextColor
      tfDownloadLocation.font = font
    }

    videoItem.downloadTo = downloadLocation
    tfDownloadLocation.text = downloadLocation.absolutePath
  }

  fun updateVideoUIState() {
    lblLocation.text = "Status"
    tfDownloadLocation.text = videoItem.state.stateText
    tfDownloadLocation.font = tfDownloadLocation.font.deriveFont(Font.BOLD)
    tfDownloadLocation.foreground = when (videoItem.state) {
      is State.Downloading -> Color.decode("#3498db")
      State.Completed -> Color.decode("#20CC82")
      State.Canceling -> Color.decode("#e85445")
      State.Canceled -> Color.decode("#e74c3c")
      State.Merging -> Color.decode("#dbb434")
      State.Normal -> JTextField().disabledTextColor
    }

    when (videoItem.state) {
      is State.Downloading, State.Merging, State.Canceling -> {
        tfVideoName.isEnabled = false
        cbVideoQuality.isEnabled = false
        tfVideoSize.isEnabled = false
        savePathBtn.text = "Cancel"
      }

      State.Canceled, State.Completed, State.Normal -> {
        savePathBtn.text = "Choose"
      }

    }
  }

  fun getVideoItem(): VideoItem {
    return videoItem.apply { name = tfVideoName.text }
  }

  private fun getThumbnailImage(): JLabel {
    val imgThumbnail = JLabel()
    imgThumbnail.border = EmptyBorder(3, 3, 3, 3)
    imgThumbnail.icon = ImageIcon().run { loadUrl(videoItem.thumbnailUrl, 88); this }
    return imgThumbnail
  }

  private fun getVideoOptionsPanel(): JPanel {
    val videoOptionsPanel = JPanel()
    videoOptionsPanel.layout = GridBagLayout()
    videoOptionsPanel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    videoOptionsPanel.background = null


    val sharedPos = GBHelper()
    downloadOptionsPanel(videoOptionsPanel, sharedPos)
    downloadLocationPanel(videoOptionsPanel, sharedPos)

    return videoOptionsPanel
  }

  private fun downloadOptionsPanel(videoOptionsPanel: JPanel, pos: GBHelper) {

    cbVideoQuality.addActionListener {
      updateVideoQuality(Quality.of(cbVideoQuality.selectedItem?.toString().orEmpty()) ?: Quality.LOW)
    }

    videoOptionsPanel.add(JLabel("Name"), pos.padding(right = 5))
    videoOptionsPanel.add(tfVideoName, pos.nextCol().width(2).fill(GridBagConstraints.HORIZONTAL).expandW())

    videoOptionsPanel.add(JLabel("Quality"), pos.nextRow().padding(right = 5).align(GridBagConstraints.WEST))
    videoOptionsPanel.add(cbVideoQuality, pos.nextCol().expandW())
    videoOptionsPanel.add(tfVideoSize, pos.nextCol().width())

  }

  private fun downloadLocationPanel(videoOptionsPanel: JPanel, pos: GBHelper) {
    savePathBtn.addActionListener {
      if (savePathBtn.text == "Choose") updateDownloadLocation(chooseFolder(File(tfDownloadLocation.text)))
      else firePropertyChange("CANCEL_VIDEO", null, videoItem)
    }

    videoOptionsPanel.add(lblLocation, pos.nextRow().padding(right = 5).align(GridBagConstraints.WEST))
    videoOptionsPanel.add(tfDownloadLocation, pos.nextCol().expandW())
    videoOptionsPanel.add(savePathBtn, pos.nextCol().align(GridBagConstraints.CENTER))
  }

}