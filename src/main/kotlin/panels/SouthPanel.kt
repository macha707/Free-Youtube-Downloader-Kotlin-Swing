package panels

import utils.GBHelper
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import javax.swing.*

class SouthPanel : JPanel() {
  private val tfPath: JTextField = JTextField(System.getProperty("user.home") + "\\Downloads")
  private val sbQuality: JComboBox<String> = JComboBox(arrayOf("360p", "240p"))
  private val btnDownload: JButton = JButton("Download")

  init {
    layout = GridBagLayout()
    border = BorderFactory.createEmptyBorder(0 , 5 , 10 , 5)

    sbQuality.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        val item = it.item.toString()
        firePropertyChange("CHANGE_DOWNLOAD_QUALITY", "", item)
      }
    }
    btnDownload.addActionListener {
      firePropertyChange("START_DOWNLOAD", "", tfPath.text)
    }

    val pos = GBHelper()
    add(tfPath, pos.expandW().padding())
    add(sbQuality, pos.nextCol().padding(left = 5, right = 5))
    add(btnDownload, pos.nextCol().padding())

  }

  fun updateQualities(qualities: List<String>) {
    (sbQuality.model as DefaultComboBoxModel<String>).apply {
      removeAllElements()
      addAll(qualities)
    }
    sbQuality.selectedIndex = sbQuality.model.size - 1
  }

  fun disable(disable: Boolean) {
    tfPath.isEnabled = !disable
    sbQuality.isEnabled = !disable
    btnDownload.isEnabled = !disable
  }

  fun updateDownloadFolder(folder: String) {
    tfPath.text = System.getProperty("user.home") + "\\Downloads\\" + folder
  }
}
