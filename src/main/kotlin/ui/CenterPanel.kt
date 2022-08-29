package ui

import models.YoutubeItem
import services.state.StateManager
import ui.views.JYoutubeList
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

class CenterPanel : JPanel() {

  private var onVideoCancelClicked: (youtubeItem: YoutubeItem) -> Unit = {}

  private val videosList = JYoutubeList()

  init {
    layout = BorderLayout()
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

    videosList.onVideoQualityChanged { youtubeVideo , quality -> StateManager.updateSelectedQuality(youtubeVideo , quality) }
    videosList.onDownloadLocationChanged { youtubeVideo , location -> StateManager.updateDownloadLocation(youtubeVideo , location) }
    videosList.onVideoNameChanged { youtubeVideo , name -> StateManager.updateVideoName(youtubeVideo , name) }
    videosList.onVideoCancelClicked { onVideoCancelClicked.invoke(it) }

    add(videosList)
  }

  fun addVideo(youtubeItem: YoutubeItem) {
    videosList.addElement(youtubeItem)
  }

  fun updateSelectedQuality() {
    videosList.updateSelectedQuality()
  }

  fun updateDownloadLocation() {
    videosList.updateDownloadLocation()
  }

  fun updateSelectedQuality(index: Int) {
    videosList.updateSelectedQuality(index)
  }

  fun updateDownloadLocation(index: Int) {
    videosList.updateDownloadLocation(index)
  }


  fun updateItemState(index: Int) {
    videosList.updateItemState(index)
  }


  fun onVideoCancelClicked(onVideoCancelClicked: (youtubeItem: YoutubeItem) -> Unit) {
    this.onVideoCancelClicked = onVideoCancelClicked
  }

}
