package utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.StringCharacterIterator
import java.util.*
import javax.swing.JTextField

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

fun Long.humanReadableSize(): String {
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
  return String.format("%.1f %ciB", value / 1024.0, ci.current())
}


var JTextField.placeholder: String
  get() = this.getClientProperty("JTextField.placeholderText")?.toString() ?: ""
  set(value) = this.putClientProperty("JTextField.placeholderText", value)
