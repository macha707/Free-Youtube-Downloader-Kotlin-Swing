package services.parser

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import models.YoutubeItem
import models.YoutubeItemFactory
import utils.Constants
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future

typealias NewVideoListener = (youtubeItem: YoutubeItem) -> Unit
typealias ParsingListener = (isParsing: Boolean) -> Unit

interface IYoutubeParser {
  fun parseVideo(videoUrl: String): YoutubeItem
  fun parseVideoAsync(videoUrl: String): Future<YoutubeItem>

  fun parsePlaylist(playlistUrl: String): List<YoutubeItem>
  fun parsePlaylistAsync(playlistUrl: String): Future<List<YoutubeItem>>
}

class YoutubeParser(private val youtubeDownloader: YoutubeDownloader) : IYoutubeParser {

  private var onNewVideo: NewVideoListener = { }
  private var onParsing: ParsingListener = { }

  private val executor = Executors.newFixedThreadPool(4)

  fun smartParseAsync(youtubeUrl: String) {
    if (youtubeUrl.contains("playlist")) parsePlaylistAsync(youtubeUrl) else parseVideoAsync(youtubeUrl)
  }

  fun smartParse(youtubeUrl: String) {
    if (youtubeUrl.contains("playlist")) parsePlaylist(youtubeUrl) else parseVideo(youtubeUrl)
  }

  override fun parseVideo(videoUrl: String): YoutubeItem {
    onParsing.invoke(true)
    val videoId = Regex(Constants.VIDEO_ID_REGEX).find(videoUrl)!!.groupValues[2]
    val videoInfo = youtubeDownloader.getVideoInfo(RequestVideoInfo(videoId)).data()

    val youtubeItem = YoutubeItemFactory.createYoutubeItem(videoInfo)
    onNewVideo.invoke(youtubeItem)
    onParsing.invoke(false)
    return youtubeItem
  }

  override fun parsePlaylist(playlistUrl: String): List<YoutubeItem> {
    val youtubeItems = mutableListOf<YoutubeItem>()
    onParsing.invoke(true)

    val playlistId = Regex(Constants.PLAYLIST_ID_REGEX).find(playlistUrl)!!.groupValues[2]
    val response = youtubeDownloader.getPlaylistInfo(RequestPlaylistInfo(playlistId))
    val playlistInfo = response.data()

    for (video in playlistInfo.videos()) {
      val videoInfo = youtubeDownloader.getVideoInfo(RequestVideoInfo(video.videoId())).data()
      val youtubeItem = YoutubeItemFactory.createYoutubeItem(videoInfo)
      youtubeItems.add(youtubeItem)
      onNewVideo.invoke(youtubeItem)

    }
    onParsing.invoke(false)

    return youtubeItems
  }

  override fun parseVideoAsync(videoUrl: String): CompletableFuture<YoutubeItem> {
    return CompletableFuture.supplyAsync({ parseVideo(videoUrl) }, executor)
  }

  override fun parsePlaylistAsync(playlistUrl: String): CompletableFuture<List<YoutubeItem>> {
    return CompletableFuture.supplyAsync({ parsePlaylist(playlistUrl) }, executor)
  }

  fun onNewVideo(onNewVideo: NewVideoListener) {
    this.onNewVideo = onNewVideo
  }

  fun onParsing(onParsing: ParsingListener) {
    this.onParsing = onParsing
  }

}