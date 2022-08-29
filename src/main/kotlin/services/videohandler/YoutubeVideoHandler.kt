package services.videohandler

import models.UserPreferences
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Supplier

interface IPromise<T> {
  fun <U> then(handler: (input: T) -> U): IPromise<U>
  fun thenAccept(consumer: Consumer<T>?): IPromise<Void?>
  fun get(): T
}

class Promise<T> : IPromise<T> {

  private val future: CompletableFuture<T>

  constructor(handler: Supplier<T>) {
    future = CompletableFuture.supplyAsync(handler)
  }

  private constructor(future: CompletableFuture<T>) {
    this.future = future
  }

  override fun <O> then(handler: (input: T) -> O): IPromise<O> {
    return Promise(future.thenApply(handler))
  }

  override fun thenAccept(consumer: Consumer<T>?): IPromise<Void?> {
    return Promise(future.thenAccept(consumer))
  }

  private fun onResult(result: T) {
    future.complete(result)
  }

  private fun doWork(result: T): Any? {
    onResult(result)
    return null
  }

  override fun get(): T {
    return this.future.get()
  }
}

interface VideoHandler<I, O> {
  fun handle(input: I): O
}

class YoutubeVideoMerger : VideoHandler<YoutubeVideoMerger.VideoMergerRequest, File> {
  data class VideoMergerRequest(val outputFileName: String , val videoFile: File ,val audioFile: File)
  override fun handle(input: VideoMergerRequest): File {
    val mergedFile = File(input.videoFile.parent, "${input.outputFileName}.mp4")
    val exeCmd = arrayOf(
      File(UserPreferences.FFMPEG_PATH).absolutePath, "-i",
      input.videoFile.absolutePath, "-i", input.audioFile.absolutePath,
      "-acodec", "copy", "-vcodec", "copy",
      mergedFile.absolutePath
    )
    val pb = ProcessBuilder(*exeCmd)

    runCatching {
      pb.redirectErrorStream()
      val p = pb.inheritIO().start()
      p.waitFor()
      input.videoFile.delete()
      input.audioFile.delete()
      p.destroy()
    }

    return mergedFile
  }
}

