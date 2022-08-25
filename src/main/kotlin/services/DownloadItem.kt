package services

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload
import com.github.kiulian.downloader.downloader.response.Response
import models.State
import models.UserPreferences
import models.VideoItem
import java.io.File
import java.io.IOException
import javax.swing.JOptionPane
import kotlin.concurrent.thread

class DownloadItem(
  private val downloader: YoutubeDownloader,
  val videoItem: VideoItem
) {

  private var onProgress: (progress: Int) -> Unit = { }
  private var onFinished: (item: DownloadItem, data: File) -> Unit = { _, _ -> }
  private var onCanceled: (item: DownloadItem, canceled: Boolean) -> Unit = { _, _ -> }

  private var videoDownloadedSize = 0L
  private var audioDownloadedSize = 0L

  private var isVideoDownloaded = false
  private var isAudioDownload = !videoItem.isVideoWithAudioFormat

  private lateinit var videoFile: File
  private lateinit var audioFile: File

  private var videoResponse: Response<File>? = null
  private var audioResponse: Response<File>? = null

  fun download() {
    downloadVideo()
    if (videoItem.isVideoWithAudioFormat) downloadAudio()
  }

  fun cancel() {
    videoResponse?.cancel()
    audioResponse?.cancel()
    videoItem.state = State.Canceling
    onCanceled(this, false)

    thread {
      while (videoFile.exists() || audioFile.exists()) {
        if (videoFile.exists()) videoFile.delete()
        if (audioFile.exists()) audioFile.delete()
      }
      videoItem.state = State.Canceled
      onCanceled(this, true)
    }

  }

  private fun downloadVideo() {
    val videoDownloadRequest = RequestVideoFileDownload(videoItem.selectedVideoQuality)
      .saveTo(videoItem.downloadTo)
      .renameTo(videoItem.name + " - Video")
      .overwriteIfExists(true)
      .callback(object : YoutubeProgressCallback<File> {
        override fun onFinished(data: File) {
          isVideoDownloaded = true
          videoFile = data
          downloadFinished()
        }

        override fun onError(throwable: Throwable?) {
          videoItem.state = State.Canceled
        }

        override fun onDownloading(progress: Int) {
          videoDownloadedSize = ((progress / 100.0) * videoItem.videoSize).toLong()
          updateProgress()
        }
      }).async()

    videoResponse = downloader.downloadVideoFile(videoDownloadRequest)
    videoFile = videoDownloadRequest.outputFile
  }

  private fun downloadAudio() {
    val audioDownloadRequest = RequestVideoFileDownload(videoItem.selectedAudioQuality)
      .saveTo(videoItem.downloadTo)
      .renameTo(videoItem.name + " - Audio")
      .overwriteIfExists(true)
      .callback(object : YoutubeProgressCallback<File> {
        override fun onFinished(data: File) {
          isAudioDownload = true
          audioFile = data
          downloadFinished()
        }

        override fun onError(throwable: Throwable?) {
          videoItem.state = State.Canceled
        }

        override fun onDownloading(progress: Int) {
          audioDownloadedSize = ((progress / 100.0) * videoItem.audioSize).toLong()
          updateProgress()
        }
      }).async()

    audioResponse = downloader.downloadVideoFile(audioDownloadRequest)
    audioFile = audioDownloadRequest.outputFile

  }

  private fun updateProgress() {
    val totalProgress = (((videoDownloadedSize + audioDownloadedSize) / videoItem.size.toDouble()) * 100.0).toInt()
    videoItem.state = State.Downloading(totalProgress)
    this@DownloadItem.onProgress(totalProgress)
  }

  private fun downloadFinished() {
    if (isAudioDownload && isVideoDownloaded) {
      if (videoItem.isVideoWithAudioFormat) {
        videoItem.state = State.Merging
        this@DownloadItem.onProgress(-1)
        val mergedFile = mergeVideoWithAudio()
        videoItem.state = State.Completed
        this@DownloadItem.onFinished(this, mergedFile)
      } else {
        videoItem.state = State.Completed
        this@DownloadItem.onFinished(this, videoFile)
      }

    }
  }

  private fun mergeVideoWithAudio(): File {
    val mergedFile = File(videoFile.parent, "${videoItem.name}.mp4")
    val exeCmd = arrayOf(
      File(UserPreferences.FFMPEG_PATH).absolutePath, "-i",
      videoFile.absolutePath, "-i", audioFile.absolutePath,
      "-acodec", "copy", "-vcodec", "copy",
      mergedFile.absolutePath
    )
    val pb = ProcessBuilder(*exeCmd)
    try {
      pb.redirectErrorStream()
      val p = pb.inheritIO().start()
      p.waitFor()
      videoFile.delete()
      audioFile.delete()
      p.destroy()
    } catch (e: InterruptedException) {
      e.printStackTrace()
      JOptionPane.showMessageDialog(null, e.message, "Error!!", JOptionPane.ERROR_MESSAGE)
    } catch (e: IOException) {
      e.printStackTrace()
      JOptionPane.showMessageDialog(null, e.message, "Error!!", JOptionPane.ERROR_MESSAGE)
    }
    return mergedFile
  }

  fun onProgress(onProgress: (progress: Int) -> Unit): DownloadItem {
    this.onProgress = onProgress
    return this
  }

  fun onFinished(onFinished: (item: DownloadItem, data: File) -> Unit): DownloadItem {
    this.onFinished = onFinished
    return this
  }

  fun onCanceled(onCanceled: (item: DownloadItem, canceled: Boolean) -> Unit): DownloadItem {
    this.onCanceled = onCanceled
    return this
  }

}