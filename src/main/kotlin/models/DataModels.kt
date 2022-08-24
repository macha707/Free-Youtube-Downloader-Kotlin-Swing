package models

import java.io.File

data class NewUrlRequest(val url: String, val downloadTo: File)

data class NewPlaylistResponse(val title: String, val isParsing: Boolean)