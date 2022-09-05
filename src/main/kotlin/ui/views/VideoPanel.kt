package ui.views

import models.Quality
import models.State
import models.YoutubeItem
import utils.*
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder

class VideoPanel(private val youtubeItem: YoutubeItem, val index: Int = 0) : JPanel() {

  private var onDownloadLocationChange: VideoDownloadLocationChangeListener = { _, _ -> }
  private var onVideoQualityChanged: VideoSelectedQualityChangeListener = { _, _ -> }
  private var onVideoNameChanged: VideoNameChangeListener = { _, _ -> }
  private var onVideoCancelClicked: (youtubeItem: YoutubeItem) -> Unit = {}
  private var onVideoRetryClicked: (youtubeItem: YoutubeItem) -> Unit = {}

  private val tfVideoName = JTextField(youtubeItem.name, 15)
  private val cbVideoQuality = JComboBox(youtubeItem.availableQualities.toTypedArray())
  private val tfVideoSize = JTextField(youtubeItem.size.toReadableFileSize())

  private val lblLocation = JLabel("Location")
  private val tfDownloadLocation = JTextField(youtubeItem.downloadTo.absolutePath, 15).apply { isEditable = false }
  private val savePathBtn = JButton("Choose")

  init {
    layout = BorderLayout()
    border = EmptyBorder(0, 5, 0, 0)
    if (index >= 1)
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
        border
      )

    add(getThumbnailImage(), BorderLayout.WEST)
    add(getVideoOptionsPanel(), BorderLayout.CENTER)
  }

  fun updateSelectedQuality() {
    cbVideoQuality.selectedItem = youtubeItem.downloadQuality
    tfVideoSize.text = youtubeItem.size.toReadableFileSize()
  }

  fun updateDownloadLocation() {
    JTextField().run {
      tfDownloadLocation.foreground = disabledTextColor
      tfDownloadLocation.font = font
    }
    tfDownloadLocation.text = youtubeItem.downloadTo.absolutePath
  }

  fun updateVideoUIState() {
    lblLocation.text = "Status"
    tfDownloadLocation.text = youtubeItem.state.stateText
    tfDownloadLocation.font = tfDownloadLocation.font.deriveFont(Font.BOLD)
    tfDownloadLocation.foreground = when (youtubeItem.state) {
      is State.Downloading -> Color.decode("#3498db")
      State.Completed -> Color.decode("#20CC82")
      State.Canceling -> Color.decode("#e85445")
      State.Canceled, is State.Error -> Color.decode("#e74c3c")
      is State.CustomState -> Color.decode("#dbb434")
      State.Normal -> JTextField().disabledTextColor
    }

    when (youtubeItem.state) {
      is State.Downloading, is State.CustomState, State.Canceling -> {
        tfVideoName.isEnabled = false
        cbVideoQuality.isEnabled = false
        tfVideoSize.isEnabled = false
        savePathBtn.text = "Cancel"
      }

      State.Completed, State.Normal -> {
        tfVideoName.isEnabled = true
        cbVideoQuality.isEnabled = true
        tfVideoSize.isEnabled = true
        savePathBtn.text = "Choose"
      }

      is State.Error , State.Canceled -> {
        tfVideoName.isEnabled = true
        cbVideoQuality.isEnabled = true
        tfVideoSize.isEnabled = true
        savePathBtn.text = "Retry"
      }

    }
  }

  private fun getThumbnailImage(): JLabel {
    val imgThumbnail = JLabel()
    imgThumbnail.border = EmptyBorder(3, 3, 3, 3)
    imgThumbnail.icon = ImageIcon().run { loadUrl(youtubeItem.thumbnailUrl, 88); this }
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

    tfVideoName.addTextChangeListener { onVideoNameChanged.invoke(youtubeItem, tfVideoName.text) }

    cbVideoQuality.addActionListener {
      val quality = cbVideoQuality.selectedItem as Quality
      onVideoQualityChanged.invoke(youtubeItem, quality)
    }

    tfVideoSize.isEditable = false
    tfVideoSize.horizontalAlignment = JTextField.CENTER

    videoOptionsPanel.add(JLabel("Name"), pos.padding(right = 5))
    videoOptionsPanel.add(tfVideoName, pos.nextCol().width(2).fill(GridBagConstraints.HORIZONTAL).expandW())

    videoOptionsPanel.add(JLabel("Quality"), pos.nextRow().padding(right = 5).align(GridBagConstraints.WEST))
    videoOptionsPanel.add(cbVideoQuality, pos.nextCol().expandW())
    videoOptionsPanel.add(tfVideoSize, pos.nextCol().width())

  }

  private fun downloadLocationPanel(videoOptionsPanel: JPanel, pos: GBHelper) {
    savePathBtn.addActionListener {
      when (savePathBtn.text) {
        "Choose" -> {
          val location = chooseFolder(File(tfDownloadLocation.text))
          onDownloadLocationChange.invoke(youtubeItem, location)
        }

        "Cancel" -> {
          onVideoCancelClicked.invoke(youtubeItem)
        }

        "Retry" -> {
          onVideoRetryClicked.invoke(youtubeItem)
        }
      }
    }

    videoOptionsPanel.add(lblLocation, pos.nextRow().padding(right = 5).align(GridBagConstraints.WEST))
    videoOptionsPanel.add(tfDownloadLocation, pos.nextCol().expandW())
    videoOptionsPanel.add(savePathBtn, pos.nextCol().align(GridBagConstraints.CENTER))
  }

  fun onVideoCancelClicked(onVideoCancelClicked: (youtubeItem: YoutubeItem) -> Unit) {
    this.onVideoCancelClicked = onVideoCancelClicked
  }

  fun onVideoQualityChanged(videoSelectedQualityChangeListener: VideoSelectedQualityChangeListener) {
    this.onVideoQualityChanged = videoSelectedQualityChangeListener
  }

  fun onDownloadLocationChange(videoDownloadLocationChangeListener: VideoDownloadLocationChangeListener) {
    this.onDownloadLocationChange = videoDownloadLocationChangeListener
  }

  fun onVideoNameChanged(videoNameChangeListener: VideoNameChangeListener) {
    this.onVideoNameChanged = videoNameChangeListener
  }

  fun onVideoRetryClicked(onVideoRetryClicked: (youtubeItem: YoutubeItem) -> Unit) {
    this.onVideoRetryClicked = onVideoRetryClicked
  }

}