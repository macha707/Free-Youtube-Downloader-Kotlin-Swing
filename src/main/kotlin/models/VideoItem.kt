package models

import com.github.kiulian.downloader.model.Extension
import com.github.kiulian.downloader.model.videos.VideoInfo
import com.github.kiulian.downloader.model.videos.formats.Format
import com.github.kiulian.downloader.model.videos.formats.VideoFormat
import java.io.File

data class VideoItem(
  val videoInfo: VideoInfo,
  var downloadTo: File,
) {
  var state: State = State.Normal

  val thumbnailUrl: String = "https://i.ytimg.com/vi/${videoInfo.details().videoId()}/hqdefault.jpg"
  var name: String = ""
    set(value) {
      field = value.replace(Regex("[#%&{}<>*?$!'/\\\\\":@+`|=]"), " ")
        .replace("\\s+".toRegex(), " ").ifEmpty { "No Name" }
    }

  val availableQualities
    get() = if (UserPreferences.FFMPEG_PATH.isEmpty()) {
      videoInfo.videoWithAudioFormats()
        .filter { it.extension() == Extension.MPEG4 }
        .sortedByDescending { it.itag().ordinal }
        .mapNotNull { Quality.of(it.itag().id()) }
    } else {
      videoInfo.videoFormats()
        .filter { it.extension() == Extension.MPEG4 && it.type() == "video" }
        .sortedByDescending { it.itag().videoQuality() }
        .mapNotNull { Quality.of(it.itag().id()) }.distinct()
    }

  var downloadQuality = availableQualities[0]
    set(value) {
      selectedVideoQuality = value.videoFormat(videoInfo) as VideoFormat
      field = value
    }

  var selectedVideoQuality: Format = downloadQuality.videoFormat(videoInfo) as VideoFormat
  var selectedAudioQuality: Format? = if (UserPreferences.FFMPEG_PATH.isNotEmpty()) videoInfo.bestAudioFormat() else null


  val videoSize: Long
    get() = if (isVideoWithAudioFormat) selectedVideoQuality.contentLength()
    else selectedVideoQuality.bitrate() / 8 * selectedVideoQuality.duration() / 1000
  val audioSize: Long get() = selectedAudioQuality?.contentLength() ?: 0
  val size: Long get() = videoSize + audioSize

  val isVideoWithAudioFormat: Boolean get() = selectedAudioQuality != null

  init {
    name = videoInfo.details().title()
  }

}

sealed class State(val stateText: String) {
  object Normal : State("")
  object Canceling : State("Canceling")
  object Canceled : State("Canceled")

  data class Downloading(val progress: Int = 0) : State("Downloading ${progress}%")
  object Merging : State("Merging")
  object Completed : State("Completed")
}

enum class Quality(val itags: List<Int> = emptyList(), val readableName: String) {

  Q2160P(itags = listOf(701 , 402, 305, 266), readableName = "2160p"),
  Q1440P(itags = listOf(700 , 401, 304, 264), readableName = "1440p"),
  Q1080P(itags = listOf(699 , 400, 299, 137), readableName = "1080p"),
  Q720P(itags = listOf(698 , 399, 298, 136), readableName = "720p"),
  Q480P(itags = listOf(697 , 398, 135), readableName = "480p"),
  Q360P(itags = listOf(696 , 397, 134), readableName = "360p"),
  Q240P(itags = listOf(695 , 396, 133), readableName = "240p"),

  HIGH(listOf(22), readableName = "High 720p"),
  MEDIUM(listOf(18), readableName = "Medium 360p"),
  LOW(listOf(17), readableName = "Low 144p");

  fun videoFormat(videoInfo: VideoInfo): Format? {
    var format = videoInfo.findFormatByItag(itags.first())
    for (itag in itags) {
      if (format != null) break
      format = videoInfo.findFormatByItag(itag)
    }
    return format
  }

  companion object {
    fun of(readableName: String): Quality? {
      return values().firstOrNull { it.readableName == readableName }
    }

    fun of(itag: Int): Quality? {
      return values().firstOrNull { it.itags.contains(itag) }
    }
  }

}
