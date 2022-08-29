package services.state

import models.Quality
import models.State
import models.YoutubeItem
import java.io.File

typealias VideoAddedListener = (youtubeItem: YoutubeItem) -> Unit
typealias TotalDownloadSizeChangeListener = (downloadSize: Long) -> Unit
typealias AvailableQualitiesChangeListener = (qualities: List<Quality>) -> Unit
typealias SelectedQualityChangeListener = (newQuality: Quality) -> Unit
typealias DownloadLocationChangeListener = (newLocation: File) -> Unit
typealias VideoSelectedQualityChangeListener = (index: Int) -> Unit
typealias VideoDownloadLocationChangeListener = (index: Int) -> Unit
typealias VideoStateChangeListener = (index: Int) -> Unit

object StateManager {

  private val mVideos = mutableListOf<YoutubeItem>()
  val videos: List<YoutubeItem> get() = mVideos

  private val totalSize get() = mVideos.sumOf { it.size }
  private val worstQualities get() = mVideos.minWith(Comparator.comparingInt { it.availableQualities.size }).availableQualities

  fun addVideo(youtubeItem: YoutubeItem) {
    mVideos.add(youtubeItem)
    onVideoAdded.invoke(youtubeItem)
    onTotalDownloadSizeChange.invoke(totalSize)
    onAvailableQualitiesChange.invoke(worstQualities)
  }

  fun updateSelectedQuality(quality: Quality) {
    mVideos.forEach { it.downloadQuality = quality }
    onSelectedQualityChange.invoke(quality)
    onTotalDownloadSizeChange.invoke(totalSize)
  }

  fun updateDownloadLocation(location: File) {
    mVideos.forEach { it.downloadTo = location }
    onDownloadLocationChange.invoke(location)
  }

  fun updateSelectedQuality(youtubeItem: YoutubeItem, quality: Quality) {
    videos.firstOrNull { it == youtubeItem }?.let {
      it.downloadQuality = quality
      onVideoSelectedQualityChanged.invoke(videos.indexOf(it))
      onTotalDownloadSizeChange.invoke(totalSize)
    }
  }

  fun updateDownloadLocation(youtubeItem: YoutubeItem, location: File) {
    videos.firstOrNull { it == youtubeItem }?.let {
      it.downloadTo = location
      onVideoDownloadLocationChanged.invoke(videos.indexOf(it))
      onTotalDownloadSizeChange.invoke(totalSize)
    }
  }

  fun updateVideoName(youtubeItem: YoutubeItem, name: String) {
    videos.firstOrNull { it == youtubeItem }?.let { it.name = name }
  }

  fun updateVideoState(youtubeItem: YoutubeItem, state: State) {
    videos.firstOrNull { it == youtubeItem }?.let {
      it.state = state
      onVideoStateChanged.invoke(videos.indexOf(it))
    }
  }


  private var onVideoAdded: VideoAddedListener = {}
  private var onTotalDownloadSizeChange: TotalDownloadSizeChangeListener = {}
  private var onAvailableQualitiesChange: AvailableQualitiesChangeListener = {}
  private var onSelectedQualityChange: SelectedQualityChangeListener = {}
  private var onDownloadLocationChange: DownloadLocationChangeListener = {}
  private var onVideoSelectedQualityChanged: VideoSelectedQualityChangeListener = { }
  private var onVideoDownloadLocationChanged: VideoDownloadLocationChangeListener = { }
  private var onVideoStateChanged: VideoDownloadLocationChangeListener = { }

  fun onVideoAdded(videoAddedListener: VideoAddedListener) {
    this.onVideoAdded = videoAddedListener;
  }

  fun onTotalDownloadSizeChange(totalDownloadSizeChangeListener: TotalDownloadSizeChangeListener) {
    this.onTotalDownloadSizeChange = totalDownloadSizeChangeListener
  }

  fun onAvailableQualitiesChange(availableQualitiesChangeListener: AvailableQualitiesChangeListener) {
    this.onAvailableQualitiesChange = availableQualitiesChangeListener
  }

  fun onSelectedQualityChange(selectedQualityChangeListener: SelectedQualityChangeListener) {
    this.onSelectedQualityChange = selectedQualityChangeListener
  }

  fun onDownloadLocationChange(downloadLocationChangeListener: DownloadLocationChangeListener) {
    this.onDownloadLocationChange = downloadLocationChangeListener
  }

  fun onVideoQualityChange(videoSelectedQualityChangeListener: VideoSelectedQualityChangeListener) {
    this.onVideoSelectedQualityChanged = videoSelectedQualityChangeListener
  }

  fun onVideoDownloadLocationChange(videoDownloadLocationChangeListener: VideoDownloadLocationChangeListener) {
    this.onVideoDownloadLocationChanged = videoDownloadLocationChangeListener
  }

  fun onVideoStateChanged(videoStateChangeListener: VideoStateChangeListener) {
    this.onVideoStateChanged = videoStateChangeListener
  }

}