package panels

import models.NewUrlData
import utils.Constants.PLAYLIST_ID_REGEX
import utils.Constants.VIDEO_ID_REGEX
import utils.GBHelper
import utils.placeholder
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField

class NorthPanel : JPanel() {

  private val tfSearch: JTextField = JTextField()
  private val btnSearch: JButton = JButton("Add")

  init {
    layout = GridBagLayout()
    border = BorderFactory.createEmptyBorder(5 , 5 , 0 , 5)

    // https://www.youtube.com/playlist?list=PLWz5rJ2EKKc8GZWCbUm3tBXKeqIi3rcVX
    tfSearch.placeholder = "https://www.youtube.com/playlist?list=example_abc123"

    btnSearch.addActionListener {
      val isPlaylist = tfSearch.text.contains("playlist")
      val id = Regex(if (isPlaylist) PLAYLIST_ID_REGEX else VIDEO_ID_REGEX).find(tfSearch.text)!!.groupValues[2]
      firePropertyChange("NEW_URL", "", NewUrlData(id, isPlaylist))
    }

    val pos = GBHelper()
    add(tfSearch, pos.expandW().padding(right = 5))
    add(btnSearch, pos.nextCol().padding())

  }

  fun disable(disable: Boolean) {
    tfSearch.isEnabled = !disable
    btnSearch.isEnabled = !disable
  }
}
