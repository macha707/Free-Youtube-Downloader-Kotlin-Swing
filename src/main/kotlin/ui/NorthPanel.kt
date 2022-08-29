package ui

import models.Quality
import models.UserPreferences
import services.state.StateManager
import utils.GBHelper
import utils.chooseFolder
import utils.placeholder
import utils.toReadableFileSize
import java.awt.Font
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import java.io.File
import javax.swing.*

class NorthPanel(frame: JFrame) : JPanel() {


  private var onAddClicked: (youtubeUrl: String) -> Unit = {}
  private var onDownloadClicked: () -> Unit = {}

  private val tfYoutubeUrl: JTextField = JTextField()
  private val btnAddUrl: JButton = JButton("Add")

  private val cbVideoQuality = JComboBox<Any>(arrayOf("No Qualities Available"))
  private val tfDownloadSize = JTextField(0L.toReadableFileSize()).apply {
    isEditable = false
    horizontalAlignment = JTextField.CENTER
  }

  private val tfDownloadLocation: JTextField = JTextField(UserPreferences.DOWNLOADS_FOLDER).apply { isEditable = false }
  private val btnChooseLocation = JButton("Choose")

  private val btnDownload: JButton = JButton("Download")

  init {
    layout = GridBagLayout()
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createEmptyBorder(5, 5, 0, 5), BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")), BorderFactory.createEmptyBorder(5, 5, 5, 5)
      )
    )
    tfYoutubeUrl.placeholder = "Enter youtube Video/Playlist url"

    btnAddUrl.addActionListener { onAddClicked.invoke(tfYoutubeUrl.text) }

    cbVideoQuality.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        if (cbVideoQuality.selectedIndex > -1)
          StateManager.updateSelectedQuality(cbVideoQuality.selectedItem as Quality)
      }
    }

    btnChooseLocation.addActionListener {
      val downloadLocation = chooseFolder(File(tfDownloadLocation.text))
      tfDownloadLocation.text = downloadLocation.absolutePath
      StateManager.updateDownloadLocation(downloadLocation)
    }

    btnDownload.apply {
      font = font.deriveFont(Font.BOLD)
      foreground = foreground.brighter()
      addActionListener { onDownloadClicked.invoke() }
    }


    val pos = GBHelper()
    add(JLabel("Youtube URL"), pos.padding(right = 5))
    add(tfYoutubeUrl, pos.nextCol().width(2).expandW())
    add(btnAddUrl, pos.nextCol())

    add(JLabel("Quality").apply { toolTipText = "Change quality for all videos" }, pos.nextRow())
    add(cbVideoQuality, pos.nextCol().width(2).expandW())
    add(tfDownloadSize, pos.nextCol().width())

    add(JLabel("Location").apply { toolTipText = "Change download location for all videos" }, pos.nextRow().padding(right = 5))
    add(tfDownloadLocation, pos.nextCol().expandW())
    add(btnChooseLocation, pos.nextCol())

    add(btnDownload, pos.nextCol().padding())

    disable(true)
    tfYoutubeUrl.isEnabled = true
    btnAddUrl.isEnabled = true

    frame.rootPane.defaultButton = btnDownload

  }

  fun disable(disable: Boolean) {
    tfYoutubeUrl.isEnabled = !disable
    btnAddUrl.isEnabled = !disable

    cbVideoQuality.isEnabled = !disable
    tfDownloadSize.isEnabled = !disable

    tfDownloadLocation.isEnabled = !disable
    btnChooseLocation.isEnabled = !disable
    btnDownload.isEnabled = !disable
  }

  fun updateQualities(qualities: List<Quality>) {
    cbVideoQuality.removeAllItems()
    qualities.forEach { cbVideoQuality.addItem(it) }
    cbVideoQuality.selectedIndex = 0
  }

  fun updateDownloadSize(downloadSize: Long) {
    tfDownloadSize.text = downloadSize.toReadableFileSize()
  }

  fun onAddClicked(onAddClicked: (youtubeUrl: String) -> Unit) {
    this.onAddClicked = onAddClicked
  }

  fun onDownloadClicked(onDownloadClicked: () -> Unit) {
    this.onDownloadClicked = onDownloadClicked
  }

}
