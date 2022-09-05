package services.downloader

import com.github.kiulian.downloader.YoutubeDownloader
import models.State
import models.UserPreferences
import models.YoutubeItem
import services.state.StateManager

typealias DownloadListener = (isDownloading: Boolean) -> Unit
typealias ProgressUpdatedListener = (videoItemIndex: Int) -> Unit

interface IYoutubeDownloader {
  fun startDownload(videos: List<YoutubeItem>)
  fun cancelDownload(youtubeItem: YoutubeItem)
  fun retryDownload(youtubeItem: YoutubeItem)
}

abstract class AbstractYoutubeDownloader : IYoutubeDownloader {
  protected var onDownloading: DownloadListener = {}
  protected var onProgressUpdated: ProgressUpdatedListener = {}

  fun onProgressUpdated(onProgressUpdated: ProgressUpdatedListener) {
    this.onProgressUpdated = onProgressUpdated
  }
  fun onDownloading(onDownloading: DownloadListener) {
    this.onDownloading = onDownloading
  }
}

class DefaultYoutubeDownloader(private val youtubeDownloader: YoutubeDownloader) : AbstractYoutubeDownloader() {

  private lateinit var currentVideos: List<YoutubeItem>
  private val downloads: MutableList<AbstractYoutubeItemDownloader> = mutableListOf()
  private var lastIndex = 0

  override fun startDownload(videos: List<YoutubeItem>) {
    this.currentVideos = videos
    onDownloading.invoke(true)
    download(currentVideos, lastIndex)
  }

  override fun cancelDownload(youtubeItem: YoutubeItem) {
    downloads.firstOrNull { it.youtubeItem == youtubeItem }?.cancel()
  }

  override fun retryDownload(youtubeItem: YoutubeItem) {
    onDownloading.invoke(true)
    downloadVideoAt(currentVideos.indexOf(youtubeItem) , false)
  }


  private fun download(videos: List<YoutubeItem>, index: Int) {
    if (downloads.size == UserPreferences.MAX_PARALLEL_DOWNLOADS) {
      lastIndex--; return
    }
    if (index >= videos.size) return

    downloadVideoAt(index)
    download(videos, ++lastIndex)
  }

  private fun downloadVideoAt(index: Int, downloadNext: Boolean = true) {
    val downloadItem = createDownloadItem(index, downloadNext)
    downloadItem.download()
    downloads.add(downloadItem)
  }

  private fun createDownloadItem(index: Int, downloadNext: Boolean): AbstractYoutubeItemDownloader {
    val downloadItem = YoutubeItemDownloaderFactory.create(youtubeDownloader, currentVideos[index])
    downloadItem.onProgress {
      StateManager.updateVideoState(downloadItem.youtubeItem, State.Downloading(it))
      onProgressUpdated.invoke(index)
    }
    downloadItem.onCanceled { canceled ->
      onProgressUpdated.invoke(index)
      if (canceled) {
        downloads.remove(downloadItem)
        StateManager.updateVideoState(downloadItem.youtubeItem, State.Canceled)
        if (downloadNext) download(currentVideos, ++lastIndex)
      } else {
        StateManager.updateVideoState(downloadItem.youtubeItem, State.Canceling)
      }
      resetDownloaderIfFinished()
    }
    downloadItem.onError { errorMessage ->
      onProgressUpdated.invoke(index)
      downloads.remove(downloadItem)
      StateManager.updateVideoState(downloadItem.youtubeItem, State.Error(errorMessage))
      if (downloadNext) download(currentVideos, ++lastIndex)
      resetDownloaderIfFinished()
    }
    downloadItem.onFinished {
      downloads.remove(downloadItem)
      StateManager.updateVideoState(downloadItem.youtubeItem, State.Completed)
      if (downloadNext) download(currentVideos, ++lastIndex)
      onProgressUpdated.invoke(index)
      resetDownloaderIfFinished()
    }
    downloadItem.onCustomState {
      StateManager.updateVideoState(downloadItem.youtubeItem, it)
      onProgressUpdated.invoke(index)
    }
    return downloadItem
  }

  private fun resetDownloaderIfFinished() {
    if (downloads.isEmpty()) {
      lastIndex = 0
      onDownloading.invoke(false)
    }
  }

}
