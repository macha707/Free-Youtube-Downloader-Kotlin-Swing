import com.formdev.flatlaf.FlatDarculaLaf
import models.*
import services.Downloader
import ui.AboutDialog
import ui.CenterPanel
import ui.NorthPanel
import ui.PreferencesDialog
import utils.Constants
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.io.File
import javax.swing.*


fun main() {
  FlatDarculaLaf.setup()

  val frame = JFrame("Free Youtube Downloader").apply frame@ {
    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    minimumSize = Dimension(500, 450)
    preferredSize = Dimension(600, 550)
    jMenuBar = JMenuBar().apply {
      add(JMenu("Help").apply {
        add(JMenuItem("Preferences").apply {
          addActionListener {
            PreferencesDialog(this@frame).isVisible = true
          }
        })
        add(JSeparator())
        add(JMenuItem("About").apply {
          addActionListener {
            AboutDialog(this@frame).isVisible = true
          }
        })
      })
    }

    iconImages = arrayOf("16", "24", "32", "48", "64", "72", "96", "128").map {
      Toolkit.getDefaultToolkit().getImage(Constants.javaClass.classLoader.getResource("icons/icon${it}.png"))
    }
  }

  val northLayout = NorthPanel(frame)
  val centerLayout = CenterPanel()

  Downloader.subscribe {
    when (it.propertyName) {
      "VIDEO_ADDED" -> {
        centerLayout.addVideo(it.newValue as VideoItem)
      }

      "NEW_PLAYLIST" -> {
        val data = it.newValue as NewPlaylistResponse
        northLayout.disable(data.isParsing)
      }

      "NEW_VIDEO" -> {
        val isParsing = it.newValue as Boolean
        northLayout.disable(isParsing)
      }

      "UPDATE_PROGRESS" -> {
        centerLayout.updateItemState(it.newValue as Int)
      }

      "DOWNLOADING" -> {
        val isDownloading = it.newValue as Boolean
        northLayout.disable(isDownloading)
      }
    }
  }
  northLayout.addPropertyChangeListener {
    when (it.propertyName) {
      "NEW_URL" -> {
        Downloader.parse(it.newValue as NewUrlRequest)
      }

      "CHANGE_DOWNLOAD_LOCATION" -> {
        centerLayout.updateDownloadLocation(it.newValue as File)
      }

      "CHANGE_DOWNLOAD_QUALITY" -> {
        centerLayout.updateSelectedQuality(it.newValue as Quality)
      }

      "START_DOWNLOAD" -> {
        Downloader.startDownload(centerLayout.videos)
      }
    }
  }
  centerLayout.addPropertyChangeListener {
    when (it.propertyName) {
      "UPDATE_TOTAL_DOWNLOAD_SIZE" -> {
        northLayout.updateDownloadSize(it.newValue as Long)
      }

      "UPDATE_AVAILABLE_QUALITIES" -> {
        northLayout.updateQualities(it.newValue as List<Quality>)
      }

      "CANCEL_VIDEO" -> {
        Downloader.cancelVideoDownload(it.newValue as VideoItem)
      }
    }
  }

  frame.add(northLayout, BorderLayout.NORTH)
  frame.add(centerLayout, BorderLayout.CENTER)

  frame.pack()
  frame.setLocationRelativeTo(null)
  frame.isVisible = true

//  Downloader.parse(NewUrlRequest("https://www.youtube.com/watch?v=mkggXE5e2yk", File(UserPreferences.DOWNLOADS_FOLDER)))
//  Downloader.parse(NewUrlRequest("https://www.youtube.com/watch?v=LXb3EKWsInQ", File(UserPreferences.DOWNLOADS_FOLDER)))
//  Downloader.parse(NewUrlRequest("https://www.youtube.com/watch?v=2Xmibe4YhpQ", File(UserPreferences.DOWNLOADS_FOLDER)))
//  Downloader.parse(NewUrlRequest("https://www.youtube.com/watch?v=1La4QzGeaaQ", File(UserPreferences.DOWNLOADS_FOLDER)))

}

