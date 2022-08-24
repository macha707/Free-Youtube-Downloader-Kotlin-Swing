package ui.views

import models.Quality
import models.VideoItem
import utils.GBHelper
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.*


class JYoutubeList : JPanel() {
  private val items = ArrayList<VideoPanel>()
  val videos: List<VideoItem> get() = items.map { it.getVideoItem() }

  private val mainPanel = JPanel()
  private val clearPanel = JPanel()
  private val scrollPane = JScrollPane(mainPanel)

  private val pos = GBHelper()

  init {
    layout = BorderLayout()
    mainPanel.layout = GridBagLayout()

    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    scrollPane.verticalScrollBar.unitIncrement = 16

    scrollPane.border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
    mainPanel.border = null

    add(scrollPane, BorderLayout.CENTER)

  }

  fun addElement(item: VideoItem) {
    val videoPanel = VideoPanel(item, index = items.size)
    videoPanel.addPropertyChangeListener {
      if (it.propertyName == "VIDEO_QUALITY_CHANGED") {
        firePropertyChange("VIDEO_QUALITY_CHANGED", null, it.newValue)
      } else if (it.propertyName == "CANCEL_VIDEO") {
        firePropertyChange("CANCEL_VIDEO", null, it.newValue)
      }
    }
    items.add(videoPanel)

    mainPanel.remove(clearPanel)
    mainPanel.add(
      videoPanel,
      pos.nextRow().expandW().align(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.HORIZONTAL)
    )
    mainPanel.add(clearPanel, pos.nextRow().expandH())

    scrollPane.validate()

    val vertical = scrollPane.verticalScrollBar
    vertical.value = vertical.maximum

  }

  fun updateSelectedQuality(quality: Quality) {
    items.forEach { it.updateVideoQuality(quality) }
  }

  fun updateDownloadLocation(downloadLocation: File) {
    items.forEach { it.updateDownloadLocation(downloadLocation) }
  }

  fun totalSize(): Long {
    return items.sumOf { it.downloadSize }
  }

  fun worstQualities(): List<Quality> {
    return items.minWith(Comparator.comparingInt { it.availableQualities.size }).availableQualities
  }

  fun updateItemState(index: Int) {
    items[index].updateVideoUIState()
  }

}