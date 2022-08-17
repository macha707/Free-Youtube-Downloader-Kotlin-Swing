import com.formdev.flatlaf.FlatDarculaLaf
import models.NewUrlData
import models.NewPlaylistData
import panels.CenterPanel
import panels.NorthPanel
import panels.SouthPanel
import services.Downloader
import java.awt.*
import javax.swing.*


fun main() {
  FlatDarculaLaf.setup()

  JFrame("Free Youtube Downloader").apply {
    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    preferredSize = Dimension(850, 450)

    val downloader = Downloader()
    val northLayout = NorthPanel()
    val centerLayout = CenterPanel(downloader.videos)
    val southLayout = SouthPanel()

    add(northLayout, BorderLayout.NORTH)
    add(centerLayout, BorderLayout.CENTER)
    add(southLayout, BorderLayout.SOUTH)

    downloader.subscribe {
      when (it.propertyName) {
        "AVAILABLE_QUALITIES" -> {
          southLayout.updateQualities(it.newValue as List<String>)
        }

        "VIDEO_ADDED" -> {
          centerLayout.addVideo(it.newValue as Int)
        }

        "QUALITY_UPDATED" -> {
          centerLayout.updateVideoSize(it.newValue as Int)
        }

        "NEW_PLAYLIST" -> {
          val data = it.newValue as NewPlaylistData
          southLayout.updateDownloadFolder(data.title)
          northLayout.disable(data.isParsing)
          southLayout.disable(data.isParsing)
        }

        "UPDATE_PROGRESS" -> {
          centerLayout.updateProgress(it.newValue as Int)
        }
      }
    }

    northLayout.addPropertyChangeListener {
      if (it.propertyName == "NEW_URL") {
        val data = it.newValue as NewUrlData
        downloader.parse(data.id, data.isPlaylist)
      }
    }

    southLayout.addPropertyChangeListener {
      if (it.propertyName == "CHANGE_DOWNLOAD_QUALITY") {
        downloader.updateQuality(it.newValue.toString())
      } else if (it.propertyName == "START_DOWNLOAD") {
        downloader.startDownload(it.newValue.toString())
      }
    }

    pack()
    setLocationRelativeTo(null)
    isVisible = true
  }
}