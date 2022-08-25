package services

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import models.NewPlaylistResponse
import models.NewUrlRequest
import models.UserPreferences
import models.VideoItem
import utils.Constants
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.File
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

object Downloader {

  private val publisher = PropertyChangeSupport(this)
  private val downloader = YoutubeDownloader()

  private val downloads = mutableListOf<DownloadItem>()
  private var lastIndex = 0

  fun parse(data: NewUrlRequest) {
    try {
      val isPlaylist = data.url.contains("playlist")
      val id = Regex(if (isPlaylist) Constants.PLAYLIST_ID_REGEX else Constants.VIDEO_ID_REGEX).find(data.url)!!.groupValues[2]
      if (!isPlaylist) parseVideo(id, data.downloadTo) else parsePlaylist(id, data.downloadTo)
    } catch (e: Exception) {
      // TODO: Fire Error to the view
      e.printStackTrace()
    }
  }

  fun startDownload(videos: List<VideoItem>) {
    firePropertyChange("DOWNLOADING", true)
    download(videos, lastIndex)
  }

  fun cancelVideoDownload(videoItem: VideoItem) {
    downloads.firstOrNull { it.videoItem == videoItem }?.cancel()
  }

  fun subscribe(listener: PropertyChangeListener) {
    publisher.addPropertyChangeListener(listener)
  }


  private fun parseVideo(videoId: String, downloadTo: File) {
    thread {
      firePropertyChange("NEW_VIDEO", true)
      val videoInfo = downloader.getVideoInfo(RequestVideoInfo(videoId)).data()
      val videoItem = VideoItem(videoInfo, downloadTo)
      firePropertyChange("VIDEO_ADDED", videoItem)
      firePropertyChange("NEW_VIDEO", false)
    }
  }

  private fun parsePlaylist(playlistId: String, downloadTo: File) {
    thread {
      val request = RequestPlaylistInfo(playlistId)
      val response = downloader.getPlaylistInfo(request)
      val playlistInfo = response.data()

      firePropertyChange("NEW_PLAYLIST", NewPlaylistResponse(playlistInfo.details().title(), true))

      for (video in playlistInfo.videos()) {
        val videoInfo = downloader.getVideoInfo(RequestVideoInfo(video.videoId())).data()
        val videoItem = VideoItem(videoInfo, downloadTo)
        firePropertyChange("VIDEO_ADDED", videoItem)
      }

      firePropertyChange("NEW_PLAYLIST", NewPlaylistResponse(playlistInfo.details().title(), false))
    }
  }

  private fun download(videos: List<VideoItem>, index: Int) {
    if (downloads.size == UserPreferences.MAX_PARALLEL_DOWNLOADS) {
      lastIndex--
      return
    }
    if (index >= videos.size) return

    val downloadItem = DownloadItem(downloader, videos[index])
      .onProgress {
        firePropertyChange("UPDATE_PROGRESS", index)
      }.onCanceled { downloadItem, canceled ->
        firePropertyChange("UPDATE_PROGRESS", index)
        if (canceled) {
          downloads.remove(downloadItem)
          download(videos, ++lastIndex)
        }
        if (downloads.isEmpty()) {
          lastIndex = 0
          firePropertyChange("DOWNLOADING", false)
        }
      }.onFinished { downloadItem, _ ->
        downloads.remove(downloadItem)
        download(videos, ++lastIndex)
        firePropertyChange("UPDATE_PROGRESS", index)
        if (downloads.isEmpty()) {
          lastIndex = 0
          firePropertyChange("DOWNLOADING", false)
        }
      }
    downloadItem.download()

    downloads.add(downloadItem)
    download(videos, ++lastIndex)
  }

  private fun firePropertyChange(name: String, newValue: Any?) {
    SwingUtilities.invokeLater {
      publisher.firePropertyChange(name, null, newValue)
    }
  }

}
