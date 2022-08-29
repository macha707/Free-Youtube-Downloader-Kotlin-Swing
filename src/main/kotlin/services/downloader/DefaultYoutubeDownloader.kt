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
}

class DefaultYoutubeDownloader(private val youtubeDownloader: YoutubeDownloader) : IYoutubeDownloader {

  private var onDownloading: DownloadListener = {}
  private var onProgressUpdated: ProgressUpdatedListener = {}

  private val downloads: MutableList<AbstractYoutubeItemDownloader> = mutableListOf()
  private var lastIndex = 0

  override fun startDownload(videos: List<YoutubeItem>) {
    onDownloading.invoke(true)
    download(videos, lastIndex)
  }

  override fun cancelDownload(youtubeItem: YoutubeItem) {
    downloads.firstOrNull { it.youtubeItem == youtubeItem }?.cancel()
  }

  private fun download(videos: List<YoutubeItem>, index: Int) {
    if (downloads.size == UserPreferences.MAX_PARALLEL_DOWNLOADS) {
      lastIndex--; return; }
    if (index >= videos.size) return


    val downloadItem = YoutubeItemDownloaderFactory.create(youtubeDownloader, videos[index])

    downloadItem.onProgress {
      StateManager.updateVideoState(downloadItem.youtubeItem, State.Downloading(it))
      onProgressUpdated.invoke(index)
    }

    downloadItem.onCanceled { canceled ->
      onProgressUpdated.invoke(index)
      if (canceled) {
        downloads.remove(downloadItem)
        StateManager.updateVideoState(downloadItem.youtubeItem, State.Canceled)
        download(videos, ++lastIndex)
      } else {
        StateManager.updateVideoState(downloadItem.youtubeItem, State.Canceling)
      }
      if (downloads.isEmpty()) {
        lastIndex = 0
        onDownloading.invoke(false)
      }
    }

    downloadItem.onFinished {
      downloads.remove(downloadItem)
      StateManager.updateVideoState(downloadItem.youtubeItem, State.Completed)
      download(videos, ++lastIndex)
      onProgressUpdated.invoke(index)
      if (downloads.isEmpty()) {
        lastIndex = 0
        onDownloading.invoke(false)
      }
    }

    downloadItem.download()

    downloadItem.onCustomState {
      StateManager.updateVideoState(downloadItem.youtubeItem, it)
      onProgressUpdated.invoke(index)
    }

    downloads.add(downloadItem)
    download(videos, ++lastIndex)
  }


  fun onProgressUpdated(onProgressUpdated: ProgressUpdatedListener) {
    this.onProgressUpdated = onProgressUpdated
  }

  fun onDownloading(onDownloading: DownloadListener) {
    this.onDownloading = onDownloading
  }

}
