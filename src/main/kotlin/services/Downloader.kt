package services

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.model.videos.VideoInfo
import models.NewPlaylistData
import models.VideoItem
import utils.getSpeedText
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.File
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

class Downloader {
  companion object {
    private const val MAX_PARALLEL_DOWNLOADS = 3
  }

  private val publisher = PropertyChangeSupport(this)
  private val downloader = YoutubeDownloader()
  val videos = mutableListOf<VideoItem>()

  private var downloads = 0
  private var lastIndex = 0


  fun parse(id: String, isPlaylist: Boolean) {
    if (!isPlaylist) parseVideo(id) else parsePlaylist(id)
  }

  private fun parseVideo(videoId: String) {
    thread {
      val videoInfo = downloader.getVideoInfo(RequestVideoInfo(videoId)).data()
      videos.add(VideoItem(videoInfo = videoInfo))
      updateAvailableQualities(videoInfo)
      firePropertyChange("VIDEO_ADDED", null, videos.size - 1)
    }
  }

  private fun parsePlaylist(playlistId: String) {
    thread {
      val request = RequestPlaylistInfo(playlistId)
      val response = downloader.getPlaylistInfo(request)
      val playlistInfo = response.data()

      firePropertyChange("NEW_PLAYLIST", null, NewPlaylistData(playlistInfo.details().title(), true))

      for (video in playlistInfo.videos()) {
        val videoInfo = downloader.getVideoInfo(RequestVideoInfo(video.videoId())).data()
        videos.add(VideoItem(videoInfo = videoInfo))
        updateAvailableQualities(videoInfo)
        firePropertyChange("VIDEO_ADDED", null, videos.size - 1)
      }

      firePropertyChange("NEW_PLAYLIST", null, NewPlaylistData(playlistInfo.details().title(), false))
    }
  }

  private fun updateAvailableQualities(videoInfo: VideoInfo) {
    firePropertyChange("AVAILABLE_QUALITIES", null, videoInfo.videoWithAudioFormats().map { it.qualityLabel() })
  }


  fun updateQuality(quality: String) {
    for ((index, video) in videos.withIndex()) {
      video.selectedQuality = video.videoInfo.videoWithAudioFormats().find { it.qualityLabel() == quality }
        ?: video.videoInfo.bestVideoWithAudioFormat()
      firePropertyChange("QUALITY_UPDATED", -1, index)
    }
  }

  fun startDownload(folderPath: String) {
    download(folderPath, lastIndex)
  }

  private fun download(folderPath: String, index: Int) {
    if (downloads == MAX_PARALLEL_DOWNLOADS) {
      lastIndex--
      return
    }

    if (index >= videos.size) {
      return
    }

    val downloadRequest = RequestVideoFileDownload(videos[index].selectedQuality)
      .saveTo(File(folderPath))
      .renameTo(videos[index].title)
      .overwriteIfExists(true)
      .callback(object : YoutubeProgressCallback<File> {
        var startTime: Long = -1
        var oldSize: Long = 0

        override fun onFinished(data: File?) {
          downloads--
          download(folderPath, ++lastIndex)

          videos[index].speed = ""
          firePropertyChange("UPDATE_PROGRESS", null, index)
        }

        override fun onError(throwable: Throwable?) {
        }

        override fun onDownloading(progress: Int) {
          if (startTime == -1L) {
            startTime = System.nanoTime()
          } else if (System.nanoTime() - startTime >= 1000000000.0) {
            val downloadedSize = (progress / 100.0 * videos[index].size).toLong()
            val speed: String = getSpeedText(startTime, downloadedSize - oldSize)
            oldSize = downloadedSize
            startTime = System.nanoTime()
            videos[index].speed = speed
          }

          videos[index].progress = progress
          firePropertyChange("UPDATE_PROGRESS", null, index)
        }
      }).async()
    downloader.downloadVideoFile(downloadRequest)
    downloads++

    download(folderPath, ++lastIndex)
  }


  fun subscribe(listener: PropertyChangeListener) {
    publisher.addPropertyChangeListener(listener)
  }

  private fun firePropertyChange(name: String, oldValue: Any?, newValue: Any?) {
    SwingUtilities.invokeLater {
      publisher.firePropertyChange(name, oldValue, newValue)
    }
  }

}
