package panels

import models.VideoItem
import utils.humanReadableSize
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel
import kotlin.math.roundToInt

class CenterPanel(videos: MutableList<VideoItem>) : JPanel() {

  inner class VideoTableItemModel(private val videos: MutableList<VideoItem>) : AbstractTableModel() {

    override fun getRowCount() = videos.size
    override fun getColumnCount() = 6

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
      var value: Any = "??"
      val video = videos[rowIndex]
      when (columnIndex) {
        0 -> value = rowIndex.plus(1).toString().padStart(3, '0')
        1 -> value = video.title
        2 -> value = video.size.humanReadableSize()
        3 -> value = "${video.progress}%"
        4 -> value = video.speed
        5 -> value = video.status
      }
      return value
    }

    override fun getColumnName(columnIndex: Int): String {
      var value = "??"
      when (columnIndex) {
        0 -> value = "#"
        1 -> value = "Video"
        2 -> value = "Size"
        3 -> value = "Progress"
        4 -> value = "Speed"
        5 -> value = "Status"
      }
      return value
    }

    fun removeVideo(index: Int) {
      videos.removeAt(index)
      fireTableRowsDeleted(index, index)
    }
  }

  var tiModel = VideoTableItemModel(videos)
  var table: JTable = JTable(tiModel)

  private var reservedWidth = 0
  private fun take(value: Int = -1): Float {
    var wv = value
    if (wv == -1) wv = 100 - reservedWidth
    val width = wv / 100.0f
    reservedWidth += wv
    return width
  }

  private var columnWidthPercentage = mapOf(
    "#" to take(3),
    "Size" to take(7),
    "Progress" to take(7),
    "Speed" to take(10),
    "Status" to take(10),
    "Video" to take(),
  )

  init {
    layout = BorderLayout()
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

    table.showHorizontalLines = true
    table.showVerticalLines = true
    table.cellSelectionEnabled = false
    table.columnSelectionAllowed = false
    table.rowSelectionAllowed = true
    table.addKeyListener(object : KeyListener {
      override fun keyTyped(e: KeyEvent?) = Unit
      override fun keyReleased(e: KeyEvent?) = Unit
      override fun keyPressed(e: KeyEvent?) {
        if (e?.keyCode == KeyEvent.VK_DELETE) {
          val rows = table.selectedRows
          for (i in rows.indices) tiModel.removeVideo(rows[i] - i)
        }
      }
    })

    resizeColumnWidth()
    add(JScrollPane(table))
  }

  private fun resizeColumnWidth() {
    val tW: Int = table.columnModel.totalColumnWidth
    var column: TableColumn
    val jTableColumnModel: TableColumnModel = table.columnModel
    val cantCols = jTableColumnModel.columnCount
    for (i in 0 until cantCols) {
      column = jTableColumnModel.getColumn(i)
      val pWidth = (columnWidthPercentage[column.headerValue]!! * tW).roundToInt()
      column.preferredWidth = pWidth
    }
  }

  fun addVideo(index: Int) {
    tiModel.fireTableRowsInserted(index, index)
    resizeColumnWidth()
    table.scrollRectToVisible(table.getCellRect(table.rowCount - 1, 0, true))
  }

  fun updateVideoSize(index: Int) {
    tiModel.fireTableCellUpdated(index, 2)
  }

  fun updateProgress(index: Int) {
    tiModel.fireTableCellUpdated(index, 3)
    tiModel.fireTableCellUpdated(index, 4)
    tiModel.fireTableCellUpdated(index, 5)
  }
}
