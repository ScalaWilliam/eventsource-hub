package services

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import javax.inject.{Inject, Singleton}

/**
  * Created by me on 05/04/2017.
  */
@Singleton
class FileStore(val eventsPath: Path) {

  @Inject def this() = this(Paths.get(FileStore.DefaultFile))

  Files.createDirectories(eventsPath.getParent)

  /** Use approach from https://gist.github.com/ScalaWilliam/37c4ef3c41e656a6d3c02d992f5c6191 **/
  def appendLine(line: String): Unit = {
    val raf = new java.io.RandomAccessFile(eventsPath.toFile, "rw").getChannel
    val lock = raf.lock()
    val lineBytes = (line + "\n").getBytes("UTF-8")
    try Files.write(eventsPath, lineBytes, StandardOpenOption.APPEND)
    finally lock.release()
  }

}

object FileStore {

  val DefaultFile = "events/aChannel.log"

}
