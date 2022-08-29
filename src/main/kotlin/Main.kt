import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme
import com.github.kiulian.downloader.YoutubeDownloader
import services.downloader.DefaultYoutubeDownloader
import services.parser.YoutubeParser
import services.state.StateManager
import ui.AboutDialog
import ui.CenterPanel
import ui.NorthPanel
import ui.PreferencesDialog
import utils.Constants
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.*

class MainFrame : JFrame("Free Youtube Downloader") {
  val northLayout: NorthPanel
  val centerLayout: CenterPanel

  init {
    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    minimumSize = Dimension(500, 450)
    preferredSize = Dimension(500, 550)
    jMenuBar = JMenuBar().apply {
      add(JMenu("Help").apply {
        add(JMenuItem("Preferences").apply {
          addActionListener {
            PreferencesDialog(this@MainFrame).isVisible = true
          }
        })
        add(JSeparator())
        add(JMenuItem("About").apply {
          addActionListener {
            AboutDialog(this@MainFrame).isVisible = true
          }
        })
      })
    }
    iconImages = arrayOf("16", "24", "32", "48", "64", "72", "96", "128").map {
      Toolkit.getDefaultToolkit().getImage(Constants.javaClass.classLoader.getResource("icons/icon${it}.png"))
    }

    northLayout = NorthPanel(this)
    centerLayout = CenterPanel()

    add(northLayout, BorderLayout.NORTH)
    add(centerLayout, BorderLayout.CENTER)

    pack()
    setLocationRelativeTo(null)
    isVisible = true
  }
}

fun main() {
  FlatOneDarkIJTheme.setup()
  UIManager.put("TitlePane.unifiedBackground", false)

  val youtubeDownloader = YoutubeDownloader()
  val parser = YoutubeParser(youtubeDownloader)
  val defaultYoutubeDownloader = DefaultYoutubeDownloader(youtubeDownloader)

  SwingUtilities.invokeLater {
    val frame = MainFrame()

    StateManager.onVideoAdded { frame.centerLayout.addVideo(it) }
    StateManager.onTotalDownloadSizeChange { frame.northLayout.updateDownloadSize(it) }
    StateManager.onAvailableQualitiesChange { frame.northLayout.updateQualities(it) }

    StateManager.onSelectedQualityChange { frame.centerLayout.updateSelectedQuality() }
    StateManager.onDownloadLocationChange { frame.centerLayout.updateDownloadLocation() }

    StateManager.onVideoQualityChange { frame.centerLayout.updateSelectedQuality(it) }
    StateManager.onVideoDownloadLocationChange { frame.centerLayout.updateDownloadLocation(it) }
    StateManager.onVideoStateChanged { frame.centerLayout.updateItemState(it) }

    parser.onParsing { isParsing -> frame.northLayout.disable(isParsing) }
    parser.onNewVideo { StateManager.addVideo(it) }

    defaultYoutubeDownloader.onDownloading { isDownloading -> frame.northLayout.disable(isDownloading) }
    defaultYoutubeDownloader.onProgressUpdated { frame.centerLayout.updateItemState(it) }

    frame.northLayout.onAddClicked { youtubeUrl -> parser.smartParseAsync(youtubeUrl) }
    frame.northLayout.onDownloadClicked { defaultYoutubeDownloader.startDownload(StateManager.videos) }
    frame.centerLayout.onVideoCancelClicked { defaultYoutubeDownloader.cancelDownload(it) }
  }

  parser.smartParse("https://www.youtube.com/watch?v=mkggXE5e2yk")
  parser.smartParse("https://www.youtube.com/watch?v=LXb3EKWsInQ")
  parser.smartParse("https://www.youtube.com/watch?v=2Xmibe4YhpQ")
  parser.smartParse("https://www.youtube.com/watch?v=1La4QzGeaaQ")
  parser.smartParse("https://www.youtube.com/playlist?list=PL0vfts4VzfNiP4xgrtnSUbK99iXLINc9m")
}

