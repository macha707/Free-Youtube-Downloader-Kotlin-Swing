package models

import com.github.kiulian.downloader.model.Extension
import com.github.kiulian.downloader.model.videos.VideoInfo
import com.github.kiulian.downloader.model.videos.formats.Format
import utils.asGoodFileName
import java.io.File

sealed class YoutubeItem(videoInfo: VideoInfo) {
  // This means it is not good idea to download same video twice
  private val id = videoInfo.details().videoId()

  var state: State = State.Normal
  var downloadTo: File = File(UserPreferences.DOWNLOADS_FOLDER)
  var name: String = videoInfo.details().title().asGoodFileName().ifEmpty { "No Name" }
    set(value) {
      field = value.asGoodFileName().ifEmpty { "No Name" }
    }
  val thumbnailUrl: String = "https://i.ytimg.com/vi/${videoInfo.details().videoId()}/hqdefault.jpg"

  abstract val availableQualities: List<Quality>

  abstract var downloadQuality: Quality
  abstract val size: Long

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as YoutubeItem
    if (id != other.id) return false
    return true
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

}

data class YoutubeVideo(val videoInfo: VideoInfo) : YoutubeItem(videoInfo) {
  override val availableQualities: List<Quality>
    get() = videoInfo.videoWithAudioFormats()
      .filter { it.extension() == Extension.MPEG4 }
      .map { Quality(it) }
      .sortedBy { it.qualityLabel?.ordinal }

  override var downloadQuality: Quality = Quality(videoInfo.bestVideoWithAudioFormat())

  override val size: Long
    get() = downloadQuality.format.bitrate() / 8 * downloadQuality.format.duration() / 1000
}

data class YoutubeVideoWithAudio(val videoInfo: VideoInfo) : YoutubeItem(videoInfo) {
  val audioDownloadFormat: Format = videoInfo.bestAudioFormat()
  val videoSize: Long get() = downloadQuality.format.contentLength() ?: 0
  val audioSize: Long get() = audioDownloadFormat.contentLength() ?: 0

  override val availableQualities: List<Quality>
    get() = videoInfo.videoFormats()
      .filter { it.type() == "video" }
      .groupBy { it.itag().videoQuality().name }
      .map { group -> group.value.maxBy { it.itag().id() } }
      .map { Quality(it) }
      .sortedBy { it.qualityLabel?.ordinal }

  override var downloadQuality: Quality = Quality(videoInfo.bestVideoFormat())

  override val size: Long
    get() = videoSize + audioSize

}


class Quality(val format: Format) {

  enum class QualityLabel(val itags: List<Int> = emptyList(), val readableName: String) {

    Q4320P(itags = listOf(402, 571, 272, 138), readableName = "4320p"),
    Q2160P(itags = listOf(701, 401, 337, 315, 313, 305, 266), readableName = "2160p"),
    Q1440P(itags = listOf(700, 400, 336, 308, 271, 304, 264), readableName = "1440p"),
    Q1080P(itags = listOf(699, 399, 335, 303, 248, 299, 137), readableName = "1080p"),
    Q720P(itags = listOf(698, 398, 334, 302, 247, 298, 136, 22), readableName = "720p"),
    Q480P(itags = listOf(697, 397, 333, 244, 135), readableName = "480p"),
    Q360P(itags = listOf(696, 396, 332, 243, 134, 18), readableName = "360p"),
    Q240P(itags = listOf(695, 395, 331, 242, 133), readableName = "240p"),
    Q144P(itags = listOf(694, 394, 330, 278, 160, 17), readableName = "144p");

    companion object {
      fun of(itag: Int): QualityLabel? {
        return values().firstOrNull { it.itags.contains(itag) }
      }
    }
  }

  private val text = format.itag().videoQuality().name
  val qualityLabel get() = QualityLabel.of(format.itag().id())

  override fun toString(): String {
    return qualityLabel?.readableName ?: String.format(
      "%s - %s (%s)", format.itag().toString(), format.itag().videoQuality().name, format.extension().value(),
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Quality
    if (text != other.text) return false
    return true
  }

  override fun hashCode(): Int {
    return text.hashCode()
  }

}

sealed class State(val stateText: String) {
  object Normal : State("")
  object Canceling : State("Canceling")
  object Canceled : State("Canceled")
  object Completed : State("Completed")
  data class Downloading(val progress: Int = 0) : State("Downloading ${progress}%")
  data class Error(val errorMessage: String = "Error") : State(errorMessage)
  data class CustomState(val text: String) : State(text)
}

object YoutubeItemFactory {
  fun createYoutubeItem(videoInfo: VideoInfo): YoutubeItem {
    if (!UserPreferences.isFFmpegAvailable()) return YoutubeVideo(videoInfo)

    lateinit var youtubeItem: YoutubeItem
    runCatching {
      youtubeItem = YoutubeVideoWithAudio(videoInfo)
      if (youtubeItem.availableQualities.isEmpty())
        youtubeItem = YoutubeVideo(videoInfo)
    }.onFailure {
      youtubeItem = YoutubeVideo(videoInfo)
    }
    return youtubeItem
  }
}
