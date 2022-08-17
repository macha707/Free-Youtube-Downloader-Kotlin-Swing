package models

import com.github.kiulian.downloader.model.videos.VideoInfo
import com.github.kiulian.downloader.model.videos.formats.VideoFormat

data class VideoItem(
  val videoInfo: VideoInfo,
  var progress: Int = 0,
  var speed: String = "0.0 KB/s",
) {
  var selectedQuality: VideoFormat = videoInfo.bestVideoWithAudioFormat()
  val title: String get() = videoInfo.details().title()
  val size: Long get() = selectedQuality.bitrate() / 8 * selectedQuality.duration() / 1000

  val status
    get() = when (progress) {
      0 -> ""
      in 1..99 -> "Downloading"
      else -> "Completed"
    }
}
