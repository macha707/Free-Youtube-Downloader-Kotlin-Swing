package utils

import models.UserPreferences
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.StringCharacterIterator
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


fun getSpeedText(startTime: Long, downloadedSize: Long): String {
  val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
  val timeInSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
  val bytesPerSec = downloadedSize / timeInSeconds
  var text = decimalFormat.format(bytesPerSec) + "B/s"
  if (bytesPerSec > 1024) {
    val downloadSizeInKb = downloadedSize / 1024.0
    val kbPerSec = downloadSizeInKb / timeInSeconds
    text = decimalFormat.format(kbPerSec) + " KB/s"
    if (kbPerSec > 1024) {
      val downloadSizeInMb = downloadSizeInKb / 1024.0
      val mbPerSec = downloadSizeInMb / timeInSeconds
      text = decimalFormat.format(mbPerSec) + " MB/s"
    }
  }
  return text
}

fun Long.toReadableFileSize(): String {
  val absB = if (this == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(this)
  if (absB < 1024) {
    return "$this B"
  }
  var value = absB
  val ci = StringCharacterIterator("KMGTPE")
  var i = 40
  while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
    value = value shr 10
    ci.next()
    i -= 10
  }
  value *= java.lang.Long.signum(this).toLong()
  return String.format("%.1f %cB", value / 1024.0, ci.current())
}

fun String.asGoodFileName(): String {
  return this.replace(Regex("[#%&{}<>*?$!'/\\\\\":@+`|=]"), " ")
    .replace("\\s+".toRegex(), " ")
}

var JTextField.placeholder: String
  get() = this.getClientProperty("JTextField.placeholderText")?.toString() ?: ""
  set(value) = this.putClientProperty("JTextField.placeholderText", value)


fun JTextField.addTextChangeListener(listener: (event: DocumentEvent?) -> Unit) {
  this.document.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) {
      listener(e)
    }

    override fun removeUpdate(e: DocumentEvent?) {
      listener(e)
    }

    override fun changedUpdate(e: DocumentEvent?) {
      listener(e)
    }
  })
}

fun ImageIcon.loadUrl(url: String, height: Int) {
  image = try {
    val webImage = ImageIO.read(URL(url))
    val ratio = 1.0 * webImage.width / webImage.height
    webImage.getScaledInstance((height * ratio).toInt(), height, Image.SCALE_SMOOTH)
  } catch (e: Exception) {
    BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB).run {
      val graphics = createGraphics()
      graphics.paint = Color.GRAY
      graphics.fillRect(0, 0, width, height)
      this
    }
  }
}

fun JPanel.chooseFolder(startLocation: File = File(UserPreferences.DOWNLOADS_FOLDER)): File {
  var file: File = startLocation
  val fileChooser = JFileChooser(startLocation)
  fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
  val option = fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(this) as JFrame)
  if (option == JFileChooser.APPROVE_OPTION) {
    file = fileChooser.selectedFile
  }
  return file
}

