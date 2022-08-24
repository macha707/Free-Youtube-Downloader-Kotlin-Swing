package ui

import models.Quality
import models.VideoItem
import ui.views.JYoutubeList
import java.awt.BorderLayout
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JPanel

class CenterPanel : JPanel() {

  private val videosList = JYoutubeList()

  val videos: List<VideoItem> get() = videosList.videos

    init {
    layout = BorderLayout()
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

    videosList.addPropertyChangeListener {
      if (it.propertyName == "VIDEO_QUALITY_CHANGED") {
        firePropertyChange("UPDATE_TOTAL_DOWNLOAD_SIZE" , "" , videosList.totalSize())
      } else if (it.propertyName == "CANCEL_VIDEO") {
        firePropertyChange("CANCEL_VIDEO" , "" , it.newValue)
      }
    }

    add(videosList)
  }

  fun addVideo(videoItem: VideoItem) {
    videosList.addElement(videoItem)
    firePropertyChange("UPDATE_TOTAL_DOWNLOAD_SIZE" , "" , videosList.totalSize())
    firePropertyChange("UPDATE_AVAILABLE_QUALITIES" , "" , videosList.worstQualities())
  }

  fun updateSelectedQuality(quality: Quality) {
    videosList.updateSelectedQuality(quality)
  }

  fun updateDownloadLocation(downloadLocation: File) {
    videosList.updateDownloadLocation(downloadLocation)
  }

  fun updateItemState(index: Int) {
    videosList.updateItemState(index)
  }

}
