package ui.views

import models.Quality
import models.YoutubeItem
import utils.GBHelper
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.*

typealias VideoDownloadLocationChangeListener = (youtubeItem: YoutubeItem, location: File) -> Unit
typealias VideoSelectedQualityChangeListener = (youtubeItem: YoutubeItem, newQuality: Quality) -> Unit
typealias VideoNameChangeListener = (youtubeItem: YoutubeItem, newName: String) -> Unit

class JYoutubeList : JPanel() {

  private var onDownloadLocationChanged: VideoDownloadLocationChangeListener = { _, _ -> }
  private var onVideoQualityChanged: VideoSelectedQualityChangeListener = { _, _ -> }
  private var onVideoCancelClicked: (youtubeItem: YoutubeItem) -> Unit = {}
  private var onVideoRetryClicked: (youtubeItem: YoutubeItem) -> Unit = {}
  private var onVideoNameChanged: VideoNameChangeListener = { _, _ -> }

  private val items = ArrayList<VideoPanel>()

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

  fun addElement(item: YoutubeItem) {
    val videoPanel = VideoPanel(item, index = items.size)
    videoPanel.onVideoQualityChanged(onVideoQualityChanged)
    videoPanel.onDownloadLocationChange(onDownloadLocationChanged)
    videoPanel.onVideoCancelClicked(onVideoCancelClicked)
    videoPanel.onVideoRetryClicked(onVideoRetryClicked)

    items.add(videoPanel)

    mainPanel.remove(clearPanel)
    mainPanel.add(videoPanel, pos.nextRow().expandW().align(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.HORIZONTAL))
    mainPanel.add(clearPanel, pos.nextRow().expandH())

    scrollPane.validate()

    val vertical = scrollPane.verticalScrollBar
    vertical.value = vertical.maximum

  }

  fun updateSelectedQuality(index: Int? = null) {
    if (index == null) items.forEach { it.updateSelectedQuality() }
    else items[index].updateSelectedQuality()
  }

  fun updateDownloadLocation(index: Int? = null) {
    if (index == null) items.forEach { it.updateDownloadLocation() }
    else items[index].updateDownloadLocation()
  }

  fun updateItemState(index: Int) {
    items[index].updateVideoUIState()
  }

  fun onVideoCancelClicked(onVideoCancelClicked: (youtubeItem: YoutubeItem) -> Unit) {
    this.onVideoCancelClicked = onVideoCancelClicked
  }

  fun onVideoQualityChanged(videoSelectedQualityChangeListener: VideoSelectedQualityChangeListener) {
    this.onVideoQualityChanged = videoSelectedQualityChangeListener
  }

  fun onDownloadLocationChanged(videoDownloadLocationChangeListener: VideoDownloadLocationChangeListener) {
    this.onDownloadLocationChanged = videoDownloadLocationChangeListener
  }

  fun onVideoNameChanged(videoNameChangeListener: VideoNameChangeListener) {
    this.onVideoNameChanged = videoNameChangeListener
  }

  fun onVideoRetryClicked(onVideoRetryClicked: (youtubeItem: YoutubeItem) -> Unit) {
    this.onVideoRetryClicked = onVideoRetryClicked
  }

}