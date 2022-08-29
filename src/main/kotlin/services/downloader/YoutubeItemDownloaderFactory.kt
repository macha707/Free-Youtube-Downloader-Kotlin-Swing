package services.downloader

import com.github.kiulian.downloader.YoutubeDownloader
import models.YoutubeItem
import models.YoutubeVideo
import models.YoutubeVideoWithAudio

object YoutubeItemDownloaderFactory {
  fun create(downloader: YoutubeDownloader, youtubeItem: YoutubeItem): AbstractYoutubeItemDownloader {
    return when(youtubeItem) {
      is YoutubeVideo -> YoutubeVideoDownloader(downloader, youtubeItem)
      is YoutubeVideoWithAudio -> YoutubeVideoWithAudioDownloader(downloader , youtubeItem)
    }
  }
}