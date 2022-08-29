package services.downloader

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload
import com.github.kiulian.downloader.downloader.response.Response
import models.State
import models.YoutubeItem
import models.YoutubeVideo
import models.YoutubeVideoWithAudio
import services.videohandler.Promise
import services.videohandler.YoutubeVideoMerger
import java.io.File
import kotlin.concurrent.thread

abstract class AbstractYoutubeItemDownloader(val youtubeItem: YoutubeItem) {

  protected var onProgress: (progress: Int) -> Unit = { }
  protected var onFinished: (data: File) -> Unit = { }
  protected var onCanceled: (canceled: Boolean) -> Unit = { }
  protected var onCustomState: (state: State.CustomState) -> Unit = { }

  protected val promise = Promise { }

  fun onProgress(onProgress: (progress: Int) -> Unit) {
    this.onProgress = onProgress
  }

  fun onFinished(onFinished: (data: File) -> Unit) {
    this.onFinished = onFinished
  }

  fun onCanceled(onCanceled: (canceled: Boolean) -> Unit) {
    this.onCanceled = onCanceled
  }

  fun onCustomState(onCustomState: (state: State) -> Unit) {
    this.onCustomState = onCustomState
  }

  abstract fun download()
  abstract fun cancel()

}

open class YoutubeVideoDownloader(
  private val downloader: YoutubeDownloader,
  youtubeVideo: YoutubeVideo,
) : AbstractYoutubeItemDownloader(youtubeVideo) {

  private var videoDownloadedSize = 0L
  private var isVideoDownloaded = false
  private lateinit var videoFile: File
  private var videoResponse: Response<File>? = null

  override fun download() {
    downloadVideo()
  }

  override fun cancel() {
    videoResponse?.cancel()
    onCanceled(false)
    thread {
      while (videoFile.exists()) if (videoFile.exists()) videoFile.delete()
      onCanceled(true)
    }
  }

  private fun downloadVideo() {
    println("Downloading: ${youtubeItem.name} with ${youtubeItem.downloadQuality}")
    val videoDownloadRequest = RequestVideoFileDownload(youtubeItem.downloadQuality.format)
      .saveTo(youtubeItem.downloadTo)
      .renameTo(youtubeItem.name + " - Video")
      .overwriteIfExists(true)
      .callback(object : YoutubeProgressCallback<File> {
        override fun onFinished(data: File) {
          isVideoDownloaded = true
          videoFile = data
          downloadFinished()
        }

        override fun onError(throwable: Throwable?) {
        }

        override fun onDownloading(progress: Int) {
          videoDownloadedSize = ((progress / 100.0) * youtubeItem.size).toLong()
          updateProgress()
        }
      }).async()

    videoResponse = downloader.downloadVideoFile(videoDownloadRequest)
    videoFile = videoDownloadRequest.outputFile
  }

  private fun updateProgress() {
    val totalProgress = (((videoDownloadedSize) / youtubeItem.size.toDouble()) * 100.0).toInt()
    onProgress(totalProgress)
  }

  private fun downloadFinished() {
    if (isVideoDownloaded) {
      onFinished(videoFile)
    }
  }
}

class YoutubeVideoWithAudioDownloader(
  private val downloader: YoutubeDownloader,
  youtubeVideoWithAudio: YoutubeVideoWithAudio
) : AbstractYoutubeItemDownloader(youtubeVideoWithAudio) {

  private var videoDownloadedSize = 0L
  private var isVideoDownloaded = false
  private lateinit var videoFile: File
  private var videoResponse: Response<File>? = null

  private var audioDownloadedSize = 0L
  private var isAudioDownload = false
  private lateinit var audioFile: File
  private var audioResponse: Response<File>? = null

  override fun download() {
    downloadVideo()
    downloadAudio()
  }

  override fun cancel() {
    videoResponse?.cancel()
    audioResponse?.cancel()
    onCanceled(false)
    thread {
      while (videoFile.exists() || audioFile.exists()) {
        if (videoFile.exists()) videoFile.delete()
        if (audioFile.exists()) audioFile.delete()
      }
      onCanceled(true)
    }
  }

  private fun downloadVideo() {
    println("Downloading: ${youtubeItem.name} with ${youtubeItem.downloadQuality}")
    val videoDownloadRequest = RequestVideoFileDownload(youtubeItem.downloadQuality.format)
      .saveTo(youtubeItem.downloadTo)
      .renameTo(youtubeItem.name + " - Video")
      .overwriteIfExists(true)
      .callback(object : YoutubeProgressCallback<File> {
        override fun onFinished(data: File) {
          isVideoDownloaded = true
          videoFile = data
          downloadFinished()
        }

        override fun onError(throwable: Throwable?) {
        }

        override fun onDownloading(progress: Int) {
          videoDownloadedSize = ((progress / 100.0) * (youtubeItem as YoutubeVideoWithAudio).videoSize).toLong()
          updateProgress()
        }
      }).async()

    videoResponse = downloader.downloadVideoFile(videoDownloadRequest)
    videoFile = videoDownloadRequest.outputFile
  }

  private fun downloadAudio() {
    val audioDownloadRequest = RequestVideoFileDownload((youtubeItem as YoutubeVideoWithAudio).audioDownloadFormat)
      .saveTo(youtubeItem.downloadTo)
      .renameTo(youtubeItem.name + " - Audio")
      .overwriteIfExists(true)
      .callback(object : YoutubeProgressCallback<File> {
        override fun onFinished(data: File) {
          isAudioDownload = true
          audioFile = data
          downloadFinished()
        }

        override fun onError(throwable: Throwable?) {

        }

        override fun onDownloading(progress: Int) {
          audioDownloadedSize = ((progress / 100.0) * youtubeItem.audioSize).toLong()
          updateProgress()
        }
      }).async()

    audioResponse = downloader.downloadVideoFile(audioDownloadRequest)
    audioFile = audioDownloadRequest.outputFile

  }

  private fun updateProgress() {
    val totalProgress = (((videoDownloadedSize + audioDownloadedSize) / youtubeItem.size.toDouble()) * 100.0).toInt()
    onProgress(totalProgress)
  }

  private fun downloadFinished() {
    if (isAudioDownload && isVideoDownloaded) {
      val mergedFile = promise.then {
        onCustomState(State.CustomState("Merging"))
        val mergerRequest = YoutubeVideoMerger.VideoMergerRequest(youtubeItem.name, videoFile, audioFile)
        YoutubeVideoMerger().handle(mergerRequest)
      }.get()
      onFinished(mergedFile)
    }
  }

}