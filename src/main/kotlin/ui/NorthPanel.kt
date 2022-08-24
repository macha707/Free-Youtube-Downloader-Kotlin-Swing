package ui

import models.NewUrlRequest
import models.Quality
import models.UserPreferences
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

  private val tfYoutubeUrl: JTextField = JTextField()
  private val btnAddUrl: JButton = JButton("Add")

  private val cbVideoQuality = JComboBox(arrayOf("No Qualities Available"))
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
      BorderFactory.createEmptyBorder(5, 5, 0, 5),
      BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
      )
    )
    tfYoutubeUrl.placeholder = "Enter youtube Video/Playlist url"

    btnAddUrl.addActionListener {
      firePropertyChange("NEW_URL", "", NewUrlRequest(tfYoutubeUrl.text, File(tfDownloadLocation.text)))
    }

    cbVideoQuality.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        if (cbVideoQuality.selectedIndex > -1)
          firePropertyChange(
            "CHANGE_DOWNLOAD_QUALITY",
            "",
            Quality.of(cbVideoQuality.selectedItem?.toString().orEmpty()) ?: Quality.LOW
          )
      }
    }

    btnChooseLocation.addActionListener {
      val downloadLocation = chooseFolder(File(tfDownloadLocation.text))
      tfDownloadLocation.text = downloadLocation.absolutePath
      firePropertyChange("CHANGE_DOWNLOAD_LOCATION", null, downloadLocation)
    }

    btnDownload.apply {
      font = font.deriveFont(Font.BOLD)
      foreground = foreground.brighter()
      addActionListener {
        firePropertyChange("START_DOWNLOAD", "", tfDownloadLocation.text)
      }
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
    if (qualities.size == cbVideoQuality.itemCount) return

    cbVideoQuality.removeAllItems()
    qualities.forEach { cbVideoQuality.addItem(it.readableName) }
    cbVideoQuality.selectedIndex = 0

  }

  fun updateDownloadSize(downloadSize: Long) {
    tfDownloadSize.text = downloadSize.toReadableFileSize()
  }


}
